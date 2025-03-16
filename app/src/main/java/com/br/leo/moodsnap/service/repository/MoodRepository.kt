package com.br.leo.moodsnap.service.repository

import android.content.Context
import com.br.leo.moodsnap.service.model.MoodModel
import com.br.leo.moodsnap.service.repository.database.MoodDatabase

class MoodRepository (context: Context) {

    private val dataBase = MoodDatabase.getDatabase(context).moodDao()

    fun get(id: Int): MoodModel {
        return dataBase.load(id)
    }

    fun save(mood: MoodModel): Boolean {
        return dataBase.save(mood) > 0
    }

    fun getAll(): List<MoodModel> {
        return dataBase.getAll()
    }


    fun getMoodType(moodType : Int): List<MoodModel> {
        return dataBase.getMoodType(moodType)
    }

    fun update(mood: MoodModel): Boolean {
        return dataBase.update(mood) > 0
    }

    fun delete(mood: MoodModel) {
        dataBase.delete(mood)
    }

}