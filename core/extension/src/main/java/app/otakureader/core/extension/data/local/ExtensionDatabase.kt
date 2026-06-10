package app.otakureader.core.extension.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Room database entities and DAOs for storing extension metadata.
 */

@Entity(tableName = "extensions")
data class ExtensionEntity(
    @PrimaryKey
    val id: Long,
    
    @ColumnInfo(name = "pkg_name")
    val pkgName: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "version_code")
    val versionCode: Int,
    
    @ColumnInfo(name = "version_name")
    val versionName: String,
    
    @ColumnInfo(name = "sources_json")
    val sourcesJson: String, // JSON array of ExtensionSource
    
    @ColumnInfo(name = "status")
    val status: String, // InstallStatus.name
    
    @ColumnInfo(name = "apk_path")
    val apkPath: String?,

    @ColumnInfo(name = "apk_url")
    val apkUrl: String?,

    @ColumnInfo(name = "icon_url")
    val iconUrl: String?,
    
    @ColumnInfo(name = "lang")
    val lang: String,
    
    @ColumnInfo(name = "is_nsfw")
    val isNsfw: Boolean,
    
    @ColumnInfo(name = "install_date")
    val installDate: Long?,
    
    @ColumnInfo(name = "signature_hash")
    val signatureHash: String?,
    
    @ColumnInfo(name = "remote_version_code")
    val remoteVersionCode: Int?, // For tracking available updates

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    /**
     * The repository URL this extension was first installed from (#1019).
     * Recorded at install time and kept sticky across updates so a different
     * repository offering the same package name cannot silently replace the
     * extension via the update path. Null for rows created before this column
     * existed; backfilled on the next update check.
     */
    @ColumnInfo(name = "source_repo_url")
    val sourceRepoUrl: String? = null
)

@Dao
interface ExtensionDao {
    
    @Query("SELECT * FROM extensions WHERE status != 'AVAILABLE' ORDER BY name ASC")
    fun getInstalledExtensions(): Flow<List<ExtensionEntity>>
    
    @Query("SELECT * FROM extensions WHERE status = 'AVAILABLE' ORDER BY name ASC")
    fun getAvailableExtensions(): Flow<List<ExtensionEntity>>
    
    @Query("SELECT * FROM extensions WHERE status = 'HAS_UPDATE'")
    fun getExtensionsWithUpdates(): Flow<List<ExtensionEntity>>
    
    @Query("SELECT * FROM extensions WHERE pkg_name = :pkgName LIMIT 1")
    suspend fun getExtensionByPkgName(pkgName: String): ExtensionEntity?
    
    @Query("SELECT * FROM extensions WHERE id = :id LIMIT 1")
    suspend fun getExtensionById(id: Long): ExtensionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(entity: ExtensionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtensions(entities: List<ExtensionEntity>)
    
    @Delete
    suspend fun deleteExtension(entity: ExtensionEntity)
    
    @Query("DELETE FROM extensions WHERE pkg_name = :pkgName")
    suspend fun deleteByPkgName(pkgName: String)
    
    @Query("UPDATE extensions SET status = :status WHERE pkg_name = :pkgName")
    suspend fun updateStatus(pkgName: String, status: String)

    @Query("UPDATE extensions SET remote_version_code = :remoteVersion WHERE pkg_name = :pkgName")
    suspend fun updateRemoteVersion(pkgName: String, remoteVersion: Int)

    /** Backfills first-seen repo provenance for rows created before the column existed (#1019). */
    @Query("UPDATE extensions SET source_repo_url = :repoUrl WHERE pkg_name = :pkgName AND source_repo_url IS NULL")
    suspend fun backfillSourceRepoUrl(pkgName: String, repoUrl: String)

    @Query("UPDATE extensions SET is_enabled = :enabled WHERE pkg_name = :pkgName")
    suspend fun updateEnabled(pkgName: String, enabled: Boolean)
    
    @Query("SELECT * FROM extensions WHERE name LIKE '%' || :query || '%' OR pkg_name LIKE '%' || :query || '%'")
    fun searchExtensions(query: String): Flow<List<ExtensionEntity>>
    
    @Query("DELETE FROM extensions")
    suspend fun clearAll()
}

@Database(
    entities = [ExtensionEntity::class],
    version = 4,
    exportSchema = false
)
abstract class ExtensionDatabase : RoomDatabase() {
    abstract fun extensionDao(): ExtensionDao
}
