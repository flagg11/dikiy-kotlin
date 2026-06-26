package com.rut.campusnavigation.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rut.campusnavigation.domain.model.*
import com.rut.campusnavigation.domain.repository.CampusRepository
import com.rut.campusnavigation.domain.usecase.FavoritesUseCase
import com.rut.campusnavigation.domain.usecase.GetRouteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

data class MapUiState(
    val selectedBuilding: Building? = null,
    val selectedRoom: Room? = null,
    val route: Route? = null,
    val isFavorite: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val campusRepository: CampusRepository,
    private val getRouteUseCase: GetRouteUseCase,
    private val favoritesUseCase: FavoritesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    val buildings: List<Building> = campusRepository.getAllBuildings()

    private var routeJob: Job? = null

    fun selectBuilding(building: Building) = viewModelScope.launch {
        _uiState.update { it.copy(selectedBuilding = building, selectedRoom = null, route = null) }
        favoritesUseCase.isFavorite(building.id).take(1)
            .collect { isFav -> _uiState.update { it.copy(isFavorite = isFav) } }
    }

    fun selectRoom(room: Room, building: Building) = viewModelScope.launch {
        _uiState.update { it.copy(selectedBuilding = building, selectedRoom = room, route = null) }
        favoritesUseCase.isFavorite(room.id).take(1)
            .collect { isFav -> _uiState.update { it.copy(isFavorite = isFav) } }
    }

    fun clearSelection() { _uiState.update { MapUiState() } }

    fun buildRoute(userLocation: GeoPoint) {
        val building = _uiState.value.selectedBuilding ?: return
        val room = _uiState.value.selectedRoom
        routeJob?.cancel()
        routeJob = viewModelScope.launch(Dispatchers.Default) {
            val route = getRouteUseCase(userLocation, building, room)
            if (isActive) {
                _uiState.update {
                    if (route != null) it.copy(route = route, error = null)
                    else it.copy(error = "Marshrut ne naiden")
                }
            }
        }
    }

    fun clearRoute() { _uiState.update { it.copy(route = null) } }

    fun toggleFavorite() = viewModelScope.launch {
        val s = _uiState.value
        val building = s.selectedBuilding ?: return@launch
        if (s.selectedRoom != null)
            favoritesUseCase.toggleRoomFavorite(s.selectedRoom, building, s.isFavorite)
        else
            favoritesUseCase.toggleBuildingFavorite(building, s.isFavorite)
        _uiState.update { it.copy(isFavorite = !s.isFavorite) }
    }
}
