package com.mhss.app.tracking.data.csv

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

data class TrackingCsvExportResult(
    val fileName: String,
    val counts: TrackingCsvCounts
)

@Factory
class TrackingCsvManager(
    private val context: Context,
    private val codec: TrackingCsvCodec,
    private val store: TrackingCsvSnapshotStore,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun export(directoryUri: String): TrackingCsvExportResult =
        withContext(ioDispatcher) {
            val uri = directoryUri.toUri()
            takePersistablePermission(uri.toString())
            val directory = DocumentFile.fromTreeUri(context, uri)
                ?: throw TrackingCsvException(TrackingCsvErrorReason.WRITE_FAILED)
            val fileName = "DailyFlow_Tracking_${
                SimpleDateFormat(FILE_TIMESTAMP_PATTERN, Locale.US).format(Date())
            }.csv"
            val destination = directory.createFile(CSV_MIME_TYPE, fileName)
                ?: throw TrackingCsvException(TrackingCsvErrorReason.WRITE_FAILED)
            try {
                val snapshot = store.readSnapshot()
                val output = context.contentResolver.openOutputStream(destination.uri, "wt")
                    ?: throw TrackingCsvException(TrackingCsvErrorReason.WRITE_FAILED)
                output.use { stream ->
                    OutputStreamWriter(stream, StandardCharsets.UTF_8).use { writer ->
                        codec.write(snapshot, writer)
                    }
                }
                TrackingCsvExportResult(
                    fileName = destination.name ?: fileName,
                    counts = snapshot.counts()
                )
            } catch (exception: CancellationException) {
                destination.delete()
                throw exception
            } catch (exception: TrackingCsvException) {
                destination.delete()
                throw exception
            } catch (exception: Exception) {
                destination.delete()
                throw TrackingCsvException(
                    reason = TrackingCsvErrorReason.WRITE_FAILED,
                    detail = exception.message,
                    cause = exception
                )
            }
        }

    suspend fun preview(fileUri: String): TrackingCsvImportPreview =
        withContext(ioDispatcher) {
            val uri = fileUri.toUri()
            takePersistablePermission(uri.toString())
            try {
                val sourceName = DocumentFile.fromSingleUri(context, uri)?.name
                    ?: uri.lastPathSegment
                    ?: "tracking.csv"
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw TrackingCsvException(TrackingCsvErrorReason.READ_FAILED)
                input.use { stream ->
                    InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
                        codec.preview(reader, sourceName)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: TrackingCsvException) {
                throw exception
            } catch (exception: Exception) {
                throw TrackingCsvException(
                    reason = TrackingCsvErrorReason.READ_FAILED,
                    detail = exception.message,
                    cause = exception
                )
            }
        }

    suspend fun import(preview: TrackingCsvImportPreview): TrackingCsvCounts =
        withContext(ioDispatcher) {
            try {
                store.import(preview.snapshot)
                preview.counts
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                throw TrackingCsvException(
                    reason = TrackingCsvErrorReason.IMPORT_FAILED,
                    detail = exception.message,
                    cause = exception
                )
            }
        }

    private fun takePersistablePermission(uri: String) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    private companion object {
        const val CSV_MIME_TYPE = "text/csv"
        const val FILE_TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss"
    }
}
