package com.cacaoforecast.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quadras",
    foreignKeys = [
        ForeignKey(
            entity = FarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["fazenda_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["fazenda_id", "ativo", "nome"], name = "idx_quadras_fazenda_ativo")
    ]
)
data class PlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "fazenda_id")
    val fazendaId: Long,
    val nome: String,
    @ColumnInfo(name = "area_ha")
    val areaHa: Double,
    val npt: Int,
    val fa: Double,
    val npf: Int,
    val ativo: Int = 1,
    @ColumnInfo(name = "criado_em")
    val criadoEm: Long,
    @ColumnInfo(name = "atualizado_em")
    val atualizadoEm: Long
)
