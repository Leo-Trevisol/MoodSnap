package com.br.leo.moodsnap.ui.dashboard

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.utils.FontUtils

/**
 * Adaptador personalizado para spinners que inclui um rótulo antes do valor.
 */
class LabeledSpinnerAdapter<T>(
    context: Context,
    private val items: List<T>,
    private val label: String,
    private val typeface: Typeface?,
    private val getItemText: (T) -> String
) : ArrayAdapter<T>(context, android.R.layout.simple_spinner_item, items) {

    val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    val currentFont = sharedPreferences.getString("current_font", "default")

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.custom_comparison_spinner_item, parent, false)

        val labelTextView = view.findViewById<TextView>(R.id.spinner_label)
        val valueTextView = view.findViewById<TextView>(R.id.spinner_text)

        labelTextView.text = label
        labelTextView.alpha = 0.5f
        valueTextView.text = getItemText(items[position])
        valueTextView.textSize = context.resources.getDimension(R.dimen.text_recycler_dialog)
        labelTextView.textSize = context.resources.getDimension(R.dimen.text_recycler_dialog)

        if (typeface != null) {
            labelTextView.typeface = typeface
            valueTextView.typeface = typeface
        }

        return view
    }


    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        view.setBackgroundColor(ContextCompat.getColor(context, R.color.primary_background))

        (view as TextView).apply {
            text = getItemText(items[position])
            setTextColor(ContextCompat.getColor(context, R.color.secundary))
            typeface = ResourcesCompat.getFont(context, FontUtils.getFontResourceId(currentFont ?: "default"))
        }
        return view
    }
}
