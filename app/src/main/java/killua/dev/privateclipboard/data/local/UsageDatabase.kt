package killua.dev.privateclipboard.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import killua.dev.arsenal.data.room.BaseDao
import kotlinx.coroutines.flow.Flow

/** SQL defaults are required: [UsageDao.ensure] inserts only the key, and OR IGNORE would swallow NOT NULL failures. */
@Entity(tableName = "usage")
data class UsageEntity(
    @PrimaryKey val packageName: String,
    @ColumnInfo(defaultValue = "0") val autoRead: Long = 0,
    @ColumnInfo(defaultValue = "0") val autoWrite: Long = 0,
    @ColumnInfo(defaultValue = "0") val manualRead: Long = 0,
    @ColumnInfo(defaultValue = "0") val manualWrite: Long = 0,
    @ColumnInfo(defaultValue = "0") val denied: Long = 0,
)

data class UsageTotals(
    val autoRead: Long?,
    val autoWrite: Long?,
    val manualRead: Long?,
    val manualWrite: Long?,
)

@Entity(tableName = "events", indices = [Index("packageName", "category", "occurredAt")])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val category: String,
    val outcome: String,
    val content: String,
    val occurredAt: Long,
)

@Dao
abstract class UsageDao : BaseDao<UsageEntity> {

    @Query("SELECT SUM(autoRead) AS autoRead, SUM(autoWrite) AS autoWrite, SUM(manualRead) AS manualRead, SUM(manualWrite) AS manualWrite FROM usage")
    abstract fun observeTotals(): Flow<UsageTotals>

    @Query("SELECT * FROM usage WHERE packageName = :packageName")
    abstract fun observe(packageName: String): Flow<UsageEntity?>

    @Query("INSERT OR IGNORE INTO usage (packageName) VALUES (:packageName)")
    protected abstract suspend fun ensure(packageName: String)

    @Query(
        """
        UPDATE usage SET
            autoRead = autoRead + :autoRead,
            autoWrite = autoWrite + :autoWrite,
            manualRead = manualRead + :manualRead,
            manualWrite = manualWrite + :manualWrite,
            denied = denied + :denied
        WHERE packageName = :packageName
        """
    )
    protected abstract suspend fun bump(
        packageName: String,
        autoRead: Int,
        autoWrite: Int,
        manualRead: Int,
        manualWrite: Int,
        denied: Int,
    )

    @Transaction
    open suspend fun increment(
        packageName: String,
        autoRead: Int = 0,
        autoWrite: Int = 0,
        manualRead: Int = 0,
        manualWrite: Int = 0,
        denied: Int = 0,
    ) {
        ensure(packageName)
        bump(packageName, autoRead, autoWrite, manualRead, manualWrite, denied)
    }
}

@Dao
abstract class EventDao : BaseDao<EventEntity> {

    @Query(
        "SELECT * FROM events WHERE packageName = :packageName AND category = :category " +
            "ORDER BY occurredAt DESC LIMIT :limit"
    )
    abstract fun observe(packageName: String, category: String, limit: Int): Flow<List<EventEntity>>

    @Query("DELETE FROM events WHERE id NOT IN (SELECT id FROM events ORDER BY occurredAt DESC LIMIT :keep)")
    abstract suspend fun trim(keep: Int)
}

@Database(entities = [UsageEntity::class, EventEntity::class], version = 3, exportSchema = false)
abstract class UsageDatabase : RoomDatabase() {
    abstract fun usage(): UsageDao

    abstract fun events(): EventDao

    companion object {
        const val NAME = "usage.db"
    }
}
