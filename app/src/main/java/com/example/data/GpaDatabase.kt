package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Degree::class,
        GradingScale::class,
        GradeEntry::class,
        Semester::class,
        Course::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GpaDatabase : RoomDatabase() {
    abstract fun gpaDao(): GpaDao

    companion object {
        @Volatile
        private var INSTANCE: GpaDatabase? = null

        fun getDatabase(context: Context): GpaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GpaDatabase::class.java,
                    "target_gpa_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
