package com.br.leo.moodsnap.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.adapters.OnboardingAdapter
import com.br.leo.moodsnap.ui.models.OnboardingMedia
import com.br.leo.moodsnap.ui.models.OnboardingSlide
import com.br.leo.moodsnap.ui.utils.ClickUtils
import com.br.leo.moodsnap.ui.utils.FontUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TutorialBottomSheet : BottomSheetDialogFragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button
    private lateinit var btnSkip: Button
    private lateinit var titleText: TextView
    private lateinit var descriptionText: TextView

    private val slides: List<OnboardingSlide> by lazy {
        val context = requireContext()
        val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        // Verificar se o tema está armazenado como Integer ou String
        val currentThemeValue = try {
            // Tentar obter como String primeiro
            sharedPreferences.getString("current_theme", "system")
        } catch (e: ClassCastException) {
            // Se falhar, tentar obter como Integer e converter para String
            when (sharedPreferences.getInt("current_theme", 0)) {
                0 -> "system"
                1 -> "light"
                2 -> "dark"
                else -> "system"
            }
        }

        val currentTheme = currentThemeValue ?: "system"

        val imageRes1 = R.drawable.ic_mascote

        val imageRes2 = when (currentTheme) {
            "light" -> R.drawable.tutorial_light_2
            "dark" -> R.drawable.tutorial_dark_2
            else -> if (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                R.drawable.tutorial_dark_2
            } else {
                R.drawable.tutorial_light_2
            }
        }

        val imageRes3 = when (currentTheme) {
            "light" -> R.drawable.tutorial_light_3
            "dark" -> R.drawable.tutorial_dark_3
            else -> if (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                R.drawable.tutorial_dark_3
            } else {
                R.drawable.tutorial_light_3
            }
        }

        listOf(
            OnboardingSlide(
                media = OnboardingMedia.Image(imageRes1),
                title = context.getString(R.string.onboarding_title_1),
                description = context.getString(R.string.onboarding_description_1)
            ),
            OnboardingSlide(
                media = OnboardingMedia.Image(imageRes2),
                title = context.getString(R.string.onboarding_title_2),
                description = context.getString(R.string.onboarding_description_2)
            ),
            OnboardingSlide(
                media = OnboardingMedia.Image(imageRes3),
                title = context.getString(R.string.onboarding_title_3),
                description = context.getString(R.string.onboarding_description_3)
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_tutorial, container, false)
    }

    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // Configurar para não cancelar ao clicar fora
        dialog.setCanceledOnTouchOutside(false)

        // Configurar para expandir completamente ao abrir
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true

                // Impedir que o usuário arraste para baixo e feche o BottomSheet
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        // Não é necessário implementar
                    }
                })
            }
        }

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar para não ser cancelável ao pressionar o botão de voltar
        isCancelable = false

        // Aplicar a fonte atual ao BottomSheet
        FontUtils.applyFontToView(requireContext(), view)

        setupViews(view)
        setupViewPager()
        setupButtons()
    }

    private fun setupViews(view: View) {
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        btnNext = view.findViewById(R.id.btn_next)
        btnPrevious = view.findViewById(R.id.btn_previous)
        btnSkip = view.findViewById(R.id.btn_skip)
        titleText = view.findViewById(R.id.text_tutorial_title)
        descriptionText = view.findViewById(R.id.text_tutorial_description)
    }

    private fun setupViewPager() {
        val adapter = OnboardingAdapter(slides)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtonsVisibility(position)
                updateDescription(position)
            }
        })
        
        // Inicializar com a descrição do primeiro slide
        updateDescription(0)
    }
    
    private fun updateDescription(position: Int) {
        if (position < slides.size) {
            descriptionText.text = slides[position].description
        }
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
            getString(R.string.btn_finish)
        else
            getString(R.string.btn_next)
    }
}