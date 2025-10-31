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
import com.example.unimarket.model.domain.entity.Product
import java.text.NumberFormat
import java.util.Locale

class MenuProductAdapter(
    private var items: List<Product>,
    private val onAdd: (Product) -> Unit
) : RecyclerView.Adapter<MenuProductAdapter.MenuProductViewHolder>() {

    fun submit(data: List<Product>) {
        items = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_business_menu_product, parent, false)
        return MenuProductViewHolder(view, onAdd)
    }

    override fun onBindViewHolder(holder: MenuProductViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class MenuProductViewHolder(
        itemView: View,
        private val onAdd: (Product) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val image: ImageView = itemView.findViewById(R.id.imgProduct)
        private val title: TextView = itemView.findViewById(R.id.tvProductName)
        private val subtitle: TextView = itemView.findViewById(R.id.tvProductSubtitle)
        private val price: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val rating: TextView = itemView.findViewById(R.id.tvProductRating)
        private val btnAdd: ImageButton = itemView.findViewById(R.id.btnAdd)

        fun bind(item: Product) {
            val context = itemView.context
            val resolvedName = item.name.ifBlank {
                item.description.ifBlank {
                    item.category.ifBlank { context.getString(R.string.business_menu_product_placeholder) }
                }
            }
            val resolvedSubtitle = item.description.takeIf { it.isNotBlank() }
                ?: item.category.ifBlank { context.getString(R.string.business_menu_product_placeholder) }

            title.text = resolvedName
            subtitle.text = resolvedSubtitle
            price.text = formatPrice(item.price)
            rating.text = formatRating(item.rating)

            if (item.image.isBlank()) {
                image.setImageResource(R.drawable.personajesingup)
            } else {
                image.load(item.image) {
                    crossfade(true)
                    placeholder(R.drawable.personajesingup)
                    error(R.drawable.personajesingup)
                }
            }

            btnAdd.setOnClickListener { onAdd(item) }
        }

        private fun formatRating(value: Double): String {
            return if (value > 0.0) String.format(Locale.US, "%.1f", value) else "4.0"
        }

        private fun formatPrice(amount: Double): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
                maximumFractionDigits = 0
            }
            val formatted = formatter.format(amount)
            return formatted.replace('\u00A0', ' ')
        }
    }
}