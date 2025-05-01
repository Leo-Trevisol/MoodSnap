package com.br.leo.moodsnap.ui.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
    fun getMainColor() : Int {
        return R.color.primary_green
    }

    fun showCustomToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)

        val toastText = layout.findViewById<TextView>(R.id.toast_text)
        toastText.text = message
        toastText.setTextColor(context.resources.getColor(R.color.secundary))

        FontUtils.applyFontToView(context, layout)

        // Carrega e aplica a animação
        val animation = AnimationUtils.loadAnimation(context, R.anim.toast_animation)
        layout.startAnimation(animation)

        // Exibe o Toast customizado
        Toast(context).apply {
            setDuration(duration)
            view = layout
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

    fun updateBackGroundColor(context: Context, button: Button, backgroundColor: Int = getMainColor(), textColor: Int = Color.BLACK) {
        button.setTextColor(textColor)
        button.backgroundTintList = ColorStateList.valueOf(context.resources.getColor(backgroundColor))
    }

    fun setupDialogConfirmButton(context: Context, confirmButton: Button) {
        updateBackGroundColor(context, confirmButton)
    }

    fun getMoodName(context: Context, moodType: Int): String {
        return when (moodType) {
            0 -> context.getString(R.string.mood_very_happy)
            1 -> context.getString(R.string.mood_happy)
            2 -> context.getString(R.string.mood_neutral)
            3 -> context.getString(R.string.mood_sad)
            4 -> context.getString(R.string.mood_very_sad)
            else -> "Desconhecido"
        }
    }

    fun getMoodDrawable(moodType: Int): Int {
        return when (moodType) {
            0 -> R.drawable.very_happy_icon
            1 -> R.drawable.happy_icon
            2 -> R.drawable.neutral_icon
            3 -> R.drawable.sad_icon
            4 -> R.drawable.very_sad_icon
            else -> R.drawable.neutral_icon
        }
    }

    fun <T : View> View.findViewsByType(type: Class<T>): List<T> {
        val result = mutableListOf<T>()
        if (type.isInstance(this)) {
            result.add(type.cast(this))
        }
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                result.addAll(getChildAt(i).findViewsByType(type))
            }
        }
        return result
    }

    fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    fun getMoodIcon(moodType: Int): Int {
        return when (moodType) {
            0 -> R.drawable.very_happy_icon
            1 -> R.drawable.happy_icon
            2 -> R.drawable.neutral_icon
            3 -> R.drawable.sad_icon
            4 -> R.drawable.very_sad_icon
            else -> R.drawable.neutral_icon
        }
    }
}