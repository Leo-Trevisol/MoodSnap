package com.br.leo.moodsnap.ui.edit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.databinding.ActivityEditDescriptionBinding
import com.br.leo.moodsnap.service.repository.MoodRepository

class EditDescriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditDescriptionBinding
    private lateinit var repository: MoodRepository
    private var moodId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setTheme(R.style.DialogRounded)
        binding = ActivityEditDescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        repository = MoodRepository(this)
//
//        // Receber o ID do humor e carregar a descrição existente
//        moodId = intent.getIntExtra("mood_id", 0)
//        if (moodId > 0) {
//            val mood = repository.get(moodId)
//            binding.editDescription.setText(mood.description)
//        }
//
//        setupListeners()
    }

//    private fun setupListeners() {
//        binding.btnSave.setOnClickListener {
//            val description = binding.editDescription.text.toString()
//
//            // Atualizar a descrição no banco de dados
//            val mood = repository.get(moodId)
//            mood.description = description
//            repository.update(mood)
//
//            finish()
//        }
//    }
} 