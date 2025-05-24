package com.br.leo.moodsnap.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.R.drawable.tutorial_dark_2
import com.br.leo.moodsnap.ui.adapters.OnboardingAdapter
import com.br.leo.moodsnap.ui.models.OnboardingSlide
import com.br.leo.moodsnap.ui.models.OnboardingMedia
import com.br.leo.moodsnap.ui.utils.FontUtils
import com.br.leo.moodsnap.ui.utils.Utils
import com.br.leo.moodsnap.ui.utils.ClickUtils
import com.br.leo.moodsnap.ui.utils.Utils.getCurrentTheme

class OnboardingDialog(context: Context) : Dialog(context) {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button
    private lateinit var btnSkip: Button

    private val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    private val currentFont = sharedPreferences.getString("current_font", "default")

    val currentTheme = getCurrentTheme(context)
    val imageRes1 = when (currentTheme) {
        AppCompatDelegate.MODE_NIGHT_YES -> R.drawable.tutorial_dark_1
        AppCompatDelegate.MODE_NIGHT_NO -> R.drawable.tutorial_light_1
        else -> R.drawable.tutorial_light_1
    }

    val imageRes2 = when (currentTheme) {
        AppCompatDelegate.MODE_NIGHT_YES -> tutorial_dark_2
        AppCompatDelegate.MODE_NIGHT_NO -> R.drawable.tutorial_light_2
        else -> R.drawable.tutorial_light_1
    }


    private val slides = listOf(
        OnboardingSlide(
            media = OnboardingMedia.Image(R.drawable.ic_mascote),
            title = context.getString(R.string.onboarding_title_1),
            description = context.getString(R.string.onboarding_description_1),
            font = currentFont ?: "default"
        ),
        OnboardingSlide(
            media = OnboardingMedia.Image(imageRes1),
            title = context.getString(R.string.onboarding_title_2),
            description = context.getString(R.string.onboarding_description_2),
            font = currentFont ?: "default"
        ),
        OnboardingSlide(
            media = OnboardingMedia.Image(imageRes2),
            title = context.getString(R.string.onboarding_title_3),
            description = context.getString(R.string.onboarding_description_3),
            font = currentFont ?: "default"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(R.layout.dialog_onboarding)
        window?.attributes?.windowAnimations = R.style.DialogAnimation

        // 🔤 Aplica a fonte ao diálogo
        currentFont?.let {
            FontUtils.applyFontToView(context, findViewById(android.R.id.content), it)
        }

        setupViews()
        setupViewPager()
        setupButtons()
    }

    private fun setupViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnNext = findViewById(R.id.btn_next)
        btnPrevious = findViewById(R.id.btn_previous)
        btnSkip = findViewById(R.id.btn_skip)
    }

    private fun setupViewPager() {
        val adapter = OnboardingAdapter(slides)
        viewPager.adapter = adapter
        
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtonsVisibility(position)
            }
        })
    }

    private fun setupButtons() {
        // Usar ClickUtils para evitar múltiplos cliques rápidos
        btnNext.setOnClickListener({
            if (viewPager.currentItem == slides.size - 1) {
                dismiss()
            } else {
                viewPager.currentItem = viewPager.currentItem + 1
            }
        })

        btnPrevious.setOnClickListener({
            viewPager.currentItem = viewPager.currentItem - 1
        })

        btnSkip.setOnClickListener({
           dismiss()
        })

        updateButtonsVisibility(0)
    }

    private fun updateButtonsVisibility(position: Int) {
        btnPrevious.visibility = if (position == 0) View.GONE else View.VISIBLE
        btnNext.text = if (position == slides.size - 1) 
            context.getString(R.string.btn_finish) 
        else 
            context.getString(R.string.btn_next)
    }
} 