package com.br.leo.moodsnap.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.br.leo.moodsnap.ui.utils.Converters
import java.util.Date

@Entity(tableName = "Mood")
@TypeConverters(Converters::class) // Adiciona o conversor para Date
data class MoodModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,

    @ColumnInfo(name = "date")
    var date: Date = Date(),

    @ColumnInfo(name = "mood_type")
    var moodType: Int = 0
)
