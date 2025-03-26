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
    }

}