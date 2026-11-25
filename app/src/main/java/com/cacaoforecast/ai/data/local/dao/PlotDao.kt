package com.cacaoforecast.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cacaoforecast.ai.data.local.entity.PlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlotDao {
    @Query("SELECT * FROM quadras WHERE fazenda_id = :farmId AND ativo = 1 ORDER BY nome COLLATE NOCASE ASC")
    fun observeActiveByFarm(farmId: Long): Flow<List<PlotEntity>>

    @Query("SELECT * FROM quadras WHERE id = :plotId LIMIT 1")
    suspend fun getById(plotId: Long): PlotEntity?

    @Query("SELECT * FROM quadras WHERE fazenda_id = :farmId AND ativo = 1 ORDER BY nome COLLATE NOCASE ASC")
    suspend fun getActiveByFarm(farmId: Long): List<PlotEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PlotEntity): Long

    @Update
    suspend fun update(entity: PlotEntity)

    @Query("UPDATE quadras SET ativo = 0, atualizado_em = :updatedAt WHERE id = :plotId")
    suspend fun softDelete(plotId: Long, updatedAt: Long)

    @Query("UPDATE quadras SET ativo = 0, atualizado_em = :updatedAt WHERE fazenda_id = :farmId AND ativo = 1")
    suspend fun softDeleteByFarm(farmId: Long, updatedAt: Long)
}
