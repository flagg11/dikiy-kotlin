package com.rut.campusnavigation.presentation.favorites

import android.view.*
import androidx.recyclerview.widget.*
import com.rut.campusnavigation.R
import com.rut.campusnavigation.data.local.entity.FavoriteEntity
import com.rut.campusnavigation.databinding.ItemFavoriteBinding

class FavoritesAdapter(
    private val onItemClick: (FavoriteEntity) -> Unit,
    private val onRemoveClick: (FavoriteEntity) -> Unit
) : ListAdapter<FavoriteEntity, FavoritesAdapter.VH>(Diff()) {

    inner class VH(private val b: ItemFavoriteBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FavoriteEntity) {
            b.tvName.text = item.name
            b.ivIcon.setImageResource(if (item.type == "building") R.drawable.ic_building else R.drawable.ic_room)
            b.root.setOnClickListener { onItemClick(item) }
            b.btnRemove.setOnClickListener { onRemoveClick(item) }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemFavoriteBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
    class Diff : DiffUtil.ItemCallback<FavoriteEntity>() {
        override fun areItemsTheSame(a: FavoriteEntity, b: FavoriteEntity) = a.id == b.id
        override fun areContentsTheSame(a: FavoriteEntity, b: FavoriteEntity) = a == b
    }
}
