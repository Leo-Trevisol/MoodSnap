package com.br.leo.moodsnap.ui.dialog

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDialog
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.interfaces.OnEmotionSelectedListener

class DialogEmotions(context: Context, private val listener: OnEmotionSelectedListener) :
    AppCompatDialog(context, R.style.RoundedDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.emotions_layout)
        initComponents()
    }

    private fun initComponents() {
        findViewById<ImageView>(R.id.emotion_very_happy)?.setOnClickListener {
            listener.onEmotionSelected(R.drawable.muito_feliz)
            dismiss()
        }

        findViewById<ImageView>(R.id.emotion_happy)?.setOnClickListener {
            listener.onEmotionSelected(R.drawable.feliz)
            dismiss()
        }

        findViewById<ImageView>(R.id.emotion_neutral)?.setOnClickListener {
            listener.onEmotionSelected(R.drawable.neutro)
            dismiss()
        }

        findViewById<ImageView>(R.id.emotion_sad)?.setOnClickListener {
            listener.onEmotionSelected(R.drawable.triste)
            dismiss()
        }

        findViewById<ImageView>(R.id.emotion_very_sad)?.setOnClickListener {
            listener.onEmotionSelected(R.drawable.muito_triste)
            dismiss()
        }
    }
}
