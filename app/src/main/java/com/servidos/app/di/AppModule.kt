/* Copyright 2018 charlag
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.servidos.app.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.servidos.app.TuskyApplication
import com.servidos.app.appstore.EventHub
import com.servidos.app.appstore.EventHubImpl
import com.servidos.app.components.notifications.Notifier
import com.servidos.app.components.notifications.SystemNotifier
import com.servidos.app.db.AppDatabase
import com.servidos.app.db.Converters
import com.servidos.app.network.MastodonApi
import com.servidos.app.network.TimelineCases
import com.servidos.app.network.TimelineCasesImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by charlag on 3/21/18.
 */

@Module
class AppModule {

    @Provides
    fun providesApplication(app: TuskyApplication): Application = app

    @Provides
    fun providesContext(app: Application): Context = app

    @Provides
    fun providesSharedPreferences(app: Application): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(app)
    }

    @Provides
    fun providesBroadcastManager(app: Application): LocalBroadcastManager {
        return LocalBroadcastManager.getInstance(app)
    }

    @Provides
    fun providesTimelineUseCases(
        api: MastodonApi,
        eventHub: EventHub
    ): TimelineCases {
        return TimelineCasesImpl(api, eventHub)
    }

    @Provides
    @Singleton
    fun providesEventHub(): EventHub = EventHubImpl

    @Provides
    @Singleton
    fun providesDatabase(appContext: Context, converters: Converters): AppDatabase {
        return Room.databaseBuilder(appContext, AppDatabase::class.java, "tuskyDB")
            .addTypeConverter(converters)
            .allowMainThreadQueries()
            .addMigrations(
                AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_10_13,
                AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15, AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17, AppDatabase.MIGRATION_17_18, AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20, AppDatabase.MIGRATION_20_21, AppDatabase.MIGRATION_21_22,
                AppDatabase.MIGRATION_22_23, AppDatabase.MIGRATION_23_24, AppDatabase.MIGRATION_24_25,
                AppDatabase.MIGRATION_26_27,
                AppDatabase.Migration25_26(appContext.getExternalFilesDir("Tusky"))
            )
            .build()
    }

    @Provides
    @Singleton
    fun notifier(context: Context): Notifier = SystemNotifier(context)
}
