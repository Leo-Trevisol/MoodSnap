package com.br.leo.moodsnap

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.br.leo.moodsnap.databinding.ActivityMainBinding
import com.br.leo.moodsnap.ui.dialog.DialogEmotions
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setListeners()
        observeViewModel()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.navView.setupWithNavController(navController)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab) {
            val dialogEmotions = DialogEmotions(this, viewModel)
            dialogEmotions.show()
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
