package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {
    @Query("SELECT * FROM readings WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE stationId = :stationId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getReadingsByStation(stationId: String): Flow<List<ReadingEntity>>

    // Operador data isolation (visualizes only his own records)
    @Query("SELECT * FROM readings WHERE operatorId = :operatorId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getReadingsByOperator(operatorId: String): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE operatorId = :operatorId AND stationId = :stationId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getReadingsByOperatorAndStation(operatorId: String, stationId: String): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE id = :id LIMIT 1")
    suspend fun getReadingById(id: Long): ReadingEntity?

    @Query("SELECT * FROM readings WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncReadings(): List<ReadingEntity>

    @Query("UPDATE readings SET syncStatus = 'SYNCED', remoteId = :remoteId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markReadingSynced(id: Long, remoteId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM readings WHERE syncStatus = 'PENDING'")
    fun getPendingSyncCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: ReadingEntity): Long

    @Update
    suspend fun updateReading(reading: ReadingEntity)

    @Delete
    suspend fun deleteReading(reading: ReadingEntity)

    @Query("DELETE FROM readings WHERE id = :id")
    suspend fun deleteReadingById(id: Long)
}

@Dao
interface StationDao {
    @Query("SELECT * FROM stations ORDER BY name ASC")
    fun getAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE id = :id LIMIT 1")
    fun getStationById(id: String): Flow<StationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: StationEntity)

    @Update
    suspend fun updateStation(station: StationEntity)

    @Delete
    suspend fun deleteStation(station: StationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<StationEntity>)
}

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies LIMIT 1")
    fun getCompany(): Flow<CompanyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompany(company: CompanyEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE stationId = :stationId ORDER BY timestamp DESC")
    fun getAllAlertsByStation(stationId: String): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun getActiveAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE stationId = :stationId AND isResolved = 0 ORDER BY timestamp DESC")
    fun getActiveAlertsByStation(stationId: String): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Query("UPDATE alerts SET isResolved = 1 WHERE id = :id")
    suspend fun resolveAlert(id: Long)
}

@Dao
interface VpsConfigDao {
    @Query("SELECT * FROM vps_configs WHERE id = 'primary_vps' LIMIT 1")
    fun getVpsConfig(): Flow<VpsConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateVpsConfig(config: VpsConfigEntity)
}
