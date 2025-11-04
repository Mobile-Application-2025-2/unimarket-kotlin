package com.example.unimarket.view.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.unimarket.R

class BusinessProductsAdapter(
    private val onAddClick: (BusinessProductItem) -> Unit
) : RecyclerView.Adapter<BusinessProductsAdapter.ProductViewHolder>() {

    private val items = mutableListOf<BusinessProductItem>()

    fun submit(newItems: List<BusinessProductItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_business_product, parent, false)
        return ProductViewHolder(view, onAddClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ProductViewHolder(
        itemView: View,
        private val onAddClick: (BusinessProductItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        private val tvName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvProductSubtitle)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val tvRating: TextView = itemView.findViewById(R.id.tvProductRating)
        private val btnAdd: ImageButton = itemView.findViewById(R.id.btnAdd)

        fun bind(item: BusinessProductItem) {
            tvName.text = item.name
            tvSubtitle.text = item.subtitle
            tvPrice.text = item.price
            tvRating.text = item.rating

            val url = item.imageUrl.trim()
            if (url.isNotEmpty()) {
                imgProduct.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.personajesingup)
                    error(R.drawable.personajesingup)
                }
            } else {
                imgProduct.setImageResource(R.drawable.personajesingup)
            }

            itemView.setOnClickListener { onAddClick(item) }
            btnAdd.setOnClickListener { onAddClick(item) }
        }
    }
}