package com.br.leo.moodsnap.service.repository.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.br.leo.moodsnap.service.repository.dao.MoodDAO
import com.br.leo.moodsnap.service.model.MoodModel
import com.br.leo.moodsnap.ui.utils.Converters

@Database(entities = [MoodModel::class], version = 2)
@TypeConverters(Converters::class) // Adiciona o conversor
abstract class MoodDatabase : RoomDatabase() {

    abstract fun moodDao(): MoodDAO

    companion object {
        private lateinit var INSTANCE: MoodDatabase

        fun getDatabase(context: Context): MoodDatabase {
            if (!Companion::INSTANCE.isInitialized) {
                synchronized(MoodDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context, MoodDatabase::class.java, "moodDB")
                        .addMigrations(MIGRATION_1_2) // Usar a migração ao invés de fallbackToDestructiveMigration
                        .allowMainThreadQueries()
                        .build()
                }
            }
            return INSTANCE
        }

        /**
         * Migração da versão 1 para 2 do banco de dados
         * Adiciona as colunas description e image_path
         */
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adicionar as novas colunas
                database.execSQL("ALTER TABLE Mood ADD COLUMN description TEXT")
                database.execSQL("ALTER TABLE Mood ADD COLUMN image_path TEXT")
            }
        }
    }
}