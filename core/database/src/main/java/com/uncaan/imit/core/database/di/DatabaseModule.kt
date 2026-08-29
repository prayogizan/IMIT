package com.uncaan.imit.core.database.di

import androidx.room3.Room
import com.uncaan.imit.core.database.MitOcwDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            MitOcwDatabase::class.java,
            "mit_ocw_database"
        ).build()
    }

    single { get<MitOcwDatabase>().videoCacheDao() }
    single { get<MitOcwDatabase>().downloadedVideoDao() }
}
