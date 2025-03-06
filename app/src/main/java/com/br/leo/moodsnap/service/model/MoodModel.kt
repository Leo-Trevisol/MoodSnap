package com.br.leo.moodsnap.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Mood")
class MoodModel {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    @ColumnInfo(name = "date")
    var date: Date = Date()

    @ColumnInfo(name = "mood_type")
    var moodType: Int = 0
}