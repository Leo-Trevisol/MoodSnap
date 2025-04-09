package com.br.leo.moodsnap.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.adapters.OnboardingAdapter
import com.br.leo.moodsnap.models.OnboardingSlide
import com.br.leo.moodsnap.ui.utils.FontUtils

class OnboardingDialog(context: Context) : Dialog(context) {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button
    private lateinit var btnSkip: Button

    private val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    private val currentFont = sharedPreferences.getString("current_font", "default")

    private val slides = listOf(
        OnboardingSlide(
            R.drawable.onboarding_1,
            context.getString(R.string.onboarding_title_1),
            context.getString(R.string.onboarding_description_1),
            currentFont ?: "default"
        ),
        OnboardingSlide(
            R.drawable.onboarding_1,
            context.getString(R.string.onboarding_title_2),
            context.getString(R.string.onboarding_description_2),
            currentFont ?: "default"
        ),
        OnboardingSlide(
            R.drawable.onboarding_1,
            context.getString(R.string.onboarding_title_3),
            context.getString(R.string.onboarding_description_3),
            currentFont ?: "default"
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
        viewPager.adapter = OnboardingAdapter(slides)
        
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtonsVisibility(position)
            }
        })
    }

    private fun setupButtons() {
        btnNext.setOnClickListener {
            if (viewPager.currentItem == slides.size - 1) {
                dismiss()
            } else {
                viewPager.currentItem = viewPager.currentItem + 1
            }
        }

        btnPrevious.setOnClickListener {
            viewPager.currentItem = viewPager.currentItem - 1
        }

        btnSkip.setOnClickListener {
           dismiss()
        }

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