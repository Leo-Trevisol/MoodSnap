package com.br.leo.moodsnap.ui.dialog

import android.content.Context
import android.os.Bundle
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
