package com.br.leo.moodsnap.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.models.OnboardingSlide
import com.br.leo.moodsnap.ui.utils.FontManager

class OnboardingAdapter(private val slides: List<OnboardingSlide>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageSlide = view.findViewById<ImageView>(R.id.imageSlide)
        private val titleSlide = view.findViewById<TextView>(R.id.titleSlide)
        private val descriptionSlide = view.findViewById<TextView>(R.id.descriptionSlide)

        fun bind(slide: OnboardingSlide) {
            imageSlide.setImageResource(slide.image)
            titleSlide.text = slide.title
            descriptionSlide.text = slide.description
            
            FontManager.applyFontToView(itemView.context, titleSlide, slide.font)
            FontManager.applyFontToView(itemView.context, descriptionSlide, slide.font)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        return OnboardingViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_onboarding_slide,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount(): Int = slides.size
} 