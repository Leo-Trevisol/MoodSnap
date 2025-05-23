package com.br.leo.moodsnap

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.br.leo.moodsnap.databinding.ActivityMainBinding
import com.br.leo.moodsnap.ui.dialog.DialogEmotions
import com.br.leo.moodsnap.ui.dialog.OnboardingDialog
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel
import com.br.leo.moodsnap.ui.utils.PreferencesManager
import com.br.leo.moodsnap.ui.utils.ClickUtils
import java.util.*
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import android.content.res.Configuration
import com.br.leo.moodsnap.ui.utils.FontUtils
import android.graphics.Color
import android.content.res.ColorStateList
import android.content.res.Resources
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Carregar configurações de idioma e tema
        loadSettings()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obter o NavController
        navController = (supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        
        // Configurar os listeners dos ícones de navegação
        setupCustomNavigation()
        
        setupNavigation()
        setListeners()
        observeViewModel()
        
        // Mostrar tutorial na primeira vez
        val preferencesManager = PreferencesManager(this)
        if (preferencesManager.isFirstTime()) {
            showOnboardingTutorial()
            preferencesManager.setFirstTimeDone()
        }
    }

    private fun loadSettings() {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        
        // Verificar se é a primeira execução do app
        val isFirstRun = sharedPreferences.getBoolean("is_first_run", true)
        if (isFirstRun) {
            // Aqui você pode adicionar qualquer lógica específica para primeira execução
            with(sharedPreferences.edit()) {
                putBoolean("is_first_run", false)
                apply()
            }
        }
        
        // Configurar idioma
        if (!sharedPreferences.contains("current_language")) {
            // Se é a primeira vez, usar o idioma do sistema
            val systemLanguage = Locale.getDefault().language
            with(sharedPreferences.edit()) {
                putString("current_language", systemLanguage)
                putBoolean("is_system_language_apply", true)
                apply()
            }
        }
        
        val isSystemLanguageApply = sharedPreferences.getBoolean("is_system_language_apply", true)
        val currentLanguage = sharedPreferences.getString("current_language", Locale.getDefault().language)
        
        if (isSystemLanguageApply) {
            // Use system language
            val systemLocale = Resources.getSystem().configuration.locales.get(0)
            Locale.setDefault(systemLocale)
            val config = resources.configuration
            config.setLocale(systemLocale)
            resources.updateConfiguration(config, resources.displayMetrics)
        } else {
            // Use selected language
            updateLocale(currentLanguage ?: "en")
        }

        // Configurar tema
        if (!sharedPreferences.contains("current_theme")) {
            // Se é a primeira vez, usar o tema do sistema
            val systemNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val defaultNightMode = when (systemNightMode) {
                Configuration.UI_MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_YES
                Configuration.UI_MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            with(sharedPreferences.edit()) {
                putInt("current_theme", defaultNightMode)
                apply()
            }
        }
        
        val currentTheme = sharedPreferences.getInt("current_theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(currentTheme)

        // Apply font
        FontUtils.applyFontToActivity(this)
    }

    private fun updateLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun setupCustomNavigation() {
        // Configurar o ícone de Home (Calendário)
        val navHome = findViewById<ImageView>(R.id.nav_home)
            ClickUtils.setDebounceClickListener(navHome){
            navController.navigate(R.id.navigation_home)
            updateNavigationIcons(R.id.navigation_home)
        }
        
        // Configurar o ícone de Dashboard (Estatísticas)
        val navDashboard = findViewById<ImageView>(R.id.nav_dashboard)
        ClickUtils.setDebounceClickListener(navDashboard){
            navController.navigate(R.id.navigation_dashboard)
            updateNavigationIcons(R.id.navigation_dashboard)
        }
        
        // Definir o ícone inicial como selecionado
        updateNavigationIcons(R.id.navigation_home)
        
        // Observar mudanças na navegação para atualizar os ícones
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home -> updateNavigationIcons(R.id.navigation_home)
                R.id.navigation_dashboard -> updateNavigationIcons(R.id.navigation_dashboard)
            }
        }
    }
    
    private fun updateNavigationIcons(selectedItemId: Int) {
        val navHome = findViewById<ImageView>(R.id.nav_home)
        val navDashboard = findViewById<ImageView>(R.id.nav_dashboard)
        
        // Resetar todos os ícones para a cor não selecionada
        navHome.setColorFilter(ContextCompat.getColor(this, R.color.gray_dark))
        navDashboard.setColorFilter(ContextCompat.getColor(this, R.color.gray_dark))
        
        // Definir a cor do ícone selecionado
        when (selectedItemId) {
            R.id.navigation_home -> navHome.setColorFilter(ContextCompat.getColor(this, R.color.primary_green))
            R.id.navigation_dashboard -> navDashboard.setColorFilter(ContextCompat.getColor(this, R.color.primary_green))
        }
    }

    private fun setupNavigation() {
        // Esta função agora está simplificada, pois a navegação é tratada em setupCustomNavigation()
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Configurar as animações de navegação
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.dialog_enter)
            .setExitAnim(R.anim.dialog_exit)
            .setPopEnterAnim(R.anim.dialog_enter)
            .setPopExitAnim(R.anim.dialog_exit)
            .build()
            
        // Configurar o controlador de navegação com as opções de animação
        navController.addOnDestinationChangedListener { _, destination, _ ->
            //Log.d("Navigation", "Navigated to: ${destination.label}")
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab) {
            // Verificar se o clique é válido (não é um clique rápido repetido)
            if (ClickUtils.isClickValid(v.id)) {
                val dialogEmotions = DialogEmotions.newInstance(viewModel, 0L, Calendar.getInstance())
                dialogEmotions.show(supportFragmentManager, dialogEmotions.tag)
            }
        }
    }

    private fun setListeners() {
        ClickUtils.setDebounceClickListener(binding.fab) {
            // Mostrar o diálogo de emoções
            val dialogEmotions = DialogEmotions.newInstance(viewModel, 0L, Calendar.getInstance())
            dialogEmotions.show(supportFragmentManager, dialogEmotions.tag)
        }

        // Configurar o FAB com fundo completamente transparente
        binding.fab.apply {
            background = null
            useCompatPadding = false
            compatElevation = 0f
            elevation = 0f
            stateListAnimator = null
        }
    }

    private fun observeViewModel() {
        // Implementar observadores do ViewModel se necessário
    }

    private fun showOnboardingTutorial() {
//        TapTargetView.showFor(this,
//            TapTarget.forView(findViewById(R.id.fab), "Esse é o botão!", "Clique aqui para começar")
//                .transparentTarget(true)
//                .tintTarget(false)
//                .targetRadius(56)
//                .outerCircleColor(R.color.white)
//                .textColor(R.color.secundary)
//                .cancelable(false),
//            object : TapTargetView.Listener() {
//                override fun onTargetClick(view: TapTargetView) {
//                    super.onTargetClick(view)
//                    binding.fab.performClick()
//                }
//            }
//        )

        val onboardingDialog = OnboardingDialog(this)
        onboardingDialog.setCancelable(false)
        onboardingDialog.show()
    }
}
