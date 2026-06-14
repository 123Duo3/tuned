package ink.duo3.tuned.ui.settings

import android.content.Context
import android.content.res.Resources
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
import androidx.compose.ui.platform.LocalResources
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
 * One-shot results are reported via [snackbarHostState]. Snackbar copy is resolved at
 * the composable layer (Compose lint forbids reading resources off LocalContext) and
 * passed down as plain strings.
 */
@Composable
internal fun rememberOpmlController(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel,
    externalImportUri: Uri? = null,
    onExternalImportConsumed: () -> Unit = {},
): OpmlController {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val exportFilename = stringResource(R.string.settings_opml_export_filename)
    val importErrorMessage = stringResource(R.string.settings_opml_import_error)
    val exportSuccessMessage = stringResource(R.string.settings_opml_export_success)
    val exportErrorMessage = stringResource(R.string.settings_opml_export_error)
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    val content = readOpml(context, uri)
                    if (content == null) {
                        snackbarHostState.showSnackbar(importErrorMessage)
                    } else {
                        viewModel.importOpml(content)
                    }
                }
            }
        }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(OPML_MIME_TYPE)) { uri ->
            val content = pendingExport
            pendingExport = null
            if (uri != null && content != null) {
                scope.launch {
                    val ok = writeOpml(context, uri, content)
                    snackbarHostState.showSnackbar(if (ok) exportSuccessMessage else exportErrorMessage)
                }
            }
        }

    ExternalOpmlImportEffect(
        uri = externalImportUri,
        enabled = !state.isOpmlBusy,
        handler =
            ExternalOpmlImportHandler(
                context = context,
                snackbarHostState = snackbarHostState,
                errorMessage = importErrorMessage,
                onImport = viewModel::importOpml,
                onConsumed = onExternalImportConsumed,
            ),
    )

    OpmlEventEffect(
        event = state.opmlEvent,
        snackbarHostState = snackbarHostState,
        messageFor = opmlMessageResolver(resources, importErrorMessage, exportErrorMessage),
        onConsume = viewModel::consumeOpmlEvent,
        onExportReady = { content ->
            pendingExport = content
            exportLauncher.launch(exportFilename)
        },
    )

    return remember(importLauncher) {
        OpmlController({ importLauncher.launch(OPML_IMPORT_MIME_TYPES) }, viewModel::exportOpml)
    }
}

private class ExternalOpmlImportHandler(
    val context: Context,
    val snackbarHostState: SnackbarHostState,
    val errorMessage: String,
    val onImport: (String) -> Unit,
    val onConsumed: () -> Unit,
)

@Composable
private fun ExternalOpmlImportEffect(
    uri: Uri?,
    enabled: Boolean,
    handler: ExternalOpmlImportHandler,
) {
    LaunchedEffect(uri, enabled) {
        val opmlUri = uri ?: return@LaunchedEffect
        if (!enabled) return@LaunchedEffect
        val content = readOpml(handler.context, opmlUri)
        if (content == null) {
            handler.snackbarHostState.showSnackbar(handler.errorMessage)
        } else {
            handler.onImport(content)
        }
        handler.onConsumed()
    }
}

@Composable
private fun OpmlEventEffect(
    event: OpmlEvent?,
    snackbarHostState: SnackbarHostState,
    messageFor: (OpmlEvent) -> String?,
    onConsume: () -> Unit,
    onExportReady: (String) -> Unit,
) {
    LaunchedEffect(event) {
        // Consume *after* handling: clearing the event flips this effect's key and
        // cancels the coroutine, so a pending showSnackbar() would never run.
        when (event) {
            null -> Unit
            is OpmlEvent.ExportReady -> {
                onExportReady(event.content)
                onConsume()
            }

            else -> {
                messageFor(event)?.let { snackbarHostState.showSnackbar(it) }
                onConsume()
            }
        }
    }
}

private fun importMessage(
    resources: Resources,
    event: OpmlEvent.Imported,
): String =
    if (event.imported + event.failed == 0) {
        resources.getString(R.string.settings_opml_import_empty)
    } else {
        resources.getString(R.string.settings_opml_import_result, event.imported, event.imported + event.failed)
    }

private fun opmlMessageResolver(
    resources: Resources,
    importErrorMessage: String,
    exportErrorMessage: String,
): (OpmlEvent) -> String? =
    { event ->
        when (event) {
            is OpmlEvent.Imported -> importMessage(resources, event)
            OpmlEvent.ImportFailed -> importErrorMessage
            OpmlEvent.ExportFailed -> exportErrorMessage
            is OpmlEvent.ExportReady -> null
        }
    }

/** Reads a picked OPML document off the IO dispatcher; null on any read failure. */
private suspend fun readOpml(
    context: Context,
    uri: Uri,
): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull()
    }

/** Writes the exported OPML to the picked location off the IO dispatcher; true on success. */
private suspend fun writeOpml(
    context: Context,
    uri: Uri,
    content: String,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                ?: error("No output stream")
        }.isSuccess
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
