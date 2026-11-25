package com.cacaoforecast.ai.pdf

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ReportShareHelper {

    fun uriForFile(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun sharePdfToWhatsApp(context: Context, pdfFile: File, caption: String) {
        val uri = uriForFile(context, pdfFile)
        val pm = context.packageManager
        val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

        val targetPackage = whatsappPackages.firstOrNull { pkg ->
            val probe = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                setPackage(pkg)
            }
            probe.resolveActivity(pm) != null
        }

        if (targetPackage != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                setPackage(targetPackage)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } else {
            shareFileGeneric(context, pdfFile, "application/pdf", "Enviar Relatório PDF", caption)
        }
    }

    fun shareFileGeneric(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String,
        caption: String? = null
    ) {
        val uri = uriForFile(context, file)
        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            if (!caption.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(baseIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }
}
