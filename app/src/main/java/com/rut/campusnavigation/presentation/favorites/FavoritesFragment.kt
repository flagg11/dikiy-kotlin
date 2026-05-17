package com.rut.campusnavigation.presentation.favorites

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rut.campusnavigation.R
import com.rut.campusnavigation.databinding.FragmentFavoritesBinding
import com.rut.campusnavigation.domain.repository.CampusRepository
import com.rut.campusnavigation.domain.usecase.FavoritesUseCase
import com.rut.campusnavigation.presentation.map.MapViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FavoritesFragment : Fragment() {
    private var _b: FragmentFavoritesBinding? = null
    private val b get() = _b!!
    private val mapViewModel: MapViewModel by viewModels({ requireActivity() })
    @Inject lateinit var campusRepository: CampusRepository
    @Inject lateinit var favoritesUseCase: FavoritesUseCase
    private lateinit var adapter: FavoritesAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentFavoritesBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FavoritesAdapter(
            onItemClick = { fav ->
                val building = campusRepository.getBuildingById(fav.buildingId) ?: return@FavoritesAdapter
                if (fav.type == "building") mapViewModel.selectBuilding(building)
                else building.rooms.find { it.id == fav.id }?.let { mapViewModel.selectRoom(it, building) }
                findNavController().navigate(R.id.action_favoritesFragment_to_mapFragment)
            },
            onRemoveClick = { fav ->
                viewLifecycleOwner.lifecycleScope.launch {
                    if (fav.type == "building") {
                        campusRepository.getBuildingById(fav.buildingId)?.let {
                            favoritesUseCase.toggleBuildingFavorite(it, true)
                        }
                    } else {
                        val building = campusRepository.getBuildingById(fav.buildingId)
                        val room = building?.rooms?.find { it.id == fav.id }
                        if (building != null && room != null) {
                            favoritesUseCase.toggleRoomFavorite(room, building, true)
                        }
                    }
                }
            }
        )

        b.rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        b.rvFavorites.adapter = adapter

        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_favoritesFragment_to_addFavoriteFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                favoritesUseCase.getAllFavorites().collect {
                    adapter.submitList(it)
                    b.tvEmpty.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
