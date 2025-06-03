package com.devshiv.holesale.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devshiv.holesale.R
import com.devshiv.holesale.databinding.MainItemLayoutBinding
import com.devshiv.holesale.model.ListModel

class MainAdapter(
    private val listModel: MutableList<ListModel>,
    private val context: Context,
    val onItemClick: (ListModel) -> Unit
) : RecyclerView.Adapter<MainAdapter.MainViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = MainItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val data = listModel[position]
        holder.binding.phoneNoPreview.text = data.phoneNumber
        Glide.with(context)
            .load(data.imageUrl)
            .placeholder(R.drawable.ic_image_not_found)
            .into(holder.binding.previewImg)
        holder.binding.addressPreview.text = data.address
        holder.binding.datePreview.text = ListModel.formatDate(data.updatedAt)
        holder.binding.timePreview.text = ListModel.formatTime(data.updatedAt)
        holder.itemView.setOnClickListener {
            onItemClick(data)
        }
    }

    override fun getItemCount(): Int = listModel.size

    fun updateList(newList: List<ListModel>) {
        listModel.clear()
        listModel.addAll(newList)
        notifyDataSetChanged()
    }


    inner class MainViewHolder(val binding: MainItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

}
