package ink.duo3.tuned.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ink.duo3.tuned.R
import ink.duo3.tuned.core.AppError

/** User-facing copy for repository failures surfaced by feature screens. */
@Composable
fun appErrorMessage(error: AppError): String =
    when (error) {
        is AppError.Network -> stringResource(R.string.error_network)
        is AppError.Http -> stringResource(R.string.error_http, error.code)
        is AppError.NotFound -> stringResource(R.string.error_not_found)
        is AppError.Parsing -> stringResource(R.string.error_parsing)
        is AppError.InvalidUrl -> stringResource(R.string.error_invalid_url)
        is AppError.Storage -> stringResource(R.string.error_storage)
        is AppError.Unknown -> stringResource(R.string.error_unknown)
    }
