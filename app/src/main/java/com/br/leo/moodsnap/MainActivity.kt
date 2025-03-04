package com.br.leo.moodsnap

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.br.leo.moodsnap.databinding.ActivityMainBinding
import com.br.leo.moodsnap.ui.dialog.DialogEmotions
import com.br.leo.moodsnap.ui.interfaces.OnEmotionSelectedListener
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), View.OnClickListener, OnEmotionSelectedListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setListeners()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab) {
            val dialogEmotions = DialogEmotions(this, this)
            dialogEmotions.show()
        }
    }

    override fun onEmotionSelected(emotionResId: Int) {
        binding.fab.setImageResource(emotionResId)
    }

    private fun setListeners() {
        binding.fab.setOnClickListener(this)
    }
}
