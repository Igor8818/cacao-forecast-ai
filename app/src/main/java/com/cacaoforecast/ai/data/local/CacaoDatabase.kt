package com.cacaoforecast.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cacaoforecast.ai.data.local.dao.EvaluationDao
import com.cacaoforecast.ai.data.local.dao.FarmDao
import com.cacaoforecast.ai.data.local.dao.PlotDao
import com.cacaoforecast.ai.data.local.entity.EvaluationEntity
import com.cacaoforecast.ai.data.local.entity.FarmEntity
import com.cacaoforecast.ai.data.local.entity.PlantSampleEntity
import com.cacaoforecast.ai.data.local.entity.PlotEntity

@Database(
    entities = [
        FarmEntity::class,
        PlotEntity::class,
        EvaluationEntity::class,
        PlantSampleEntity::class
    ],
    version = CacaoDatabase.VERSION,
    exportSchema = true
)
abstract class CacaoDatabase : RoomDatabase() {
    abstract fun farmDao(): FarmDao
    abstract fun plotDao(): PlotDao
    abstract fun evaluationDao(): EvaluationDao

    companion object {
        const val VERSION = 1
        const val DB_NAME = "cacao_forecast.db"

        fun create(context: Context): CacaoDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CacaoDatabase::class.java,
                DB_NAME
            ).build()
        }
    }
}
