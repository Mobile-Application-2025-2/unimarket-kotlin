package com.example.unimarket.view.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unimarket.R
import coil.load

data class CartItemUi(
    val id: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String?
)

class CartProductAdapter(
    private val onPlusClick: (CartItemUi) -> Unit,
    private val onMinusClick: (CartItemUi) -> Unit,
    private val onRemoveClick: (CartItemUi) -> Unit
) : RecyclerView.Adapter<CartProductAdapter.CartViewHolder>() {

    private val items = mutableListOf<CartItemUi>()

    fun submit(newItems: List<CartItemUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_product, parent, false)
        return CartViewHolder(view, onPlusClick, onMinusClick, onRemoveClick)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class CartViewHolder(
        itemView: View,
        private val onPlusClick: (CartItemUi) -> Unit,
        private val onMinusClick: (CartItemUi) -> Unit,
        private val onRemoveClick: (CartItemUi) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivProduct: ImageView    = itemView.findViewById(R.id.ivProduct)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvProductPrice: TextView= itemView.findViewById(R.id.tvProductPrice)
        private val tvQuantity: TextView    = itemView.findViewById(R.id.tvQuantity)

        private val btnRemove: ImageButton  = itemView.findViewById(R.id.btnRemove)
        private val btnMinus: ImageButton   = itemView.findViewById(R.id.btnMinus)
        private val btnPlus: ImageButton    = itemView.findViewById(R.id.btnPlus)

        fun bind(item: CartItemUi) {
            tvProductName.text = item.name
            tvQuantity.text = item.quantity.toString()
            tvProductPrice.text = formatPrice(item.price)

            val url = item.imageUrl?.trim().orEmpty()
            if (url.isNotEmpty()) {
                ivProduct.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.personajesingup)
                    error(R.drawable.personajesingup)
                    
                }
            } else {
                ivProduct.setImageResource(R.drawable.personajesingup)
            }

            btnPlus.setOnClickListener  { onPlusClick(item) }
            btnMinus.setOnClickListener { onMinusClick(item) }
            btnRemove.setOnClickListener{ onRemoveClick(item) }
        }

        private fun formatPrice(price: Double): String {
            return String.format("$%,.0f", price)
        }
    }
}