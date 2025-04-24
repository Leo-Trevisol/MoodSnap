package com.br.leo.moodsnap.ui.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.VideoView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.models.OnboardingSlide
import com.br.leo.moodsnap.ui.models.OnboardingMedia
import com.br.leo.moodsnap.ui.utils.FontUtils

class OnboardingAdapter(private val slides: List<OnboardingSlide>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val videoSlide = view.findViewById<VideoView>(R.id.videoSlide)
        private val imageSlide = view.findViewById<ImageView>(R.id.imageSlide)
        private val titleSlide = view.findViewById<TextView>(R.id.titleSlide)
        private val descriptionSlide = view.findViewById<TextView>(R.id.descriptionSlide)

        fun bind(slide: OnboardingSlide) {
            // Configurar o título e descrição
            titleSlide.text = slide.title
            descriptionSlide.text = slide.description
            
            // Aplicar fontes
            FontUtils.applyFontToView(itemView.context, titleSlide, slide.font)
            FontUtils.applyFontToView(itemView.context, descriptionSlide, slide.font)

            // Configurar mídia (imagem ou vídeo)
            when (slide.media) {
                is OnboardingMedia.Image -> {
                    imageSlide.visibility = View.VISIBLE
                    videoSlide.visibility = View.GONE
                    imageSlide.setImageResource(slide.media.resourceId)
                }
                is OnboardingMedia.Video -> {
                    imageSlide.visibility = View.GONE
                    videoSlide.visibility = View.VISIBLE
                    
                    // Configurar o vídeo
                    val uri = Uri.parse(slide.media.url)
                    videoSlide.setVideoURI(uri)
                    videoSlide.setOnPreparedListener { mediaPlayer ->
                        // Configurar o vídeo para loop
                        mediaPlayer.isLooping = true
                        // Ajustar o volume (opcional)
                        mediaPlayer.setVolume(0f, 0f) // Mudo
                        // Iniciar a reprodução
                        videoSlide.start()
                    }
                }
            }
        }

        fun stopVideo() {
            if (videoSlide.isPlaying) {
                videoSlide.stopPlayback()
            }
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

    override fun onViewRecycled(holder: OnboardingViewHolder) {
        super.onViewRecycled(holder)
        holder.stopVideo()
    }
} 