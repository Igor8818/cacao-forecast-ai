package com.cacaoforecast.ai.di

import android.content.Context
import com.cacaoforecast.ai.data.backup.SqliteBackupManager
import com.cacaoforecast.ai.data.local.CacaoDatabase
import com.cacaoforecast.ai.data.repository.DefaultCacaoRepository
import com.cacaoforecast.ai.domain.ml.FruitAnalysisEngine
import com.cacaoforecast.ai.domain.repository.CacaoRepository
import com.cacaoforecast.ai.ml.TfliteFruitAnalysisEngine
import com.cacaoforecast.ai.pdf.PdfReportGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    private val dbLock = Any()

    @Volatile
    private var databaseInstance: CacaoDatabase = CacaoDatabase.create(appContext)

    private val _databaseGeneration = MutableStateFlow(0)
    val databaseGeneration: StateFlow<Int> = _databaseGeneration.asStateFlow()

    val repository: CacaoRepository by lazy {
        DefaultCacaoRepository(
            databaseProvider = { currentDatabase() },
            sessionDirProvider = { evaluationId -> sessionDirFor(evaluationId) },
            databaseGeneration = databaseGeneration
        )
    }

    val fruitAnalysisEngine: FruitAnalysisEngine by lazy {
        TfliteFruitAnalysisEngine(appContext)
    }

    val pdfReportGenerator: PdfReportGenerator by lazy {
        PdfReportGenerator(appContext)
    }

    val backupManager: SqliteBackupManager by lazy {
        SqliteBackupManager(
            context = appContext,
            currentDatabase = { currentDatabase() },
            closeDatabase = { closeDatabase() },
            reopenDatabase = { reopenDatabase() }
        )
    }

    fun currentDatabase(): CacaoDatabase {
        synchronized(dbLock) {
            return databaseInstance
        }
    }

    fun closeDatabase() {
        synchronized(dbLock) {
            if (databaseInstance.isOpen) {
                databaseInstance.close()
            }
        }
    }

    fun reopenDatabase(): CacaoDatabase {
        synchronized(dbLock) {
            if (databaseInstance.isOpen) {
                databaseInstance.close()
            }
            databaseInstance = CacaoDatabase.create(appContext)
            _databaseGeneration.value += 1
            return databaseInstance
        }
    }

    fun sessionDirFor(evaluationId: Long): File {
        return File(appContext.filesDir, "sessions/$evaluationId").apply {
            if (!exists()) mkdirs()
        }
    }
}
