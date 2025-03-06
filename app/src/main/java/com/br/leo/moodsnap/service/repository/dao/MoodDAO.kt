package com.br.leo.moodsnap.service.repository.dao

import androidx.room.*
import com.br.leo.moodsnap.model.MoodModel

@Dao
interface MoodDAO {
    @Insert
    fun save(guest: MoodModel): Long

    @Update
    fun update(guest: MoodModel): Int

    @Delete
    fun delete(guest: MoodModel)

    @Query("SELECT * FROM Mood WHERE id = :id")
    fun load(id: Int): MoodModel

    @Query("SELECT * FROM Mood")
    fun getAll(): List<MoodModel>

    @Query("SELECT * FROM Mood WHERE mood_type = :moodType")
    fun getMoodType(moodType : Int): List<MoodModel>
}