package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.*

class HydroScanConverters {
    @TypeConverter
    fun fromStationType(value: StationType): String = value.name

    @TypeConverter
    fun toStationType(value: String): StationType = try {
        StationType.valueOf(value)
    } catch (_: Exception) {
        StationType.ETA
    }

    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (_: Exception) {
        UserRole.OPERATOR
    }

    @TypeConverter
    fun fromCalibrationStatus(value: CalibrationStatus): String = value.name

    @TypeConverter
    fun toCalibrationStatus(value: String): CalibrationStatus = try {
        CalibrationStatus.valueOf(value)
    } catch (_: Exception) {
        CalibrationStatus.APROVADO
    }

    @TypeConverter
    fun fromAlertLevel(value: AlertLevel): String = value.name

    @TypeConverter
    fun toAlertLevel(value: String): AlertLevel = try {
        AlertLevel.valueOf(value)
    } catch (_: Exception) {
        AlertLevel.NORMAL
    }

    @TypeConverter
    fun fromReadingSource(value: ReadingSource): String = value.name

    @TypeConverter
    fun toReadingSource(value: String): ReadingSource = try {
        ReadingSource.valueOf(value)
    } catch (_: Exception) {
        ReadingSource.MANUAL_VERIFICADO
    }

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = try {
        SyncStatus.valueOf(value)
    } catch (_: Exception) {
        SyncStatus.SYNCED
    }
}
