package com.br.leo.moodsnap.ui.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.br.leo.moodsnap.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Utils {

    fun getMainColor() : Int{
        return R.color.primary_green
    }


    fun showCustomToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)

        layout.findViewById<TextView>(R.id.toast_text).text = message

        Toast(context).apply {
            setDuration(duration)
            setView(layout)
            show()
        }
    }

    fun CardView.flashError(onComplete: () -> Unit) {
        val originalColor = cardBackgroundColor
        val context = this.context
        
        CoroutineScope(Dispatchers.Main).launch {
            // Piscar vermelho
            setCardBackgroundColor(context.getColor(R.color.primary_red))
            delay(200) // Aguarda 100ms
            
            // Volta para a cor original
            setCardBackgroundColor(originalColor)
            delay(50) // Pequeno delay antes de chamar o callback
            
            onComplete()
        }
    }

    fun updateBackGroundColor(context: Context, button: Button, color: Int = getMainColor()) {

        button.setTextColor(Color.BLACK)
        button.backgroundTintList = ColorStateList.valueOf(context.getResources().getColor(color))
    }

}