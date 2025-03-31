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
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel
import java.util.*
import android.content.Context

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load saved language preference
        loadSavedLanguage()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setListeners()
        observeViewModel()
    }

    private fun loadSavedLanguage() {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentLanguage = sharedPreferences.getString("current_language", "en")
        
        // Set the locale to the saved language
        val locale = Locale(currentLanguage)
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
        viewModel.selectedEmotion.observe(this) { emotionResId ->
            binding.fab.setImageResource(emotionResId)
        }
    }
}
