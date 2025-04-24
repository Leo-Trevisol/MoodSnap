package com.br.leo.moodsnap.ui.models

sealed class OnboardingMedia {
    data class Image(val resourceId: Int) : OnboardingMedia()
    data class Video(val url: String) : OnboardingMedia()
}

data class OnboardingSlide(
    val media: OnboardingMedia,
    val title: String,
    val description: String,
    val font: String = "default"
) {
    val imageResId: Int?
        get() = (media as? OnboardingMedia.Image)?.resourceId

    val videoUrl: String?
        get() = (media as? OnboardingMedia.Video)?.url
} 