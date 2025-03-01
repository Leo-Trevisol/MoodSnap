package com.br.leo.moodsnap

import android.annotation.SuppressLint
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.br.leo.moodsnap.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private var isLongPress = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        navView.menu.getItem(1).isChecked = false
        navView.menu.getItem(0).isChecked = false

        val fab = binding.fab

        // Defina as imagens
        val normalImage = R.drawable.sorriso_brilho
        val clickImage = R.drawable.fechado_brilho
        val holdImage = R.drawable.icon_animation

        // Imagem normal ao iniciar
        fab.setImageResource(normalImage)

        // Variável para controlar o toque longo
        var isLongPress = false

        fab.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongPress = false

                    // Inicia um temporizador para detectar o toque longo
                    handler.postDelayed({
                        isLongPress = true
                        fab.setImageResource(holdImage) // Troca para a imagem de segurar
                    }, 300) // Define o tempo para considerar toque longo
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isLongPress) {
                        fab.setImageResource(normalImage) // Soltou depois de segurar
                    } else {
                        // Se foi um toque curto, faz a troca com atraso
                        handler.postDelayed({
                            fab.setImageResource(clickImage) // Troca para a imagem de clique

                            // Volta para a imagem normal após 500ms
                            handler.postDelayed({
                                fab.setImageResource(normalImage)
                            }, 200)

                        }, 100) // Atraso antes de trocar a imagem
                    }
                }
            }
            true
        }
    }
}
