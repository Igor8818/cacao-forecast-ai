package com.cacaoforecast.ai.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "avaliacoes",
    foreignKeys = [
        ForeignKey(
            entity = FarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["fazenda_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["quadra_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["fazenda_id", "status", "data"], name = "idx_avaliacoes_relatorio"),
        Index(value = ["quadra_id", "status", "data"], name = "idx_avaliacoes_quadra_status")
    ]
)
data class EvaluationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "fazenda_id")
    val fazendaId: Long,
    @ColumnInfo(name = "quadra_id")
    val quadraId: Long,
    val status: String,
    @ColumnInfo(name = "snapshot_quadra_nome")
    val snapshotQuadraNome: String,
    @ColumnInfo(name = "snapshot_area_ha")
    val snapshotAreaHa: Double,
    @ColumnInfo(name = "snapshot_npt")
    val snapshotNpt: Int,
    @ColumnInfo(name = "snapshot_fa")
    val snapshotFa: Double,
    @ColumnInfo(name = "snapshot_npf")
    val snapshotNpf: Int,
    @ColumnInfo(name = "n_plantas_amostradas")
    val nPlantasAmostradas: Int = 0,
    @ColumnInfo(name = "n_frutos_sadios")
    val nFrutosSadios: Int = 0,
    @ColumnInfo(name = "n_frutos_doentes")
    val nFrutosDoentes: Int = 0,
    @ColumnInfo(name = "n_podridao_parda")
    val nPodridaoParda: Int = 0,
    @ColumnInfo(name = "n_moniliase")
    val nMoniliase: Int = 0,
    @ColumnInfo(name = "n_vassoura_bruxa")
    val nVassouraBruxa: Int = 0,
    @ColumnInfo(name = "producao_estimada")
    val producaoEstimada: Double = 0.0,
    @ColumnInfo(name = "producao_estimada_liquida")
    val producaoEstimadaLiquida: Double = 0.0,
    val produtividade: Double = 0.0,
    @ColumnInfo(name = "produtividade_liquida")
    val produtividadeLiquida: Double = 0.0,
    @ColumnInfo(name = "perdas_pct")
    val perdasPct: Double = 0.0,
    @ColumnInfo(name = "producao_real")
    val producaoReal: Double? = null,
    @ColumnInfo(name = "producao_real_em")
    val producaoRealEm: Long? = null,
    @ColumnInfo(name = "iniciado_em")
    val iniciadoEm: Long,
    val data: Long
)

data class EvaluationWithPeriodCountTuple(
    @Embedded
    val evaluation: EvaluationEntity,
    @ColumnInfo(name = "qtd_avaliacoes_no_periodo")
    val qtdAvaliacoesNoPeriodo: Int
)
