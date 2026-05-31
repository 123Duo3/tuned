package ink.duo3.tuned.core

/**
 * App-wide error taxonomy. Repositories map low-level failures (IO, parsing,
 * HTTP) into these so feature/ViewModel code never sees Ktor/Room exceptions.
 */
sealed interface AppError {
    val cause: Throwable?

    data class Network(
        override val cause: Throwable? = null,
    ) : AppError

    data class Http(
        val code: Int,
        override val cause: Throwable? = null,
    ) : AppError

    data class NotFound(
        override val cause: Throwable? = null,
    ) : AppError

    data class Parsing(
        override val cause: Throwable? = null,
    ) : AppError

    data class Unknown(
        override val cause: Throwable? = null,
    ) : AppError
}
