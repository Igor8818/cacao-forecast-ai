package com.cacaoforecast.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "amostras_planta",
    foreignKeys = [
        ForeignKey(
            entity = EvaluationEntity::class,
            parentColumns = ["id"],
            childColumns = ["avaliacao_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["avaliacao_id", "indice_planta"], name = "idx_amostras_avaliacao_indice", unique = true)
    ]
)
data class PlantSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "avaliacao_id")
    val avaliacaoId: Long,
    @ColumnInfo(name = "indice_planta")
    val indicePlanta: Int,
    @ColumnInfo(name = "n_sadios")
    val nSadios: Int,
    @ColumnInfo(name = "n_doentes")
    val nDoentes: Int,
    @ColumnInfo(name = "n_podridao_parda")
    val nPodridaoParda: Int = 0,
    @ColumnInfo(name = "n_moniliase")
    val nMoniliase: Int = 0,
    @ColumnInfo(name = "n_vassoura_bruxa")
    val nVassouraBruxa: Int = 0,
    @ColumnInfo(name = "editado_manualmente")
    val editadoManualmente: Int = 0,
    @ColumnInfo(name = "foto_temp_path")
    val fotoTempPath: String? = null,
    @ColumnInfo(name = "capturado_em")
    val capturadoEm: Long
)
