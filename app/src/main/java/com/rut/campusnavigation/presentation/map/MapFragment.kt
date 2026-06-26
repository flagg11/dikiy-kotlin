package com.rut.campusnavigation.presentation.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.*
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.rut.campusnavigation.R
import com.rut.campusnavigation.databinding.FragmentMapBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// Корпус 1 РУТ МИИТ — ул. Образцова, 9с9 (начальный центр карты)
// Метро: Новослободская / Менделеевская / Достоевская / Марьина Роща
private val CAMPUS_CENTER = GeoPoint(55.787989, 37.608329)

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by activityViewModels()
    private lateinit var campusOverlay: CampusOverlay
    private lateinit var locationOverlay: MyLocationNewOverlay

    /** true — панель сейчас видима (или анимируется в показ) */
    private var panelVisible = false

    /**
     * true — пользователь свернул панель кнопкой, но здание остаётся выделенным.
     * Сбрасывается при смене здания или повторном тапе по тому же корпусу.
     */
    private var panelCollapsed = false

    /**
     * Защита от случайного onEmptyTap при возврате из другого фрагмента.
     * Игнорируем пустые тапы в течение 300 мс после onResume.
     */
    private var emptyTapGuardUntil = 0L

    private val locationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) enableMyLocation()
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentMapBinding.inflate(i, c, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap(); setupButtons(); observeState()
        requestLocation()
        // Панель изначально за нижним краем экрана
        binding.bottomSheet.visibility = View.INVISIBLE
        binding.bottomSheet.translationY = 2000f   // за экраном, точная высота — в showPanel()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        // Ставим защиту: игнорируем пустые тапы 300 мс после возврата на экран
        emptyTapGuardUntil = System.currentTimeMillis() + 300L
    }
    override fun onPause()  { super.onPause();  binding.mapView.onPause()  }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    // ─── Карта ───────────────────────────────────────────────────────────────

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 15.0; maxZoomLevel = 20.0
            controller.setZoom(18.5); controller.setCenter(CAMPUS_CENTER)
        }
        campusOverlay = CampusOverlay(
            requireContext(),
            onBuildingClick = { building ->
                val currentId = viewModel.uiState.value.selectedBuilding?.id
                if (building.id == currentId && panelCollapsed) {
                    // То же здание, панель была свёрнута — раскрываем
                    panelCollapsed = false
                    showPanel()
                } else {
                    viewModel.selectBuilding(building)
                }
            },
            onEmptyTap = {
                if (System.currentTimeMillis() > emptyTapGuardUntil) {
                    viewModel.clearSelection()
                }
            }
        )
        campusOverlay.buildings = viewModel.buildings
        binding.mapView.overlays.add(campusOverlay)

        // DEBUG: показывает координаты при долгом нажатии (убери потом)
        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                p ?: return false
                val msg = "%.6f, %.6f".format(p.latitude, p.longitude)
                android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG).show()
                android.util.Log.d("COORDS", "GeoPoint($msg)")
                return true
            }
        })
        binding.mapView.overlays.add(0, eventsOverlay)
    }

    private fun enableMyLocation() {
        locationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(requireContext()), binding.mapView
        ).also { it.enableMyLocation() }
        binding.mapView.overlays.add(locationOverlay)
    }

    // ─── Кнопки ──────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.fabSearch.setOnClickListener {
            viewModel.clearSelection()
            findNavController().navigate(R.id.action_mapFragment_to_searchFragment)
        }
        binding.fabFavorites.setOnClickListener {
            viewModel.clearSelection()
            findNavController().navigate(R.id.action_mapFragment_to_favoritesFragment)
        }
        binding.fabCenter.setOnClickListener {
            binding.mapView.controller.animateTo(CAMPUS_CENTER)
        }
        binding.fabLocateMe.setOnClickListener {
            val loc = if (::locationOverlay.isInitialized) locationOverlay.myLocation else null
            if (loc != null) {
                binding.mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
            } else {
                Snackbar.make(binding.root, "GPS недоступен", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnFavorite.setOnClickListener { viewModel.toggleFavorite() }
        binding.btnRoute.setOnClickListener {
            val loc = if (::locationOverlay.isInitialized) locationOverlay.myLocation else null
            viewModel.buildRoute(
                if (loc != null) GeoPoint(loc.latitude, loc.longitude)
                else GeoPoint(55.787335, 37.608694) // КПП-1 (ул. Образцова)
            )
            if (loc == null)
                Snackbar.make(binding.root, "Геолокация недоступна — маршрут от КПП-1", Snackbar.LENGTH_SHORT).show()
        }
        binding.btnClearRoute.setOnClickListener { viewModel.clearRoute() }
        binding.btnClose.setOnClickListener {
            panelCollapsed = true
            hidePanel()
        }
    }

    // ─── Состояние ───────────────────────────────────────────────────────────

    private var lastAnimatedBuildingId: String? = null
    private var lastSelectedRoomId: String? = null

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Сбрасываем collapsed ДО updatePanel, иначе панель не покажется.
                    // Также сбрасываем при смене аудитории (даже если корпус тот же).
                    val buildingChanged = state.selectedBuilding?.id != lastAnimatedBuildingId
                    val roomChanged = state.selectedRoom?.id != lastSelectedRoomId
                    if (buildingChanged || roomChanged) {
                        panelCollapsed = false
                        lastAnimatedBuildingId = state.selectedBuilding?.id
                        lastSelectedRoomId = state.selectedRoom?.id
                    }
                    updatePanel(state)
                    // Перерисовываем карту только если изменилось то, что на ней рисуется
                    val mapNeedsRedraw = buildingChanged ||
                        (state.route !== campusOverlay.route)
                    campusOverlay.selectedBuilding = state.selectedBuilding
                    campusOverlay.route = state.route
                    if (mapNeedsRedraw) binding.mapView.invalidate()
                    if (buildingChanged) {
                        state.selectedBuilding?.let { binding.mapView.controller.animateTo(it.center) }
                    }
                }
            }
        }
    }

    private fun updatePanel(state: MapUiState) {
        if (state.selectedBuilding == null) {
            panelCollapsed = false
            hidePanel(); return
        }

        // Заполняем контент
        if (state.selectedRoom != null) {
            val fullNum = "${state.selectedBuilding.num}${state.selectedRoom.number}"
            binding.tvBuildingName.text = "Ауд. $fullNum"
            binding.tvBuildingDescription.text =
                "${state.selectedBuilding.name}, ${state.selectedRoom.floor} этаж"
        } else {
            binding.tvBuildingName.text = state.selectedBuilding.name
            binding.tvBuildingDescription.text = state.selectedBuilding.description
        }
        binding.btnFavorite.setIconResource(
            if (state.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )
        if (state.route != null) {
            binding.routePanel.visibility = View.VISIBLE
            val d = state.route.distanceM
            binding.tvRouteDistance.text =
                if (d > 1000) "До входа: %.1f км".format(d / 1000) else "До входа: ${d.toInt()} м"
            binding.tvIndoorSteps.text = state.route.indoorSteps.joinToString("\n")
            binding.tvIndoorSteps.visibility =
                if (state.route.indoorSteps.isEmpty()) View.GONE else View.VISIBLE
        } else {
            binding.routePanel.visibility = View.GONE
        }
        state.error?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() }

        if (!panelCollapsed) showPanel()
    }

    /** Показать панель снизу с анимацией */
    private fun showPanel() {
        if (panelVisible) return
        panelVisible = true
        val panel = binding.bottomSheet
        panel.visibility = View.VISIBLE
        // Если высота ещё не известна — ждём layout
        if (panel.height == 0) {
            panel.post { slideIn(panel) }
        } else {
            slideIn(panel)
        }
    }

    private fun slideIn(panel: View) {
        val shift = panel.height.toFloat()
        panel.translationY = shift
        panel.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
        // Поднимаем fabCenter вместе с панелью — без layout passes, без прыжков
        binding.fabCenter.animate()
            .translationY(-shift)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /** Скрыть панель с анимацией */
    private fun hidePanel() {
        if (!panelVisible) return
        panelVisible = false
        val panel = binding.bottomSheet
        panel.animate()
            .translationY(panel.height.toFloat())
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { if (_binding != null) panel.visibility = View.GONE }
            .start()
        // Возвращаем fabCenter на исходную позицию
        binding.fabCenter.animate()
            .translationY(0f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun requestLocation() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        val ctx = requireContext()
        if (ContextCompat.checkSelfPermission(ctx, fine)   == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, coarse) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        } else {
            locationLauncher.launch(arrayOf(fine, coarse))
        }
    }
}
