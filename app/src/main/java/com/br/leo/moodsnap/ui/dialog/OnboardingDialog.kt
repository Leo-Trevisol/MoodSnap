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

class OnboardingDialog(context: Context) : Dialog(context) {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button

    private val slides = listOf(
        OnboardingSlide(
            R.drawable.onboarding_1,
            context.getString(R.string.onboarding_title_1),
            context.getString(R.string.onboarding_description_1)
        ),
        OnboardingSlide(
            R.drawable.onboarding_1,
            context.getString(R.string.onboarding_title_2),
            context.getString(R.string.onboarding_description_2)
        ),
        OnboardingSlide(
            R.drawable.onboarding_1,
            context.getString(R.string.onboarding_title_3),
            context.getString(R.string.onboarding_description_3)
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_onboarding)

        setupViews()
        setupViewPager()
        setupButtons()
    }

    private fun setupViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnNext = findViewById(R.id.btnNext)
        btnPrevious = findViewById(R.id.btnPrevious)
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