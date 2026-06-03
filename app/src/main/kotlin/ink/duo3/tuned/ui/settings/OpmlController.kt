package ink.duo3.tuned.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ink.duo3.tuned.R
import ink.duo3.tuned.presentation.settings.OpmlEvent
import ink.duo3.tuned.presentation.settings.SettingsUiState
import ink.duo3.tuned.presentation.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The import/export click handlers, wired to SAF launchers + a snackbar. */
internal class OpmlController(
    val onImport: () -> Unit,
    val onExport: () -> Unit,
)

/**
 * Bridges the settings screen to the Storage Access Framework: import reads a picked
 * document and hands its text to the ViewModel; export waits for the ViewModel to
 * produce an OPML document, then launches the create-document picker holding it.
 * One-shot results are reported via [snackbarHostState].
 */
@Composable
internal fun rememberOpmlController(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel,
): OpmlController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportFilename = stringResource(R.string.settings_opml_export_filename)
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch { importOpmlFromUri(context, uri, snackbarHostState, viewModel::importOpml) }
            }
        }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(OPML_MIME_TYPE)) { uri ->
            val content = pendingExport
            pendingExport = null
            if (uri != null && content != null) {
                scope.launch { writeOpmlToUri(context, uri, content, snackbarHostState) }
            }
        }

    OpmlEventEffect(
        event = state.opmlEvent,
        snackbarHostState = snackbarHostState,
        onConsume = viewModel::consumeOpmlEvent,
        onExportReady = { content ->
            pendingExport = content
            exportLauncher.launch(exportFilename)
        },
    )

    return remember(importLauncher) {
        OpmlController(
            onImport = { importLauncher.launch(OPML_IMPORT_MIME_TYPES) },
            onExport = viewModel::exportOpml,
        )
    }
}

@Composable
private fun OpmlEventEffect(
    event: OpmlEvent?,
    snackbarHostState: SnackbarHostState,
    onConsume: () -> Unit,
    onExportReady: (String) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(event) {
        when (event) {
            is OpmlEvent.Imported -> {
                onConsume()
                snackbarHostState.showSnackbar(importMessage(context, event))
            }

            OpmlEvent.ImportFailed -> {
                onConsume()
                snackbarHostState.showSnackbar(context.getString(R.string.settings_opml_import_error))
            }

            is OpmlEvent.ExportReady -> {
                onConsume()
                onExportReady(event.content)
            }

            OpmlEvent.ExportFailed -> {
                onConsume()
                snackbarHostState.showSnackbar(context.getString(R.string.settings_opml_export_error))
            }

            null -> Unit
        }
    }
}

private fun importMessage(
    context: Context,
    event: OpmlEvent.Imported,
): String =
    if (event.imported + event.failed == 0) {
        context.getString(R.string.settings_opml_import_empty)
    } else {
        context.getString(R.string.settings_opml_import_result, event.imported, event.imported + event.failed)
    }

private suspend fun importOpmlFromUri(
    context: Context,
    uri: Uri,
    snackbarHostState: SnackbarHostState,
    onContent: (String) -> Unit,
) {
    val content =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            }.getOrNull()
        }
    if (content == null) {
        snackbarHostState.showSnackbar(context.getString(R.string.settings_opml_import_error))
    } else {
        onContent(content)
    }
}

private suspend fun writeOpmlToUri(
    context: Context,
    uri: Uri,
    content: String,
    snackbarHostState: SnackbarHostState,
) {
    val ok =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                    ?: error("No output stream")
            }.isSuccess
        }
    snackbarHostState.showSnackbar(
        context.getString(
            if (ok) R.string.settings_opml_export_success else R.string.settings_opml_export_error,
        ),
    )
}

private const val OPML_MIME_TYPE = "text/x-opml"

private val OPML_IMPORT_MIME_TYPES =
    arrayOf(
        "text/x-opml",
        "text/xml",
        "application/xml",
        "text/plain",
        "application/octet-stream",
    )
