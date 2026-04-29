package com.rut.campusnavigation.domain.usecase

import com.rut.campusnavigation.domain.model.*
import com.rut.campusnavigation.domain.repository.CampusRepository
import org.osmdroid.util.GeoPoint
import java.util.PriorityQueue
import javax.inject.Inject

/**
 * Строит маршрут от userLocation до ближайшего входа targetBuilding.
 *
 * Если пользователь СНАРУЖИ кампуса — маршрут начинается с ближайшего КПП.
 * Если пользователь ВНУТРИ кампуса — маршрут начинается с ближайшего узла графа.
 * Граф учитывает переходы между корпусами и внутренние тротуары.
 * Fallback на прямую линию если граф не находит путь.
 */
class GetRouteUseCase @Inject constructor(
    private val campusRepository: CampusRepository
) {
    // Полигон периметра основного кампуса РУТ МИИТ (по забору).
    // К12 (Новосущевская 26А) — отдельный корпус вне основного забора, поэтому
    // в периметр не включён; маршрут к нему строится через КПП-2.
    // Порядок: по часовой стрелке, начиная с ЮЗ угла.
    private val CAMPUS_PERIMETER = listOf(
        GeoPoint(55.787050, 37.603700), // ЮЗ — Образцова/Новосущевская
        GeoPoint(55.787050, 37.609900), // ЮВ — восточный забор (за К8/К10)
        GeoPoint(55.789800, 37.609900), // СВ — северо-восточный угол
        GeoPoint(55.789800, 37.603700), // СЗ — Новосущевская (у К4 север)
        GeoPoint(55.787050, 37.603700)  // замыкание
    )

    // Кешируются один раз на всё время жизни use case
    private val graph by lazy { campusRepository.getCampusGraph() }
    private val nodeMap by lazy { graph.nodes.associateBy { it.id } }
    private val adj: Map<String, List<Pair<String, Double>>> by lazy {
        val map = HashMap<String, MutableList<Pair<String, Double>>>(graph.nodes.size * 2)
        graph.nodes.forEach { map[it.id] = mutableListOf() }
        graph.edges.forEach { edge ->
            map.getOrPut(edge.fromId) { mutableListOf() }.add(edge.toId to edge.distanceM)
            map.getOrPut(edge.toId)   { mutableListOf() }.add(edge.fromId to edge.distanceM)
        }
        map
    }

    operator fun invoke(
        userLocation: GeoPoint,
        targetBuilding: Building,
        targetRoom: Room? = null
    ): Route? {
        if (targetBuilding.entrances.isEmpty()) return null
        if (targetBuilding.offCampus) return null  // К12 и поликлиника вне кампуса

        // Стартовая точка Дейкстры:
        // - снаружи кампуса → ближайший КПП (не «срезаем» через забор)
        // - внутри кампуса  → ближайший узел графа
        val insideCampus = isInsideCampus(userLocation)
        val startNode = if (insideCampus) {
            graph.nodes
                .minByOrNull { it.location.distanceToAsDouble(userLocation) }
        } else {
            graph.nodes
                .filter { it.id.startsWith("KPP") }
                .minByOrNull { it.location.distanceToAsDouble(userLocation) }
        }

        // Запускаем Дейкстру до КАЖДОГО входа здания, берём минимум по дистанции графа.
        // Это гарантирует выбор входа с кратчайшим реальным маршрутом, а не прямой.
        data class Candidate(
            val entrance: Entrance,
            val result: DijkstraResult
        )

        val best: Candidate? = if (startNode != null) {
            targetBuilding.entrances
                .mapNotNull { entrance ->
                    if (startNode.id == entrance.id) return@mapNotNull null
                    val r = dijkstra(startNode.id, entrance.id) ?: return@mapNotNull null
                    Candidate(entrance, r)
                }
                .minByOrNull { it.result.totalDistance }
        } else null

        val walkingDist: Double
        val routePoints: List<GeoPoint>
        val toEntrance: Entrance

        if (best != null) {
            toEntrance = best.entrance
            walkingDist = best.result.totalDistance
            // Снаружи кампуса: маршрут начинается с КПП-узла, не с GPS пользователя
            // (чтобы не рисовать линию через забор).
            // Внутри кампуса: добавляем userLocation в начало.
            val pts = mutableListOf<GeoPoint>()
            if (insideCampus) pts.add(userLocation)
            best.result.path.mapNotNullTo(pts) { nodeId -> nodeMap[nodeId]?.location }
            if (pts.lastOrNull() != toEntrance.location) pts.add(toEntrance.location)
            routePoints = pts
        } else {
            // Fallback: прямая до ближайшего входа по прямой
            toEntrance = targetBuilding.entrances
                .minByOrNull { it.location.distanceToAsDouble(userLocation) }!!
            walkingDist = userLocation.distanceToAsDouble(toEntrance.location)
            routePoints = listOf(userLocation, toEntrance.location)
        }

        return Route(
            points = routePoints,
            distanceM = walkingDist,
            indoorSteps = buildIndoorSteps(targetRoom, targetBuilding),
            toEntrance = toEntrance
        )
    }

    // ── Дейкстра ──────────────────────────────────────────────────────────────

    data class DijkstraResult(val path: List<String>, val totalDistance: Double)

    private fun dijkstra(startId: String, endId: String): DijkstraResult? {
        if (!adj.containsKey(startId) || !adj.containsKey(endId)) return null

        val dist = HashMap<String, Double>(adj.size).apply {
            adj.keys.forEach { put(it, Double.MAX_VALUE) }
            put(startId, 0.0)
        }
        val prev = HashMap<String, String?>(adj.size)
        val pq = PriorityQueue<Pair<Double, String>>(compareBy { it.first })
        pq.add(0.0 to startId)
        val visited = HashSet<String>(adj.size)

        while (pq.isNotEmpty()) {
            val (d, u) = pq.poll()
            if (!visited.add(u)) continue
            if (u == endId) break
            for ((v, w) in adj[u] ?: continue) {
                if (v in visited) continue
                val nd = d + w
                if (nd < (dist[v] ?: Double.MAX_VALUE)) {
                    dist[v] = nd
                    prev[v] = u
                    pq.add(nd to v)
                }
            }
        }

        val finalDist = dist[endId] ?: return null
        if (finalDist == Double.MAX_VALUE) return null

        val path = ArrayDeque<String>()
        var cur: String? = endId
        while (cur != null) { path.addFirst(cur); cur = prev[cur] }
        return DijkstraResult(path.toList(), finalDist)
    }

    // ── Проверка: пользователь внутри кампуса? (ray casting) ─────────────────

    private fun isInsideCampus(point: GeoPoint): Boolean {
        var count = 0
        val n = CAMPUS_PERIMETER.size
        for (i in 0 until n - 1) {   // последняя точка == первая, пропускаем
            if (perimeterRayIntersects(point, CAMPUS_PERIMETER[i], CAMPUS_PERIMETER[i + 1]))
                count++
        }
        return count % 2 == 1
    }

    /** Ray casting по longitude/latitude (луч вправо по долготе). */
    private fun perimeterRayIntersects(p: GeoPoint, a: GeoPoint, b: GeoPoint): Boolean {
        var (ax, ay) = a.longitude to a.latitude
        var (bx, by) = b.longitude to b.latitude
        val (px, py) = p.longitude to p.latitude
        if (ay > by) { val tmp = ax to ay; ax = bx; ay = by; bx = tmp.first; by = tmp.second }
        if (py <= ay || py > by) return false
        if (px > maxOf(ax, bx)) return false
        if (px < minOf(ax, bx)) return true
        val slope = if (bx != ax) (by - ay) / (bx - ax) else Double.MAX_VALUE
        val intersectX = ax + (py - ay) / slope
        return px < intersectX
    }

    // ── Внутренние подсказки ──────────────────────────────────────────────────

    private fun buildIndoorSteps(room: Room?, building: Building): List<String> {
        if (room == null) return emptyList()
        return buildList {
            add("Войдите в ${building.name}")
            if (room.floor > 1) add("Поднимитесь на ${room.floor} этаж")
            add("Найдите аудиторию ${room.number}" +
                if (room.name.isNotBlank()) " (${room.name})" else "")
        }
    }
}
