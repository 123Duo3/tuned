package ink.duo3.tuned.core

/**
 * Domain result type used as a repository return value instead of kotlin.Result,
 * so the error channel is the typed [AppError] rather than a bare Throwable.
 */
sealed interface Outcome<out T> {
    data class Success<T>(
        val value: T,
    ) : Outcome<T>

    data class Failure(
        val error: AppError,
    ) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> =
    when (this) {
        is Outcome.Success -> Outcome.Success(transform(value))
        is Outcome.Failure -> this
    }

inline fun <T> Outcome<T>.getOrElse(fallback: (AppError) -> T): T =
    when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> fallback(error)
    }
