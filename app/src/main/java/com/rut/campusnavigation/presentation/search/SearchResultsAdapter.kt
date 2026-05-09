package com.rut.campusnavigation.presentation.search

import android.view.*
import androidx.recyclerview.widget.*
import com.rut.campusnavigation.R
import com.rut.campusnavigation.databinding.ItemSearchResultBinding
import com.rut.campusnavigation.domain.usecase.SearchResult

class SearchResultsAdapter(
    private val onClick: (SearchResult) -> Unit
) : ListAdapter<SearchResult, SearchResultsAdapter.VH>(Diff()) {

    inner class VH(private val b: ItemSearchResultBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: SearchResult) {
            when (item) {
                is SearchResult.BuildingResult -> {
                    b.tvTitle.text = item.building.name
                    b.tvSubtitle.text = item.building.description.take(60)
                    b.ivIcon.setImageResource(R.drawable.ic_building)
                }
                is SearchResult.RoomResult -> {
                    // Полный номер аудитории: корпус + номер. Пример: "8413"
                    val fullNum = "${item.building.num}${item.room.number}"
                    val title = if (item.room.name.isNotBlank())
                        "Ауд. $fullNum — ${item.room.name}"
                    else
                        "Ауд. $fullNum"
                    b.tvTitle.text = title
                    b.tvSubtitle.text = "${item.building.name}, ${item.room.floor} этаж"
                    b.ivIcon.setImageResource(R.drawable.ic_room)
                }
            }
            b.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemSearchResultBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))

    class Diff : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(a: SearchResult, b: SearchResult) = when {
            a is SearchResult.BuildingResult && b is SearchResult.BuildingResult -> a.building.id == b.building.id
            a is SearchResult.RoomResult && b is SearchResult.RoomResult -> a.room.id == b.room.id
            else -> false
        }
        override fun areContentsTheSame(a: SearchResult, b: SearchResult) = a == b
    }
}
