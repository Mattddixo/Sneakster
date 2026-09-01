package com.mattdixon.snakeapi

import org.jetbrains.exposed.sql.Database

/**
 * One shared in-memory H2 database for every repository test class, not one per class.
 *
 * Exposed pins a TransactionManager to whichever thread first runs a transaction against it,
 * and `DatabaseFactory.dbQuery` hops onto `Dispatchers.IO`'s shared worker pool — so a second
 * test class connecting to its *own* differently-named H2 database can still have its
 * `dbQuery` calls land on an IO thread that was already pinned to the *first* class's database
 * (same JVM, same thread pool), silently querying the wrong, schema-less database. Using one
 * shared database for all repository tests removes the ambiguity entirely: there's only ever
 * one to pin to. Each test class still owns and resets only its own table(s).
 */
object TestDatabase {
    init {
        Database.connect("jdbc:h2:mem:sneakster-backend-test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    }

    /** No-op call site to force this object's init block to run before a test class's own. */
    fun ensureConnected() = Unit
}
