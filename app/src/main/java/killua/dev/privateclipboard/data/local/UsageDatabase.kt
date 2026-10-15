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
    @ColumnInfo(defaultValue = "0") val read: Long = 0,
    @ColumnInfo(defaultValue = "0") val write: Long = 0,
    @ColumnInfo(defaultValue = "0") val denied: Long = 0,
)

data class UsageTotals(val read: Long?, val write: Long?)

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

    @Query("SELECT SUM(`read`) AS `read`, SUM(`write`) AS `write` FROM usage WHERE packageName NOT IN (:excluded)")
    abstract fun observeTotals(excluded: Collection<String>): Flow<UsageTotals>

    @Query("SELECT * FROM usage WHERE packageName = :packageName")
    abstract fun observe(packageName: String): Flow<UsageEntity?>

    @Query("INSERT OR IGNORE INTO usage (packageName) VALUES (:packageName)")
    protected abstract suspend fun ensure(packageName: String)

    @Query(
        "UPDATE usage SET `read` = `read` + :read, `write` = `write` + :write, denied = denied + :denied " +
            "WHERE packageName = :packageName"
    )
    protected abstract suspend fun bump(packageName: String, read: Int, write: Int, denied: Int)

    @Transaction
    open suspend fun increment(packageName: String, read: Int, write: Int, denied: Int) {
        ensure(packageName)
        bump(packageName, read, write, denied)
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

@Database(entities = [UsageEntity::class, EventEntity::class], version = 4, exportSchema = false)
abstract class UsageDatabase : RoomDatabase() {
    abstract fun usage(): UsageDao

    abstract fun events(): EventDao

    companion object {
        const val NAME = "usage.db"
    }
}
