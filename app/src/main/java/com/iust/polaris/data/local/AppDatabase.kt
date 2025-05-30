package com.iust.polaris.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database class for the application.
 * This class defines the database configuration and serves as the main access point
 * to the persisted data.
 *
 * Entities: Lists all the data entities (tables) that are part of this database.
 * Version: The version of the database schema. If you change the schema (e.g., add a new
 * column or table), you MUST increment the version number and provide a migration strategy.
 * ExportSchema: Set to false to avoid exporting the schema to a JSON file during build,
 * which is useful for version control but not strictly necessary for basic operation.
 */
@Database(entities = [NetworkMetric::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Abstract method to get an instance of the NetworkMetricDao.
     * Room will generate the implementation for this.
     */
    abstract fun networkMetricDao(): NetworkMetricDao

    companion object {
        // Volatile ensures that the value of INSTANCE is always up-to-date and the same
        // to all execution threads. It means that changes made by one thread to INSTANCE
        // are visible to all other threads immediately.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton instance of the AppDatabase.
         * This method uses a synchronized block to prevent multiple threads from creating
         * multiple instances of the database simultaneously.
         *
         * @param context The application context.
         * @return The singleton AppDatabase instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            // If the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "polaris_network_database" // Name of your database file
                )
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    // Migration is not covered in this basic example. For a production app,
                    // you'll want to implement proper migration strategies.
                    .fallbackToDestructiveMigration() // Be careful with this in production!
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
