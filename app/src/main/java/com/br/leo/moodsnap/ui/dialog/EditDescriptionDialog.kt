package com.br.leo.moodsnap.ui.dialog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.databinding.ActivityEditDescriptionBinding
import com.br.leo.moodsnap.service.repository.MoodRepository
import com.br.leo.moodsnap.ui.utils.Utils.showCustomToast
import java.io.File
import java.io.FileOutputStream
import java.util.*

class EditDescriptionDialog(private val moodId: Int) : DialogFragment() {

    private lateinit var binding: ActivityEditDescriptionBinding
    private lateinit var repository: MoodRepository
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityEditDescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = MoodRepository(requireContext())

        // Carregar dados existentes
        if (moodId > 0) {
            val mood = repository.get(moodId)
            binding.editDescription.setText(mood.description)
            
            // Carregar imagem se existir
            mood.imagePath?.let { path ->
                val imageFile = File(path)
                if (imageFile.exists()) {
                    Glide.with(this)
                        .load(imageFile)
                        .into(binding.imageDay)
                }
            }
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.imageDay.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        binding.btnSave.setOnClickListener {
            val description = binding.editDescription.text.toString()
            
            // Atualizar a descrição e imagem no banco de dados
            val mood = repository.get(moodId)
            mood.description = description
            
            // Salvar a imagem selecionada
            selectedImageUri?.let { uri ->
                val imagePath = saveImageToInternalStorage(uri)
                mood.imagePath = imagePath
            }
            
            repository.update(mood)
            showCustomToast(requireContext(), getString(R.string.description_saved))
            dismiss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .into(binding.imageDay)
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val fileName = "mood_image_${System.currentTimeMillis()}.jpg"
        val file = File(requireContext().filesDir, fileName)
        
        FileOutputStream(file).use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
        
        return file.absolutePath
    }
} 