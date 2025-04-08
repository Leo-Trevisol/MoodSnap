package com.br.leo.moodsnap

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.br.leo.moodsnap.databinding.ActivityMainBinding
import com.br.leo.moodsnap.ui.dialog.DialogEmotions
import com.br.leo.moodsnap.ui.dialog.OnboardingDialog
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel
import com.br.leo.moodsnap.utils.PreferencesManager
import java.util.*
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import android.content.res.Configuration
import com.br.leo.moodsnap.ui.utils.FontManager

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
                apply()
            }
        }
        
        val currentLanguage = sharedPreferences.getString("current_language", Locale.getDefault().language)
        updateLocale(currentLanguage ?: "en")

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

        // Configurar fonte
        val currentFont = sharedPreferences.getString("current_font", "default")
        FontManager.applyFontToActivity(this, currentFont ?: "default")
    }

    private fun updateLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.navView.setupWithNavController(navController)

        // Configurar o comportamento dos itens do menu
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val navOptions = NavOptions.Builder()
                        .setEnterAnim(R.anim.dialog_enter)
                        .setExitAnim(R.anim.dialog_exit)
                        .setPopEnterAnim(R.anim.dialog_enter)
                        .setPopExitAnim(R.anim.dialog_exit)
                        .build()
                    navController.navigate(R.id.navigation_home, null, navOptions)
                    true
                }
                R.id.navigation_dashboard -> {
                    val navOptions = NavOptions.Builder()
                        .setEnterAnim(R.anim.dialog_enter)
                        .setExitAnim(R.anim.dialog_exit)
                        .setPopEnterAnim(R.anim.dialog_enter)
                        .setPopExitAnim(R.anim.dialog_exit)
                        .build()
                    navController.navigate(R.id.navigation_dashboard, null, navOptions)
                    true
                }
                else -> false
            }
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab) {
            val dialogEmotions = DialogEmotions(viewModel, 0L, Calendar.getInstance())
            dialogEmotions.show(supportFragmentManager, dialogEmotions.tag)
        }
    }

    private fun setListeners() {
        binding.fab.setOnClickListener(this)
    }

    private fun observeViewModel() {
        // Implementar observadores do ViewModel se necessário
    }

    private fun showOnboardingTutorial() {
        val onboardingDialog = OnboardingDialog(this)
        onboardingDialog.setCancelable(false)
        onboardingDialog.show()
    }
}
