package com.example.unimarket.view.business

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.unimarket.R
import com.example.unimarket.databinding.ItemBusinessProductBinding
import java.text.NumberFormat
import java.util.Locale

data class BusinessProductUi(
    val id: String,
    val name: String,
    val price: Double,
    val rating: Double,
    val imageUrl: String
)

class BusinessProductsAdapter(
    private val onClick: (BusinessProductUi) -> Unit
) : ListAdapter<BusinessProductUi, BusinessProductsAdapter.VH>(DiffCallback) {

    private val currencyFormatter = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemBusinessProductBinding.inflate(inflater, parent, false)
        return VH(binding, currencyFormatter, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemBusinessProductBinding,
        private val formatter: NumberFormat,
        private val onClick: (BusinessProductUi) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BusinessProductUi) {
            binding.root.setOnClickListener { onClick(item) }

            binding.tvProductName.text = item.name
            binding.tvProductPrice.text = "${'$'}${formatter.format(item.price)}"
            binding.tvProductRating.text = String.format(Locale.getDefault(), "%.1f", item.rating)

            val imageUrl = item.imageUrl.orEmpty()
            if (imageUrl.isNotBlank()) {
                binding.imgProduct.load(imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.personajesingup)
                    error(R.drawable.personajesingup)
                }
            } else {
                binding.imgProduct.setImageResource(R.drawable.personajesingup)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<BusinessProductUi>() {
            override fun areItemsTheSame(oldItem: BusinessProductUi, newItem: BusinessProductUi): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BusinessProductUi, newItem: BusinessProductUi): Boolean =
                oldItem == newItem
        }
    }
}
