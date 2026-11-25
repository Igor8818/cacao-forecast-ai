package com.cacaoforecast.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cacaoforecast.ai.data.local.entity.FarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {
    @Query("SELECT * FROM fazendas WHERE ativo = 1 ORDER BY nome COLLATE NOCASE ASC")
    fun observeActiveFarms(): Flow<List<FarmEntity>>

    @Query("SELECT * FROM fazendas WHERE id = :farmId LIMIT 1")
    fun observeById(farmId: Long): Flow<FarmEntity?>

    @Query("SELECT * FROM fazendas WHERE id = :farmId LIMIT 1")
    suspend fun getById(farmId: Long): FarmEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: FarmEntity): Long

    @Update
    suspend fun update(entity: FarmEntity)

    @Query("UPDATE fazendas SET ativo = 0, atualizado_em = :updatedAt WHERE id = :farmId")
    suspend fun softDelete(farmId: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM fazendas")
    suspend fun countAll(): Int
}
