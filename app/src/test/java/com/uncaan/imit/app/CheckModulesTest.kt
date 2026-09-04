package com.uncaan.imit.app

import android.content.Context
import com.uncaan.imit.core.data.repository.VideoRepository
import com.uncaan.imit.core.database.dao.DownloadedVideoDao
import com.uncaan.imit.core.database.dao.VideoCacheDao
import com.uncaan.imit.core.download.DownloadManagerHelper
import com.uncaan.imit.core.network.api.ArchiveApiService
import com.uncaan.imit.core.network.di.dataModule
import com.uncaan.imit.core.network.di.networkModule
import com.uncaan.imit.core.player.di.playerModule
import com.uncaan.imit.feature.catalog.di.catalogViewModelModule
import com.uncaan.imit.feature.details.di.detailsViewModelModule
import com.uncaan.imit.feature.downloads.di.downloadsViewModelModule
import org.junit.After
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.verify.verify

class CheckModulesTest : KoinTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `verify all Koin modules dependency graph`() {
        networkModule.verify(
            extraTypes = listOf(
                Context::class
            )
        )
        dataModule.verify(
            extraTypes = listOf(
                ArchiveApiService::class,
                VideoCacheDao::class
            )
        )
        playerModule.verify(
            extraTypes = listOf(
                Context::class
            )
        )
        catalogViewModelModule.verify(
            extraTypes = listOf(
                VideoRepository::class
            )
        )
        detailsViewModelModule.verify(
            extraTypes = listOf(
                VideoRepository::class,
                DownloadedVideoDao::class,
                String::class
            )
        )
        downloadsViewModelModule.verify(
            extraTypes = listOf(
                DownloadedVideoDao::class,
                DownloadManagerHelper::class
            )
        )
    }
}
