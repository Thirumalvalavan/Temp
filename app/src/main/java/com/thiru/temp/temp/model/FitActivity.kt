package com.thiru.temp.temp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.thiru.temp.temp.R

/**
 * Entity that describes an activity performed by the user.
 *
 * This entity is used for the Room DB in the fit_activities table.
 */
@Entity(
    tableName = "fit_activities",
    indices = [Index("id")]
)
data class FitActivity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "type") val type: Type = Type.UNKNOWN,
    @ColumnInfo(name = "distanceMeters") val distanceMeters: Double,
    @ColumnInfo(name = "durationMs") val durationMs: Long
) {

    /**
     * Defines the type of activity
     *
     * @see https://developers.google.com/actions/reference/built-in-intents/ for different
     * supported exercise names by App Actions
     */
    enum class Type(val nameId: Int) {
        UNKNOWN(R.string.activity_unknown),
        RUNNING(R.string.activity_running),
        WALKING(R.string.activity_walking),
        CYCLING(R.string.activity_cycling);

        companion object {

            /**
             * @return a FitActivity.Type that matches the given name
             */
            fun find(type: String): Type {
                return values().find { it.name.equals(other = type, ignoreCase = true) } ?: UNKNOWN
            }
        }
    }
}