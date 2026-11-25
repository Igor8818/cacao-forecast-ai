package com.cacaoforecast.ai.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.cacaoforecast.ai.data.local.CacaoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SqliteBackupManager(
    private val context: Context,
    private val currentDatabase: () -> CacaoDatabase,
    private val closeDatabase: () -> Unit,
    private val reopenDatabase: () -> CacaoDatabase
) {
    companion object {
        private val SQLITE_MAGIC_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        private val REQUIRED_TABLES = setOf("fazendas", "quadras", "avaliacoes", "amostras_planta")
    }

    suspend fun exportToUri(destUri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val dbFile = checkpointAndGetDbFile()
            val out = context.contentResolver.openOutputStream(destUri)
                ?: error("Não foi possível abrir o destino para gravação.")
            out.use { output ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(output)
                }
                output.flush()
            }
            dbFile.length()
        }
    }

    suspend fun exportToShareableFile(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dbFile = checkpointAndGetDbFile()
            val backupsDir = File(context.filesDir, "backups").apply {
                if (!exists()) mkdirs()
            }
            val outFile = File(backupsDir, "cacao_forecast_backup_${System.currentTimeMillis()}.db")
            FileInputStream(dbFile).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        }
    }

    suspend fun importFromUri(sourceUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val validateDir = File(context.cacheDir, "backup_validate").apply {
            if (!exists()) mkdirs()
        }
        val candidateFile = File(validateDir, "candidate_${System.currentTimeMillis()}.db")
        val dbFile = context.getDatabasePath(CacaoDatabase.DB_NAME)
        val walFile = File("${dbFile.absolutePath}-wal")
        val shmFile = File("${dbFile.absolutePath}-shm")

        val bakDb = File(validateDir, "rollback.db")
        val bakWal = File(validateDir, "rollback.db-wal")
        val bakShm = File(validateDir, "rollback.db-shm")

        try {
            val input = context.contentResolver.openInputStream(sourceUri)
                ?: error("Não foi possível abrir o arquivo selecionado.")
            input.use { stream ->
                FileOutputStream(candidateFile).use { out ->
                    stream.copyTo(out)
                }
            }

            validateSqliteCandidate(candidateFile)

            // Garante checkpoint antes do snapshot de rollback
            runCatching { checkpointWal() }

            dbFile.parentFile?.mkdirs()
            if (dbFile.exists()) dbFile.copyTo(bakDb, overwrite = true) else bakDb.delete()
            if (walFile.exists()) walFile.copyTo(bakWal, overwrite = true) else bakWal.delete()
            if (shmFile.exists()) shmFile.copyTo(bakShm, overwrite = true) else bakShm.delete()

            closeDatabase()

            try {
                candidateFile.copyTo(dbFile, overwrite = true)
                walFile.delete()
                shmFile.delete()

                val reopened = reopenDatabase()
                reopened.farmDao().countAll()
                Result.success(Unit)
            } catch (swapError: Throwable) {
                // Rollback automático em caso de qualquer falha ao trocar ou reabrir
                runCatching { closeDatabase() }
                if (bakDb.exists()) bakDb.copyTo(dbFile, overwrite = true)
                if (bakWal.exists()) bakWal.copyTo(walFile, overwrite = true) else walFile.delete()
                if (bakShm.exists()) bakShm.copyTo(shmFile, overwrite = true) else shmFile.delete()
                runCatching { reopenDatabase() }
                Result.failure(swapError)
            }
        } catch (err: Throwable) {
            Result.failure(err)
        } finally {
            candidateFile.delete()
            bakDb.delete()
            bakWal.delete()
            bakShm.delete()
        }
    }

    private fun checkpointAndGetDbFile(): File {
        checkpointWal()
        val dbFile = context.getDatabasePath(CacaoDatabase.DB_NAME)
        require(dbFile.exists() && dbFile.length() > 0L) {
            "Banco de dados local ainda não foi inicializado."
        }
        return dbFile
    }

    private fun checkpointWal() {
        val sqlDb = currentDatabase().openHelper.writableDatabase
        sqlDb.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
            cursor.moveToFirst()
        }
    }

    private fun validateSqliteCandidate(candidateFile: File) {
        require(candidateFile.exists() && candidateFile.length() >= 100L) {
            "Arquivo inválido ou vazio."
        }
        val header = ByteArray(16)
        FileInputStream(candidateFile).use { fis ->
            val read = fis.read(header)
            require(read == 16 && header.contentEquals(SQLITE_MAGIC_HEADER)) {
                "O arquivo selecionado não é um banco SQLite válido."
            }
        }

        val sqliteDb = SQLiteDatabase.openDatabase(
            candidateFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        try {
            sqliteDb.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                require(cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)) {
                    "Falha na verificação de integridade (PRAGMA integrity_check)."
                }
            }

            sqliteDb.rawQuery("PRAGMA user_version", null).use { cursor ->
                require(cursor.moveToFirst()) { "Versão de schema ausente." }
                val version = cursor.getInt(0)
                require(version in 1..CacaoDatabase.VERSION) {
                    "Versão de backup incompatível ($version)."
                }
            }

            val foundTables = mutableSetOf<String>()
            sqliteDb.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    foundTables.add(cursor.getString(0))
                }
            }
            require(foundTables.containsAll(REQUIRED_TABLES)) {
                "Backup incompatível: tabelas do CacaoForecastAI ausentes."
            }
        } finally {
            sqliteDb.close()
        }
    }
}
