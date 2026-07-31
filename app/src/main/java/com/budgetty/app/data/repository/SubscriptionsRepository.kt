package com.budgetty.app.data.repository

import com.budgetty.app.data.local.IgnoredSubscriptionEntity
import com.budgetty.app.data.local.UserDatabaseManager
import kotlinx.coroutines.flow.Flow

/** The user's dismissed subscription merchants (detection still finds them; they just stay hidden). */
class SubscriptionsRepository(private val db: UserDatabaseManager) {

    private val dao get() = db.database.ignoredSubscriptionDao()

    val ignored: Flow<List<IgnoredSubscriptionEntity>> =
        db.flow { it.ignoredSubscriptionDao().getAll() }

    suspend fun ignore(merchant: String, atMillis: Long) =
        dao.ignore(IgnoredSubscriptionEntity(merchant, atMillis))

    suspend fun restore(merchant: String) = dao.restore(merchant)
}
