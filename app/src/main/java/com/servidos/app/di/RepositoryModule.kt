package com.servidos.app.di

import com.google.gson.Gson
import com.servidos.app.components.timeline.TimelineRepository
import com.servidos.app.components.timeline.TimelineRepositoryImpl
import com.servidos.app.db.AccountManager
import com.servidos.app.db.AppDatabase
import com.servidos.app.network.MastodonApi
import dagger.Module
import dagger.Provides

@Module
class RepositoryModule {
    @Provides
    fun providesTimelineRepository(
        db: AppDatabase,
        mastodonApi: MastodonApi,
        accountManager: AccountManager,
        gson: Gson
    ): TimelineRepository {
        return TimelineRepositoryImpl(db.timelineDao(), mastodonApi, accountManager, gson)
    }
}
