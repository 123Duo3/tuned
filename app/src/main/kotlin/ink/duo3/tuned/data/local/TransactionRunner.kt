package ink.duo3.tuned.data.local

import androidx.room.withTransaction

/**
 * Runs a block inside a single database transaction. Abstracted so the import
 * pipeline can guarantee atomic multi-DAO writes while staying testable with
 * fakes — Room does not run in plain JVM unit tests.
 */
interface TransactionRunner {
    suspend operator fun <R> invoke(block: suspend () -> R): R
}

class RoomTransactionRunner(
    private val database: TunedDatabase,
) : TransactionRunner {
    override suspend operator fun <R> invoke(block: suspend () -> R): R = database.withTransaction { block() }
}
