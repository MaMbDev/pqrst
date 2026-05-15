package dam.pmdm.pqrstlearn.presentation.report

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PageRange
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dam.pmdm.pqrstlearn.R
import dam.pmdm.pqrstlearn.presentation.component.PqrstTopBar
import dam.pmdm.pqrstlearn.ui.theme.PqrstTheme
import java.io.FileOutputStream
import java.io.IOException

/**
 * Report preview and export screen (RF-08).
 *
 * Renders the PDF generation lifecycle and provides two export actions once the PDF is ready:
 * - **Print** — invokes the Android [PrintManager] with [PdfPrintAdapter], which streams
 *   the generated PDF to any printer or "Save to PDF" virtual printer the system offers.
 * - **Share** — fires an [Intent.ACTION_SEND] with the PDF URI so the user can send it
 *   via email, Drive, or any other share target.
 *
 * **Layout states**
 * - [ReportState.isLoading] = true → spinner + "Generating…" text.
 * - [ReportState.error] != null → error message + "Retry" button (calls [ReportPreviewViewModel.generate]).
 * - [ReportState.pdfUri] != null → "Ready" text + Print and Share buttons.
 *
 * State is hoisted to [ReportPreviewViewModel]. The [LocalContext] is retrieved in this
 * composable (not the ViewModel) because [PrintManager] and [Intent.createChooser] require
 * an Activity context, which must not be held by the ViewModel.
 *
 * @param consultationId The ID of the consultation whose data populates the report.
 * @param ecgRecordId The ID of the ECG record to include in the report; null omits the ECG section.
 * @param onBack Callback invoked when the user taps the back arrow.
 * @param viewModel Hilt-injected ViewModel; overridable for tests.
 */
@Composable
fun ReportPreviewScreen(
    consultationId: Long,
    ecgRecordId: Long?,
    onBack: () -> Unit,
    viewModel: ReportPreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            PqrstTopBar(
                title = stringResource(R.string.report_title),
                role = null,
                onMenuClick = {},
                onBackClick = onBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.report_generating),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                state.error != null -> Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = viewModel::generate) {
                        Text(stringResource(R.string.report_retry))
                    }
                }

                state.pdfUri != null -> Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.report_ready),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))

                    // Print button — delegates to Android PrintManager with a custom adapter
                    Button(
                        onClick = {
                            val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                            pm.print(
                                "pqrst_informe",
                                PdfPrintAdapter(context, state.pdfUri!!),
                                PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .build(),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.report_print))
                    }

                    // Share button — ACTION_SEND with FLAG_GRANT_READ_URI_PERMISSION so the
                    // receiving app can read the FileProvider URI without needing storage permission.
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, state.pdfUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(intent, context.getString(R.string.report_share))
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.report_share))
                    }
                }
            }
        }
    }
}

/** Preview of the screen in its loading / generating state. */
@Preview(showBackground = true)
@Composable
private fun ReportLoadingPreview() {
    PqrstTheme {
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = stringResource(R.string.report_title),
                    role = null,
                    onMenuClick = {},
                    onBackClick = {},
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.report_generating),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/** Preview of the screen with the PDF ready for printing / sharing. */
@Preview(showBackground = true)
@Composable
private fun ReportReadyPreview() {
    PqrstTheme {
        Scaffold(
            topBar = {
                PqrstTopBar(
                    title = stringResource(R.string.report_title),
                    role = null,
                    onMenuClick = {},
                    onBackClick = {},
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.report_ready),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.report_print))
                }
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.report_share))
                }
            }
        }
    }
}

/**
 * [PrintDocumentAdapter] that streams a PDF file stored at [pdfUri] to the Android print system.
 *
 * This adapter is intentionally minimal: it reports the document as A4 with an unknown page count
 * (suitable for a pre-rendered PDF) and copies the raw PDF bytes directly to the print destination.
 * This approach avoids re-rendering the PDF in memory and works with any standard PDF viewer.
 *
 * @param context Context used to open the PDF content stream via [ContentResolver].
 * @param pdfUri FileProvider URI of the generated PDF file.
 */
private class PdfPrintAdapter(
    private val context: Context,
    private val pdfUri: android.net.Uri,
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal.isCanceled) { callback.onLayoutCancelled(); return }
        val info = PrintDocumentInfo.Builder("pqrst_informe.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback,
    ) {
        try {
            context.contentResolver.openInputStream(pdfUri)?.use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("No se pudo abrir el PDF")
            if (cancellationSignal.isCanceled) {
                callback.onWriteCancelled()
            } else {
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        }
    }
}
