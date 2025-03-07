package com.br.leo.moodsnap.service.repository.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.br.leo.moodsnap.service.repository.dao.MoodDAO
import com.br.leo.moodsnap.model.MoodModel
import com.br.leo.moodsnap.ui.utils.Converters

@Database(entities = [MoodModel::class], version = 1)
@TypeConverters(Converters::class) // Adiciona o conversor
abstract class MoodDatabase : RoomDatabase() {

    abstract fun moodDao(): MoodDAO

    companion object {
        private lateinit var INSTANCE: MoodDatabase

        fun getDatabase(context: Context): MoodDatabase {
            if (!Companion::INSTANCE.isInitialized) {
                synchronized(MoodDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context, MoodDatabase::class.java, "moodDB")
                        .addMigrations(MIGRATION_1_2)
                        .allowMainThreadQueries()
                        .build()
                }
            }
            return INSTANCE
        }

        /**
         * Atualização de versão de banco de dados
         */
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE FROM Mood")
            }
        }

    }
}