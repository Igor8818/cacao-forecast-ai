package com.cacaoforecast.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fazendas",
    indices = [
        Index(value = ["ativo", "nome"], name = "idx_fazendas_ativo_nome")
    ]
)
data class FarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val nome: String,
    @ColumnInfo(name = "municipio_uf")
    val municipioUf: String?,
    val proprietario: String?,
    val ativo: Int = 1,
    @ColumnInfo(name = "criado_em")
    val criadoEm: Long,
    @ColumnInfo(name = "atualizado_em")
    val atualizadoEm: Long
)
