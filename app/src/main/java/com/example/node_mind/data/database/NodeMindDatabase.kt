package com.example.node_mind.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.node_mind.data.dao.*
import com.example.node_mind.data.model.*

@Database(
    entities = [Task::class, Node::class, FocusSession::class, DailyStats::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NodeMindDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun nodeDao(): NodeDao  
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun dailyStatsDao(): DailyStatsDao
    
    companion object {
        @Volatile
        private var INSTANCE: NodeMindDatabase? = null
        
        fun getDatabase(context: Context): NodeMindDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NodeMindDatabase::class.java,
                    "node_mind_database"
                )
                .fallbackToDestructiveMigration() // For development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
