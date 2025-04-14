package com.br.leo.moodsnap.ui.edit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.databinding.ActivityEditDescriptionBinding
import com.br.leo.moodsnap.service.repository.MoodRepository
import android.graphics.Bitmap
import android.view.View

class EditDescriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditDescriptionBinding
    private lateinit var repository: MoodRepository
    private var moodId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setTheme(R.style.DialogRounded)
        binding = ActivityEditDescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialmente, mostra o placeholder
        binding.placeholderContainer.visibility = View.VISIBLE
        binding.imageDay.visibility = View.GONE
    }

    // Método para ser chamado quando uma imagem for selecionada
    private fun updateImageView(bitmap: Bitmap) {
        binding.imageDay.setImageBitmap(bitmap)
        binding.imageDay.visibility = View.VISIBLE
        binding.placeholderContainer.visibility = View.GONE
    }

    // Método para limpar a imagem
    private fun clearImage() {
        binding.imageDay.setImageBitmap(null)
        binding.imageDay.visibility = View.GONE
        binding.placeholderContainer.visibility = View.VISIBLE
    }
}