package com.br.leo.moodsnap.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DialogEmotions(private val viewModel: MainViewModel) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.emotions_layout, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                
                // Definir a altura máxima para garantir que o diálogo fique acima do FAB
                val displayMetrics = requireContext().resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val marginFromBottom = (80 * displayMetrics.density).toInt() // 80dp em pixels
                it.layoutParams.height = screenHeight - marginFromBottom
            }
        }
        
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initComponents(view)
    }

    private fun initComponents(view: View) {
        view.findViewById<ImageView>(R.id.emotion_very_happy)?.setOnClickListener {
            viewModel.setSelectedEmotion(R.drawable.muito_feliz)
            dismiss()
        }

        view.findViewById<ImageView>(R.id.emotion_happy)?.setOnClickListener {
            viewModel.setSelectedEmotion(R.drawable.feliz)
            dismiss()
        }

        view.findViewById<ImageView>(R.id.emotion_neutral)?.setOnClickListener {
            viewModel.setSelectedEmotion(R.drawable.neutro)
            dismiss()
        }

        view.findViewById<ImageView>(R.id.emotion_sad)?.setOnClickListener {
            viewModel.setSelectedEmotion(R.drawable.triste)
            dismiss()
        }

        view.findViewById<ImageView>(R.id.emotion_very_sad)?.setOnClickListener {
            viewModel.setSelectedEmotion(R.drawable.muito_triste)
            dismiss()
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme
}
