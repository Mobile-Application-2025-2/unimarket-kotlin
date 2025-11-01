package com.example.unimarket.view.profile.manage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.unimarket.R
import com.example.unimarket.databinding.ItemBusinessOwnerProductBinding

class BusinessOwnerProductsAdapter(
    private val onEdit: (BusinessOwnerProductItem) -> Unit
) : ListAdapter<BusinessOwnerProductItem, BusinessOwnerProductsAdapter.ProductViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemBusinessOwnerProductBinding.inflate(inflater, parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemBusinessOwnerProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BusinessOwnerProductItem) {
            binding.tvProductName.text = item.name
            binding.tvProductCategory.text = item.categoryLabel
            binding.tvProductPrice.text = item.priceLabel
            binding.tvProductRating.text = item.ratingLabel

            val placeholder = R.drawable.formas
            binding.imgProduct.load(item.imageUrl.ifBlank { null }) {
                crossfade(true)
                placeholder(placeholder)
                error(placeholder)
            }

            binding.btnEdit.setOnClickListener { onEdit(item) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<BusinessOwnerProductItem>() {
        override fun areItemsTheSame(
            oldItem: BusinessOwnerProductItem,
            newItem: BusinessOwnerProductItem
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: BusinessOwnerProductItem,
            newItem: BusinessOwnerProductItem
        ): Boolean = oldItem == newItem
    }
}