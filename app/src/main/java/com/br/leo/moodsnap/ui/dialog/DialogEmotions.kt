package com.br.leo.moodsnap.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialog
import androidx.lifecycle.ViewModelProvider
import com.br.leo.moodsnap.R
import android.widget.ImageView
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel

class DialogEmotions(context: Context, private val viewModel: MainViewModel) :
    AppCompatDialog(context, R.style.RoundedDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.emotions_layout)

        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setGravity(Gravity.BOTTOM) // Define na parte inferior

        val params = window?.attributes
        params?.y = 150 // Ajusta a posição para cima (quanto maior, mais para cima)
        window?.attributes = params
        initComponents()
    }

    private fun initComponents() {
        findViewById<ImageView>(R.id.emotion_very_happy)?.setOnClickListener {
            viewModel.setSelectedEmotion(R.drawable.muito_feliz)
            dismiss()
        }

        findViewById<ImageView>(R.id.emotion_happy)?.setOnClickListener {
            viewModel.setSelectedEmotion(R.drawable.feliz)
            dismiss()
        }

        findViewById<ImageView>(R.id.emotion_neutral)?.setOnClickListener {
            viewModel.setSelectedEmotion(R.drawable.neutro)
            dismiss()
        }

        findViewById<ImageView>(R.id.emotion_sad)?.setOnClickListener {
            viewModel.setSelectedEmotion(R.drawable.triste)
            dismiss()
        }

        findViewById<ImageView>(R.id.emotion_very_sad)?.setOnClickListener {
            viewModel.setSelectedEmotion(R.drawable.muito_triste)
            dismiss()
        }
    }
}
