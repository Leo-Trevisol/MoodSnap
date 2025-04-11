package com.br.leo.moodsnap.ui.utils

import android.content.Context
import android.util.DisplayMetrics
import kotlin.math.min

object ScreenUtils {
    fun getSmallestWidth(context: Context): Float {
        val displayMetrics = context.resources.displayMetrics
        val widthDp = displayMetrics.widthPixels / displayMetrics.density
        val heightDp = displayMetrics.heightPixels / displayMetrics.density
        return min(widthDp, heightDp)
    }

    fun getScreenInfo(context: Context): String {
        val metrics = context.resources.displayMetrics
        val widthDp = metrics.widthPixels / metrics.density
        val heightDp = metrics.heightPixels / metrics.density
        val smallestWidth = getSmallestWidth(context)
        
        return """
            Screen Info:
            Width (dp): $widthDp
            Height (dp): $heightDp
            Smallest Width (dp): $smallestWidth
            Density: ${metrics.density}
            Density DPI: ${metrics.densityDpi}
        """.trimIndent()
    }
} 