package com.cacaoforecast.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cacaoforecast.ai.data.local.entity.EvaluationEntity
import com.cacaoforecast.ai.data.local.entity.EvaluationWithPeriodCountTuple
import com.cacaoforecast.ai.data.local.entity.PlantSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvaluation(entity: EvaluationEntity): Long

    @Update
    suspend fun updateEvaluation(entity: EvaluationEntity)

    @Query("SELECT * FROM avaliacoes WHERE id = :evaluationId LIMIT 1")
    suspend fun getEvaluationById(evaluationId: Long): EvaluationEntity?

    @Query("SELECT * FROM avaliacoes WHERE quadra_id = :plotId AND status = 'IN_PROGRESS' ORDER BY iniciado_em DESC, id DESC LIMIT 1")
    suspend fun findInProgressByPlot(plotId: Long): EvaluationEntity?

    @Query("SELECT * FROM avaliacoes WHERE status = 'IN_PROGRESS' ORDER BY iniciado_em DESC, id DESC LIMIT 1")
    suspend fun findAnyInProgress(): EvaluationEntity?

    @Query("SELECT * FROM avaliacoes WHERE quadra_id = :plotId AND status = 'COMPLETED' ORDER BY data DESC, id DESC")
    fun observeCompletedByPlot(plotId: Long): Flow<List<EvaluationEntity>>

    @Query(
        """
        SELECT
            a.*,
            (
                SELECT COUNT(*)
                FROM avaliacoes ac
                WHERE ac.quadra_id = a.quadra_id
                  AND ac.status = 'COMPLETED'
                  AND ac.data BETWEEN :startMillis AND :endMillis
            ) AS qtd_avaliacoes_no_periodo
        FROM avaliacoes a
        WHERE a.fazenda_id = :farmId
          AND a.status = 'COMPLETED'
          AND a.data BETWEEN :startMillis AND :endMillis
          AND (:quadraId IS NULL OR a.quadra_id = :quadraId)
          AND a.id = (
              SELECT a2.id
              FROM avaliacoes a2
              WHERE a2.quadra_id = a.quadra_id
                AND a2.status = 'COMPLETED'
                AND a2.data BETWEEN :startMillis AND :endMillis
              ORDER BY a2.data DESC, a2.id DESC
              LIMIT 1
          )
        ORDER BY a.perdas_pct DESC, a.snapshot_quadra_nome ASC
        """
    )
    fun observeLatestCompletedPerPlotInPeriod(
        farmId: Long,
        startMillis: Long,
        endMillis: Long,
        quadraId: Long?
    ): Flow<List<EvaluationWithPeriodCountTuple>>

    @Query("SELECT * FROM amostras_planta WHERE avaliacao_id = :evaluationId ORDER BY indice_planta ASC")
    suspend fun getSamplesByEvaluation(evaluationId: Long): List<PlantSampleEntity>

    @Query("SELECT * FROM amostras_planta WHERE avaliacao_id = :evaluationId AND indice_planta = :plantIndex LIMIT 1")
    suspend fun getSampleByIndex(evaluationId: Long, plantIndex: Int): PlantSampleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlantSample(sample: PlantSampleEntity): Long

    @Query("UPDATE amostras_planta SET foto_temp_path = NULL WHERE avaliacao_id = :evaluationId")
    suspend fun clearTempPhotoPaths(evaluationId: Long)

    @Query("UPDATE avaliacoes SET producao_real = :realArrobas, producao_real_em = :updatedAt WHERE id = :evaluationId AND status = 'COMPLETED'")
    suspend fun updateRealProduction(evaluationId: Long, realArrobas: Double?, updatedAt: Long)

    @Query("UPDATE avaliacoes SET status = 'CANCELLED' WHERE id = :evaluationId")
    suspend fun markCancelled(evaluationId: Long)
}
