package com.rut.campusnavigation.presentation.search

import android.os.Bundle
import android.view.*
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.*
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rut.campusnavigation.databinding.FragmentSearchBinding
import com.rut.campusnavigation.domain.usecase.SearchResult
import com.rut.campusnavigation.presentation.map.MapViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private val mapViewModel: MapViewModel by viewModels({ requireActivity() })
    private lateinit var adapter: SearchResultsAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentSearchBinding.inflate(i, c, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SearchResultsAdapter { result ->
            when (result) {
                is SearchResult.BuildingResult -> mapViewModel.selectBuilding(result.building)
                is SearchResult.RoomResult -> mapViewModel.selectRoom(result.room, result.building)
            }
            findNavController().navigateUp()
        }
        binding.rvResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResults.adapter = adapter
        binding.etSearch.doAfterTextChanged { viewModel.onQueryChanged(it?.toString() ?: "") }
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.results.collect {
                    adapter.submitList(it)
                    binding.tvEmpty.visibility =
                        if (it.isEmpty() && viewModel.query.value.isNotBlank()) View.VISIBLE else View.GONE
                }
            }
        }
        binding.etSearch.requestFocus()
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
