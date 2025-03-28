package com.br.leo.moodsnap.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


import com.br.leo.moodsnap.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class LineChartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_chart);

        LineChart lineChart = findViewById(R.id.lineChart);

        // Create a list of entries
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 1));
        entries.add(new Entry(1, 2));
        entries.add(new Entry(2, 0));
        entries.add(new Entry(3, 4));
        entries.add(new Entry(4, 3));

        // Create a dataset and give it a type
        LineDataSet lineDataSet = new LineDataSet(entries, "Sample Data");
        lineDataSet.setColor(R.color.primary_green);
        lineDataSet.setValueTextColor(R.color.day_text_color);

        // Create a data object with the dataset
        LineData lineData = new LineData(lineDataSet);

        // Set data to the chart
        lineChart.setData(lineData);

        // Customize chart description
        Description description = new Description();
        description.setText("Line Chart Example");
        lineChart.setDescription(description);

        // Refresh the chart
        lineChart.invalidate();
    }
} 