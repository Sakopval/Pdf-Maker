package com.rk.activities.main.session

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.Serializable

@Entity(tableName = "document_state")
data class DocumentState(
    @PrimaryKey val path: String,
    val cursorLineLeft: Int,
    val cursorColumnLeft: Int,
    val cursorLineRight: Int,
    val cursorColumnRight: Int,
    val scrollX: Int,
    val scrollY: Int,
    val lastOpened: Long,
) : Serializable

@Dao
interface DocumentStateDao {
    @Query("SELECT * FROM document_state WHERE path = :path") suspend fun getState(path: String): DocumentState?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertState(state: DocumentState)

    @Query(
        "DELETE FROM document_state WHERE path NOT IN (SELECT path FROM document_state ORDER BY lastOpened DESC LIMIT :limit)"
    )
    suspend fun deleteOldestRecords(limit: Int)

    @Query("DELETE FROM document_state WHERE path = :path") suspend fun deleteByPath(path: String)

    @Query("DELETE FROM document_state WHERE lastOpened < :timestamp") suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM document_state") suspend fun clear()
}

@Database(entities = [DocumentState::class], version = 1, exportSchema = false)
abstract class DocumentStateDatabase : RoomDatabase() {
    abstract fun documentStateDao(): DocumentStateDao

    companion object {
        @Volatile private var INSTANCE: DocumentStateDatabase? = null

        fun getDatabase(context: Context): DocumentStateDatabase {
            return INSTANCE
                ?: synchronized(this) {
                    val instance =
                        Room.databaseBuilder(
                                context,
                                DocumentStateDatabase::class.java,
                                "document_state_database",
                            )
                            .build()
                    INSTANCE = instance
                    instance
                }
        }
    }
}
