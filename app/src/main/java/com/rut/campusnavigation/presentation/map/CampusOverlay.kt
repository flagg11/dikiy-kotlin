package com.rut.campusnavigation.presentation.map

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.rut.campusnavigation.domain.model.Building
import com.rut.campusnavigation.domain.model.Route
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.*

class CampusOverlay(
    context: Context,
    private val onBuildingClick: (Building) -> Unit,
    private val onEmptyTap: () -> Unit = {}
) : Overlay() {

    var buildings: List<Building> = emptyList()
    var selectedBuilding: Building? = null
    var route: Route? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2D6A9F"); alpha = 160; style = Paint.Style.FILL
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E84C3D"); alpha = 200; style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; strokeWidth = 3f; style = Paint.Style.STROKE
    }
    // Крупный номер корпуса
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 48f
        typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
    }
    // Подпись под номером
    private val subLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0"); textSize = 24f
        typeface = Typeface.DEFAULT; textAlign = Paint.Align.CENTER
    }
    // Тень для лучшей читаемости
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88000000"); textSize = 48f
        typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
    }
    // Вход — обычный
    private val entrancePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#27AE60"); style = Paint.Style.FILL
    }
    // Вход — при выделении здания
    private val entranceSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2ECC71"); style = Paint.Style.FILL
    }
    // Стрелка к входу (контур + заливка)
    private val arrowStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E84C3D"); strokeWidth = 5f; style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val arrowFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E84C3D"); style = Paint.Style.FILL
    }
    // DashPathEffect отключает GPU-ускорение → software rendering → лаг на устройстве.
    private val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E84C3D"); strokeWidth = 10f; style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND; alpha = 210
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        // Проход 1: все полигоны + подписи
        buildings.forEach { drawBuilding(canvas, it, mapView) }
        // Проход 2: входы и стрелки выделенного здания ПОВЕРХ всех полигонов
        selectedBuilding?.let { sel ->
            val proj = mapView.projection
            val pts = sel.polygon.map {
                val p = proj.toPixels(it, null)
                PointF(p.x.toFloat(), p.y.toFloat())
            }
            val centerPx = proj.toPixels(sel.center, null)
            val cx = centerPx.x.toFloat(); val cy = centerPx.y.toFloat()
            sel.entrances.forEach { e ->
                val ep = proj.toPixels(e.location, null)
                val ex = ep.x.toFloat(); val ey = ep.y.toFloat()
                canvas.drawCircle(ex, ey, 18f, entranceSelectedPaint)
                canvas.drawCircle(ex, ey, 18f, strokePaint)
                drawEntranceArrow(canvas, ex, ey, pts, cx, cy, 18f)
            }
        }
        // Проход 3: маршрут ПОВЕРХ всех зданий и входов (стандартный UX навигации)
        route?.let { drawRoute(canvas, it, mapView) }
    }

    private fun drawBuilding(canvas: Canvas, building: Building, mapView: MapView) {
        val proj = mapView.projection
        val pts = building.polygon.map {
            val p = proj.toPixels(it, null)
            PointF(p.x.toFloat(), p.y.toFloat())
        }
        if (pts.size < 3) return
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
            close()
        }
        val isSelected = building.id == selectedBuilding?.id
        canvas.drawPath(path, if (isSelected) selectedPaint else fillPaint)
        canvas.drawPath(path, strokePaint)

        // Подписи корпусов
        val c = proj.toPixels(building.center, null)
        val cx = c.x.toFloat()
        val cy = c.y.toFloat()
        val zoom = mapView.zoomLevelDouble
        if (zoom >= 15.5) {
            canvas.drawText(building.shortName, cx + 2f, cy + 2f + labelPaint.textSize / 3, shadowPaint)
            canvas.drawText(building.shortName, cx, cy + labelPaint.textSize / 3, labelPaint)
            if (zoom >= 17.0) {
                canvas.drawText(building.name.substringBefore(" ("), cx, cy + labelPaint.textSize + 4f, subLabelPaint)
            }
        }

        // Входы не-выделенного здания при большом зуме (только кружки, без стрелок)
        // Входы выделенного здания рисуются в draw() поверх всех полигонов
        if (!isSelected && zoom >= 17.0) {
            building.entrances.forEach { e ->
                val ep = proj.toPixels(e.location, null)
                canvas.drawCircle(ep.x.toFloat(), ep.y.toFloat(), 14f, entrancePaint)
                canvas.drawCircle(ep.x.toFloat(), ep.y.toFloat(), 14f, strokePaint)
            }
        }
    }

    /**
     * Стрелка перпендикулярна ближайшей стене здания.
     * Острие — у края кружка входа (со стороны улицы), хвост — снаружи на улице.
     * Т.е. стрелка указывает ОТ улицы К входу.
     */
    private fun drawEntranceArrow(
        canvas: Canvas,
        ex: Float, ey: Float,        // центр окружности входа
        polygonPts: List<PointF>,    // пиксельные вершины полигона здания
        cx: Float, cy: Float,        // центр здания в пикселях (для ориентации нормали)
        circleRadius: Float
    ) {
        if (polygonPts.size < 2) return

        // Находим ближайшее ребро полигона к точке входа
        var minDist = Float.MAX_VALUE
        var outNx = 0f; var outNy = 0f
        val n = polygonPts.size
        for (i in 0 until n) {
            val ax = polygonPts[i].x;           val ay = polygonPts[i].y
            val bx = polygonPts[(i + 1) % n].x; val by = polygonPts[(i + 1) % n].y
            val edgeDx = bx - ax; val edgeDy = by - ay
            val edgeLen = hypot(edgeDx, edgeDy)
            if (edgeLen < 1f) continue

            // Проекция входа на ребро
            val t = ((ex - ax) * edgeDx + (ey - ay) * edgeDy) / (edgeLen * edgeLen)
            val tc = t.coerceIn(0f, 1f)
            val closestX = ax + tc * edgeDx; val closestY = ay + tc * edgeDy
            val dist = hypot(ex - closestX, ey - closestY)

            if (dist < minDist) {
                minDist = dist
                // Нормаль к ребру (две кандидата)
                val nx1 = -edgeDy / edgeLen; val ny1 = edgeDx / edgeLen
                // Выбираем ту, что смотрит НАРУЖУ (от центра здания)
                val dot = nx1 * (ex - cx) + ny1 * (ey - cy)
                outNx = if (dot >= 0f) nx1 else -nx1
                outNy = if (dot >= 0f) ny1 else -ny1
            }
        }

        if (hypot(outNx, outNy) < 0.5f) return

        val shaftLen = 48f; val headLen = 20f; val headHalf = 13f

        // Острие — у внешнего края кружка входа (стрелка смотрит на вход)
        val tipX = ex + outNx * circleRadius
        val tipY = ey + outNy * circleRadius
        // База наконечника — дальше наружу
        val baseX = tipX + outNx * headLen
        val baseY = tipY + outNy * headLen
        // Хвост — ещё дальше (на улице)
        val tailX = baseX + outNx * shaftLen
        val tailY = baseY + outNy * shaftLen

        // Ствол
        canvas.drawLine(tailX, tailY, baseX, baseY, arrowStrokePaint)

        // Наконечник — острие ближе к зданию, основание дальше (стрелка → вход)
        val px = -outNy; val py = outNx
        val arrowPath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(baseX + px * headHalf, baseY + py * headHalf)
            lineTo(baseX - px * headHalf, baseY - py * headHalf)
            close()
        }
        canvas.drawPath(arrowPath, arrowFillPaint)
    }

    private fun drawRoute(canvas: Canvas, route: Route, mapView: MapView) {
        if (route.points.size < 2) return
        val proj = mapView.projection
        val path = Path()
        route.points.forEachIndexed { i, gp ->
            val p = proj.toPixels(gp, null)
            if (i == 0) path.moveTo(p.x.toFloat(), p.y.toFloat())
            else path.lineTo(p.x.toFloat(), p.y.toFloat())
        }
        canvas.drawPath(path, routePaint)
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val tapped = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
        val hit = buildings.find { isPointInPolygon(tapped, it.polygon) }
        return if (hit != null) { onBuildingClick(hit); true } else { onEmptyTap(); false }
    }

    private fun isPointInPolygon(p: GeoPoint, poly: List<GeoPoint>): Boolean {
        var count = 0
        val n = poly.size
        for (i in poly.indices) {
            if (rayIntersects(p, poly[i], poly[(i + 1) % n])) count++
        }
        return count % 2 == 1
    }

    /** Ray casting: луч от p вправо, подсчёт пересечений с рёбрами полигона. */
    private fun rayIntersects(p: GeoPoint, a: GeoPoint, b: GeoPoint): Boolean {
        var (ax, ay) = a.longitude to a.latitude
        var (bx, by) = b.longitude to b.latitude
        val (px, py) = p.longitude to p.latitude
        if (ay > by) return rayIntersects(p, b, a)
        if (py == ay || py == by) return rayIntersects(GeoPoint(py + 1e-10, px), a, b)
        if (py > by || py < ay || px > maxOf(ax, bx)) return false
        if (px < minOf(ax, bx)) return true
        val red = if (ax != bx) (by - ay) / (bx - ax) else Double.MAX_VALUE
        val blue = if (ax != px) (py - ay) / (px - ax) else Double.MAX_VALUE
        return blue >= red
    }
}
