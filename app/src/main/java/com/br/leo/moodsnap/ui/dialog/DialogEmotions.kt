package com.br.leo.moodsnap.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class DialogEmotions(
    private val viewModel: MainViewModel,
    private val existingMoodId: Long = 0,
    private val selectedDate: Calendar
) : BottomSheetDialogFragment() {

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
        // Configurar a data selecionada
        val selectedDateText = view.findViewById<TextView>(R.id.selected_date)
        val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
        selectedDateText.text = dateFormat.format(selectedDate.time)

        // Configurar botão de editar
        val btnEdit = view.findViewById<MaterialButton>(R.id.btn_edit)
        if (existingMoodId > 0) {
            btnEdit.visibility = View.VISIBLE
            btnEdit.setOnClickListener {
                val editDialog = EditDescriptionDialog(existingMoodId.toInt())
                editDialog.isCancelable = false
                editDialog.show(parentFragmentManager, "EditDescriptionDialog")
                dismissAllowingStateLoss()
            }
        } else {
            btnEdit.visibility = View.GONE
        }

        // Configurar botão de deletar
        val btnDelete = view.findViewById<MaterialButton>(R.id.btn_delete)
        if (existingMoodId > 0) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                CustomAlertDialog.create(requireContext())
                    .setTitle("Atenção")
                    .setMessage("Você deseja realmente deletar o humor do dia ${selectedDateText.text}?")
                    .setPositiveListener {
                        viewModel.deleteMood(existingMoodId)
                        Toast.makeText(requireContext(), "Humor deletado com sucesso!", Toast.LENGTH_SHORT).show()
                        dismissAllowingStateLoss()
                    }
                    .setNegativeListener(null).show()
            }
        }

        view.findViewById<ImageView>(R.id.emotion_very_happy)?.setOnClickListener {
            if (isAdded) {
                viewModel.setSelectedEmotion(R.drawable.muito_feliz)
                Toast.makeText(requireContext(), "Humor cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                dismissAllowingStateLoss()
            }
        }

        view.findViewById<ImageView>(R.id.emotion_happy)?.setOnClickListener {
            if (isAdded) {
                viewModel.setSelectedEmotion(R.drawable.feliz)
                Toast.makeText(requireContext(), "Humor cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                dismissAllowingStateLoss()
            }
        }

        view.findViewById<ImageView>(R.id.emotion_neutral)?.setOnClickListener {
            if (isAdded) {
                viewModel.setSelectedEmotion(R.drawable.neutro)
                Toast.makeText(requireContext(), "Humor cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                dismissAllowingStateLoss()
            }
        }

        view.findViewById<ImageView>(R.id.emotion_sad)?.setOnClickListener {
            if (isAdded) {
                viewModel.setSelectedEmotion(R.drawable.triste)
                Toast.makeText(requireContext(), "Humor cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                dismissAllowingStateLoss()
            }
        }

        view.findViewById<ImageView>(R.id.emotion_very_sad)?.setOnClickListener {
            if (isAdded) {
                viewModel.setSelectedEmotion(R.drawable.muito_triste)
                Toast.makeText(requireContext(), "Humor cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                dismissAllowingStateLoss()
            }
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        } catch (e: IllegalStateException) {
            // Ignorar exceção se o fragmento não puder ser adicionado
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme
}
