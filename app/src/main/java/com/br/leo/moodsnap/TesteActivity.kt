package com.br.leo.moodsnap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import com.br.leo.moodsnap.databinding.ActivityTesteBinding
import com.br.leo.moodsnap.ui.dashboard.DashboardFragment.DayFilterType

class TesteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTesteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTesteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val feelings = listOf(DayFilterType.LAST_7_DAYS, DayFilterType.LAST_MONTH, DayFilterType.LAST_3_MONTHS, DayFilterType.LAST_6_MONTHS, DayFilterType.LAST_9_MONTHS, DayFilterType.LAST_YEAR)
        val arrayAdapter = ArrayAdapter(this, R.layout.dropdown_item, feelings)
        binding.dropdownInput.autoCompleteTextView.setAdapter(arrayAdapter)
    }
}