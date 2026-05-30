package com.wakeup.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "mission_configs",
    foreignKeys = [ForeignKey(
        entity = AlarmEntity::class,
        parentColumns = ["id"],
        childColumns = ["alarmId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("alarmId")],
)
data class MissionConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    val order: Int,
    val type: String,   // MissionType.name
    val params: String, // MissionParamsCodec JSON
)

data class AlarmWithMissions(
    @Embedded val alarm: AlarmEntity,
    @Relation(parentColumn = "id", entityColumn = "alarmId")
    val missions: List<MissionConfigEntity>,
)
