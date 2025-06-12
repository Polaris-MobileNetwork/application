package com.iust.polaris.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database class for the application.
 *
 * Entities: Lists all the data entities (tables) that are part of this database.
 * Version: The version of the database schema. If you change the schema (e.g., add a new
 * column or table), you MUST increment the version number and provide a migration strategy.
 */
@Database(
    entities = [NetworkMetric::class, Test::class, TestResult::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Abstract method to get an instance of the NetworkMetricDao.
     */
    abstract fun networkMetricDao(): NetworkMetricDao

    /**
     * Abstract method to get an instance of the TestResultDao.
     */
    abstract fun testResultDao(): TestResultDao

    /**
     * Abstract method to get an instance of the TestDao.
     */
    abstract fun testDao(): TestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "polaris_network_database"
                )
                    // Wipes and rebuilds instead of migrating if no Migration object is provided.
                    // This is useful for development but will erase user data on schema change.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
