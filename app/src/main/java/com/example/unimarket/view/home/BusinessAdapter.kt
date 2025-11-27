package com.example.unimarket.view.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.unimarket.R
import com.example.unimarket.model.domain.entity.Business
import java.util.Locale
import coil.request.CachePolicy

class BusinessAdapter(
    private var items: List<Business>,
    private val onClick: (Business) -> Unit
) : RecyclerView.Adapter<BusinessAdapter.VH>() {

    fun submit(newItems: List<Business>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.business_card, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(
        itemView: View,
        private val onClick: (Business) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val imgCover: ImageView = itemView.findViewById(R.id.imgCover)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        private val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        private val tvReviewsCount: TextView = itemView.findViewById(R.id.tvReviewsCount)
        private val tvEta: TextView = itemView.findViewById(R.id.tvEta)

        fun bind(b: Business) {
            tvName.text = b.name.ifBlank { "Negocio universitario" }

            tvSubtitle.text = b.categories
                .mapNotNull { it.name?.trim()?.takeIf { n -> n.isNotEmpty() } }
                .joinToString(" - ")

            tvRating.text = String.format(Locale.US, "%.1f", b.rating)

            tvReviewsCount.text = "(${b.amountRatings})"

            tvEta.text = "15–25 min"

            val url = b.logo?.trim().orEmpty()
            if (url.isNotEmpty()) {
                imgCover.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.personajesingup)
                    error(R.drawable.personajesingup)
                    memoryCachePolicy(CachePolicy.ENABLED)
                    diskCachePolicy(CachePolicy.ENABLED)
                    networkCachePolicy(CachePolicy.ENABLED)
                }
            } else {
                imgCover.setImageResource(R.drawable.personajesingup)
            }

            itemView.setOnClickListener { onClick(b) }
        }
    }
}