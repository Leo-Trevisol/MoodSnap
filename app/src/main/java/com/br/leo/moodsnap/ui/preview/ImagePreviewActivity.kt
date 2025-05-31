package com.br.leo.moodsnap.ui.preview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.databinding.ActivityImagePreviewBinding
import com.br.leo.moodsnap.ui.utils.ClickUtils
import com.br.leo.moodsnap.ui.utils.Utils
import com.bumptech.glide.Glide

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar o botão de voltar
        ClickUtils.setDebounceClickListener(binding.btnBack) {
            finish()
        }

        val btnConfirm = binding.btnConfirm
        Utils.updateBackGroundColor(this, btnConfirm)
        btnConfirm.setOnClickListener {
            finish()
        }

        // Recuperar e exibir a imagem
        intent.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)?.let { uri ->
            Glide.with(this)
                .load(uri)
                .into(binding.previewImage)
        }
    }

    companion object {
        private const val EXTRA_IMAGE_URI = "extra_image_uri"

        fun start(context: Context, imageUri: Uri) {
            val intent = Intent(context, ImagePreviewActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_URI, imageUri)
            }
            context.startActivity(intent)
        }
    }
}
