package com.cacaoforecast.ai.data.backup

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.cacaoforecast.ai.data.local.CacaoDatabase
import com.cacaoforecast.ai.data.repository.DefaultCacaoRepository
import com.cacaoforecast.ai.domain.model.Farm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SqliteBackupManagerTest {

    private lateinit var context: Context
    private lateinit var db: CacaoDatabase
    private lateinit var repository: DefaultCacaoRepository
    private lateinit var backupManager: SqliteBackupManager
    private lateinit var exportFile: File
    private lateinit var invalidFile: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(CacaoDatabase.DB_NAME)
        db = CacaoDatabase.create(context)
        repository = DefaultCacaoRepository(
            databaseProvider = { db },
            sessionDirProvider = { id -> File(context.cacheDir, "bk_test_$id") }
        )
        backupManager = SqliteBackupManager(
            context = context,
            currentDatabase = { db },
            closeDatabase = { if (db.isOpen) db.close() },
            reopenDatabase = {
                if (db.isOpen) db.close()
                db = CacaoDatabase.create(context)
                db
            }
        )
        exportFile = File(context.cacheDir, "exported_backup.db").apply { delete() }
        invalidFile = File(context.cacheDir, "corrupt_backup.db").apply { delete() }
    }

    @After
    fun tearDown() {
        if (db.isOpen) db.close()
        context.deleteDatabase(CacaoDatabase.DB_NAME)
        exportFile.delete()
        invalidFile.delete()
    }

    @Test
    fun `exporta banco integro, restaura apos reabrir Room e rejeita arquivo corrompido sem perda`() = runBlocking {
        repository.saveFarm(Farm(nome = "Fazenda Original Backup")).getOrThrow()
        assertEquals(1, repository.observeActiveFarms().first().size)

        val exportRes = backupManager.exportToUri(Uri.fromFile(exportFile))
        assertTrue(exportRes.isSuccess)
        assertTrue(exportFile.exists() && exportFile.length() > 100L)

        // Adiciona nova fazenda apos o backup
        repository.saveFarm(Farm(nome = "Fazenda Temporária")).getOrThrow()
        assertEquals(2, repository.observeActiveFarms().first().size)

        // Tenta importar arquivo invalido -> deve falhar mantendo os 2 registros atuais intactos
        invalidFile.writeText("NOT_A_VALID_SQLITE_FILE")
        val corruptImport = backupManager.importFromUri(Uri.fromFile(invalidFile))
        assertTrue(corruptImport.isFailure)
        assertEquals(2, repository.observeActiveFarms().first().size)

        // Importa backup valido -> restaura estado com 1 fazenda
        val validImport = backupManager.importFromUri(Uri.fromFile(exportFile))
        assertTrue(validImport.isSuccess)
        val restoredFarms = repository.observeActiveFarms().first()
        assertEquals(1, restoredFarms.size)
        assertEquals("Fazenda Original Backup", restoredFarms.single().nome)
    }
}
