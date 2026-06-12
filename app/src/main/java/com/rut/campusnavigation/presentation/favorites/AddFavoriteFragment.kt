package com.rut.campusnavigation.presentation.favorites

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rut.campusnavigation.databinding.FragmentAddFavoriteBinding
import com.rut.campusnavigation.domain.usecase.FavoritesUseCase
import com.rut.campusnavigation.domain.usecase.SearchResult
import com.rut.campusnavigation.presentation.search.SearchResultsAdapter
import com.rut.campusnavigation.presentation.search.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddFavoriteFragment : Fragment() {
    private var _b: FragmentAddFavoriteBinding? = null
    private val b get() = _b!!
    private val searchViewModel: SearchViewModel by viewModels()
    @Inject lateinit var favoritesUseCase: FavoritesUseCase
    private lateinit var adapter: SearchResultsAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentAddFavoriteBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SearchResultsAdapter { result ->
            viewLifecycleOwner.lifecycleScope.launch {
                when (result) {
                    is SearchResult.BuildingResult ->
                        favoritesUseCase.toggleBuildingFavorite(result.building, false)
                    is SearchResult.RoomResult ->
                        favoritesUseCase.toggleRoomFavorite(result.room, result.building, false)
                }
                Toast.makeText(requireContext(), "Добавлено в избранное", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        b.rvResults.layoutManager = LinearLayoutManager(requireContext())
        b.rvResults.adapter = adapter
        b.etSearch.doAfterTextChanged { searchViewModel.onQueryChanged(it?.toString() ?: "") }
        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchViewModel.results.collect { results ->
                    adapter.submitList(results)
                    b.tvEmpty.visibility =
                        if (results.isEmpty() && searchViewModel.query.value.isNotBlank()) View.VISIBLE else View.GONE
                }
            }
        }
        b.etSearch.requestFocus()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
