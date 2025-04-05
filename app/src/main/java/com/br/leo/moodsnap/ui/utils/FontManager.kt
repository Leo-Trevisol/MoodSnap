package com.br.leo.moodsnap.ui.utils

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.br.leo.moodsnap.R

object FontManager {
    fun applyFontToActivity(activity: Activity, fontName: String) {
        val rootView = activity.findViewById<View>(android.R.id.content)
        applyFontToViewHierarchy(activity, rootView as ViewGroup, getFontResourceId(fontName))
    }

    fun applyFontToView(context: Context, view: View, fontName: String) {
        when (view) {
            is ViewGroup -> applyFontToViewHierarchy(context, view, getFontResourceId(fontName))
            is TextView -> {
                val typeface = ResourcesCompat.getFont(context, getFontResourceId(fontName))
                view.typeface = typeface
            }
        }
    }

    private fun applyFontToViewHierarchy(context: Context, root: ViewGroup, fontResourceId: Int) {
        val childCount = root.childCount
        for (i in 0 until childCount) {
            val child = root.getChildAt(i)
            when (child) {
                is ViewGroup -> applyFontToViewHierarchy(context, child, fontResourceId)
                is TextView -> {
                    val typeface = ResourcesCompat.getFont(context, fontResourceId)
                    child.typeface = typeface
                }
            }
        }
    }

    fun getFontResourceId(fontName: String): Int {
        return when (fontName) {
            "roboto" -> R.font.roboto_regular
            "open_sans" -> R.font.open_sans_regular
            "lato" -> R.font.lato_regular
            "poppins" -> R.font.poppins_regular
            "mulish" -> R.font.mulish_regular
            "limelight" -> R.font.lime_light_regular
            else -> R.font.poppins_regular // default font
        }
    }

}