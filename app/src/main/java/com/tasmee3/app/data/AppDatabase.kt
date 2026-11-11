package com.tasmee3.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.tasmee3.app.match.SessionResult

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val finishedAt: Long,
    val page: Int,
    val surahName: String,
    val total: Int,
    val firstTry: Int,
    val notRecited: Int,
    val completed: Boolean,
)

@Entity(
    tableName = "review_words",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class ReviewWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val wordId: String,
    val text: String,
    val reason: String,
    val heard: String?,
)

@Dao
abstract class SessionDao {
    @Insert abstract suspend fun insertSession(session: SessionEntity): Long
    @Insert abstract suspend fun insertWords(words: List<ReviewWordEntity>)

    @Transaction
    open suspend fun save(session: SessionEntity, rows: List<ReviewRow>): Long {
        val id = insertSession(session)
        insertWords(rows.map { ReviewWordEntity(sessionId = id, wordId = it.wordId, text = it.text, reason = it.reason, heard = it.heard) })
        return id
    }

    @Query("SELECT * FROM sessions ORDER BY finishedAt DESC LIMIT :limit")
    abstract suspend fun recent(limit: Int): List<SessionEntity>

    @Query("SELECT * FROM review_words WHERE sessionId = :sessionId")
    abstract suspend fun wordsOf(sessionId: Long): List<ReviewWordEntity>

    @Query("SELECT COUNT(*) FROM sessions")
    abstract suspend fun count(): Int
}

/** On-device database for finished sessions and their review words. Nothing leaves the phone. */
@Database(entities = [SessionEntity::class, ReviewWordEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tasmee3.db")
                .build().also { instance = it }
        }
    }
}

/** Saves a finished session. */
class SessionStore(private val dao: SessionDao) {
    suspend fun save(result: SessionResult, nowMs: Long = System.currentTimeMillis()): Long {
        val s = result.summary
        val entity = SessionEntity(
            finishedAt = nowMs, page = result.page, surahName = result.surahName, total = s.total,
            firstTry = s.firstTryCorrect, notRecited = s.notRecited, completed = s.completed
        )
        return dao.save(entity, SessionRows.reviewRows(s.review))
    }
}
