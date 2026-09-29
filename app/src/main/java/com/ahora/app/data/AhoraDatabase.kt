package com.ahora.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 2, exportSchema = true)
abstract class AhoraDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var instance: AhoraDatabase? = null

        fun getInstance(context: Context): AhoraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AhoraDatabase::class.java,
                    "ahora.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
