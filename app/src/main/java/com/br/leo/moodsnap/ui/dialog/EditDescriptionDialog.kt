package com.br.leo.moodsnap.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.databinding.ActivityEditDescriptionBinding
import com.br.leo.moodsnap.service.repository.MoodRepository

class EditDescriptionDialog(private val moodId: Int) : DialogFragment() {

    private lateinit var binding: ActivityEditDescriptionBinding
    private lateinit var repository: MoodRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ActivityEditDescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = MoodRepository(requireContext())

        // Carregar a descrição existente
        if (moodId > 0) {
            val mood = repository.get(moodId)
            binding.editDescription.setText(mood.description)
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            // Fechar o diálogo sem fazer alterações
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            val description = binding.editDescription.text.toString()

            // Atualizar a descrição no banco de dados
            if (moodId > 0) {
                val mood = repository.get(moodId)
                mood.description = description
                repository.update(mood)
                
                // Mostrar Toast de sucesso
                Toast.makeText(requireContext(), "Descrição editada com sucesso!", Toast.LENGTH_SHORT).show()
            }

            // Fechar o diálogo após salvar
            dismiss()
        }
    }
} 