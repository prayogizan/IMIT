package com.uncaan.imit.core.network.di

import com.uncaan.imit.core.data.repository.VideoRepository
import com.uncaan.imit.core.database.dao.VideoCacheDao
import com.uncaan.imit.core.network.api.ArchiveApiService
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

class DataModuleTest : KoinTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `verify dataModule binds VideoRepository`() {
        val testModule = module {
            single<ArchiveApiService> { mockk() }
            single<VideoCacheDao> { mockk() }
        }

        val koinApp = startKoin {
            modules(testModule, dataModule)
        }

        assertNotNull(koinApp)
        val repository = get<VideoRepository>()
        assertNotNull(repository)
    }
}
