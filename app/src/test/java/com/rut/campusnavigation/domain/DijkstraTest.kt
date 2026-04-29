package com.rut.campusnavigation.domain

import com.rut.campusnavigation.domain.model.*
import com.rut.campusnavigation.domain.usecase.GetRouteUseCase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import org.osmdroid.util.GeoPoint

class DijkstraTest {

    private val repo = mockk<com.rut.campusnavigation.domain.repository.CampusRepository>()
    private val useCase = GetRouteUseCase(repo)

    /** Строит граф: A -- 10m --> B -- 5m --> C. Кратчайший путь A->C = 15m. */
    @Test
    fun `dijkstra finds shortest path in simple graph`() {
        val graph = CampusGraph(
            nodes = listOf(
                PathNode("A", GeoPoint(0.0, 0.0)),
                PathNode("B", GeoPoint(0.0001, 0.0)),
                PathNode("C", GeoPoint(0.0002, 0.0))
            ),
            edges = listOf(
                PathEdge("A", "B", 10.0),
                PathEdge("B", "C", 5.0)
            )
        )
        val result = useCase.dijkstra(graph, "A", "C")
        assertNotNull(result)
        assertEquals(listOf("A", "B", "C"), result!!.path)
        assertEquals(15.0, result.totalDistance, 0.001)
    }

    /** Кратчайший из двух путей: A->B->C (10+5=15) vs A->C напрямую (20). */
    @Test
    fun `dijkstra prefers shorter path`() {
        val graph = CampusGraph(
            nodes = listOf(
                PathNode("A", GeoPoint(0.0, 0.0)),
                PathNode("B", GeoPoint(0.0001, 0.0)),
                PathNode("C", GeoPoint(0.0002, 0.0))
            ),
            edges = listOf(
                PathEdge("A", "B", 10.0),
                PathEdge("B", "C", 5.0),
                PathEdge("A", "C", 20.0)
            )
        )
        val result = useCase.dijkstra(graph, "A", "C")
        assertNotNull(result)
        assertEquals(15.0, result!!.totalDistance, 0.001)
    }

    /** Если пути нет — возвращает null. */
    @Test
    fun `dijkstra returns null when no path exists`() {
        val graph = CampusGraph(
            nodes = listOf(
                PathNode("A", GeoPoint(0.0, 0.0)),
                PathNode("B", GeoPoint(0.0001, 0.0))
            ),
            edges = emptyList()
        )
        val result = useCase.dijkstra(graph, "A", "B")
        assertNull(result)
    }
}
