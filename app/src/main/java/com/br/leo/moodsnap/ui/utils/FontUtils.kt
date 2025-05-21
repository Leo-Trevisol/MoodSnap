package com.br.leo.moodsnap.ui.utils

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.model.FontModel
import com.br.leo.moodsnap.ui.utils.Utils.findViewsByType
import com.google.android.material.button.MaterialButton

object FontUtils {
    /**
     * Gets the current font name from preferences or returns "default" if not set
     */
    private fun getCurrentFont(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("current_font", "default") ?: "default"
    }

    /**
     * Applies the current font to all TextViews in the activity
     */
    fun applyFontToActivity(activity: Activity) {
        val fontName = getCurrentFont(activity)
        applyFontToView(activity, activity.window.decorView, fontName)
    }

    /**
     * Applies the current font to a specific view and its children
     */
    fun applyFontToView(context: Context, view: View, fontName: String = getCurrentFont(context)) {
        if (view is TextView) {
            val typeface = ResourcesCompat.getFont(context, getFontResourceId(fontName))
            view.typeface = typeface
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyFontToView(context, view.getChildAt(i), fontName)
            }
        }
    }

    fun getAllFonts(context: Context): List<FontModel> {
        return listOf(
            FontModel("default", context.getString(R.string.default_font), R.font.poppins_regular, true),
            FontModel("open_sans", context.getString(R.string.open_sans_font), R.font.open_sans_regular),
            FontModel("itim", "Itim", R.font.itim_regular),
            FontModel("source_code_pro", "Source Code Pro", R.font.source_code_pro_regular),
            FontModel("lato", context.getString(R.string.lato_font), R.font.lato_regular),
            FontModel("lobster", "Lobster", R.font.lobster_regular),
            FontModel("noto_sans", "Noto Sans", R.font.noto_sans_regular),
            FontModel("pangolin", "Pangolin", R.font.pangolin_regular)
        )
    }

    /**
     * Gets the resource ID for the specified font name
     */

    fun getFontResourceId(fontName: String): Int {
        return when (fontName) {
            "open_sans" -> R.font.open_sans_regular
            "itim" -> R.font.itim_regular
            "source_code_pro" -> R.font.source_code_pro_regular
            "lato" -> R.font.lato_regular
            "lobster" -> R.font.lobster_regular
            "noto_sans" -> R.font.noto_sans_regular
            "pangolin" -> R.font.pangolin_regular
            else -> R.font.poppins_regular
        }
    }

    fun updateFontDialogPicker(context : Context, dialog: AlertDialog, dialogView : View){
        // Apply font to dialog title
        dialog.findViewById<TextView>(com.google.android.material.R.id.alertTitle)?.let { titleView ->
            applyFontToView(context, titleView)
        }

        // Apply font to dialog buttons
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { button ->
            applyFontToView(context, button)
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { button ->
            applyFontToView(context, button)
        }

        // Apply font to dialog message if exists
        dialog.findViewById<TextView>(android.R.id.message)?.let { messageView ->
            applyFontToView(context, messageView)
        }

        // Apply font to all TextViews in the dialog
        dialogView.findViewsByType(TextView::class.java).forEach { textView ->
            applyFontToView(context, textView)
        }

        // Apply font to all Buttons in the dialog
        dialogView.findViewsByType(Button::class.java).forEach { button ->
            applyFontToView(context, button)
        }

        // Apply font to all MaterialButtons in the dialog
        dialogView.findViewsByType(MaterialButton::class.java).forEach { button ->
            applyFontToView(context, button)
        }
    }
} 