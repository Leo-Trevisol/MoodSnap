package com.br.leo.moodsnap.service.repository.dao

import androidx.room.*
import com.br.leo.moodsnap.service.model.MoodModel
import java.util.Date

@Dao
interface MoodDAO {
    @Insert
    fun save(guest: MoodModel): Long

    @Update
    fun update(guest: MoodModel): Int


    @Delete
    fun delete(guest: MoodModel)

    @Query("SELECT * FROM Mood WHERE id = :moodId")
    fun load(moodId: Int): MoodModel

    @Query("SELECT * FROM Mood")
    fun getAll(): List<MoodModel>

    @Query("SELECT * FROM Mood WHERE mood_type = :moodType")
    fun getMoodType(moodType : Int): List<MoodModel>

    @Query("SELECT * FROM mood WHERE date BETWEEN :startDate AND :endDate LIMIT 1")
    fun getMoodByDateRange(startDate: Date, endDate: Date): MoodModel?
}