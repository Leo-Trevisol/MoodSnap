package com.br.leo.moodsnap.ui.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.br.leo.moodsnap.R

object ButtonUtils {
    /**
     * Highlights a button by changing its background color to primary_green and text color to white.
     * Optionally updates the drawable tint list.
     *
     * @param context The context to get resources
     * @param button The button to highlight
     * @param updateDrawableTint Whether to update the drawable tint (default true)
     */
    fun highlightButton(context: Context, button: Button, updateDrawableTint: Boolean = true) {
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_green)))
        button.setTextColor(Color.WHITE)
        if (updateDrawableTint) {
         //   TextViewCompat.setCompoundDrawableTintList(button, ColorStateList.valueOf(Color.WHITE))
        }
    }

    /**
     * Resets a button's appearance to its default state.
     *
     * @param context The context to get resources
     * @param button The button to reset
     * @param backgroundColor The background color resource ID (default R.color.gray_dark)
     * @param drawableTintColor The drawable tint color resource ID (default R.color.secundary)
     */
    fun resetButton(
        context: Context, 
        button: Button, 
        backgroundColor: Int = R.color.gray_dark,
    ) {
        Utils.updateBackGroundColor(context, button, backgroundColor)
        button.setTextColor(ContextCompat.getColor(context, R.color.dark_secondary))
    }

    /**
     * Resets all buttons in a list to their default state.
     *
     * @param context The context to get resources
     * @param buttons List of buttons to reset
     * @param buttons List of buttons to reset
     * @param backgroundColor The background color resource ID (default R.color.gray_dark)
     * @param drawableTintColor The drawable tint color resource ID (default R.color.secundary)
     */
    fun resetAllButtons(
        context: Context,
        buttons: List<Button>,
        backgroundColor: Int = R.color.gray_dark,
    ) {
        buttons.forEach { button ->
            resetButton(context, button, backgroundColor)
        }
    }

    /**
     * Sets up click listeners for a group of buttons where only one can be highlighted at a time.
     * When a button is clicked, all other buttons are reset and the clicked button is highlighted.
     *
     * @param context The context to get resources
     * @param buttons Map of buttons to their click handlers (optional)
     * @param onButtonClicked Optional callback for when a button is clicked, receives the clicked button
     */
    fun setupToggleButtonGroup(
        context: Context,
        buttons: List<Button>,
        onButtonClicked: ((Button) -> Unit)? = null
    ) {
        buttons.forEach { button ->
            button.setOnClickListener {
                resetAllButtons(context, buttons)
                highlightButton(context, button)
                onButtonClicked?.invoke(button)
            }
        }
    }
} 