package com.uncaan.imit.core.network.api

import com.uncaan.imit.core.network.model.ArchiveMetadataResponseDto
import com.uncaan.imit.core.network.model.ArchiveSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ArchiveApiService {

    @GET("advancedsearch.php")
    suspend fun searchMitOcwCollection(
        @Query("q") query: String = "collection:mit_ocw AND mediatype:movies",
        @Query("fl[]") fields: List<String> = listOf(
            "identifier", "title", "description",
            "creator", "year", "publicdate", "downloads"
        ),
        @Query("sort[]") sort: String = "publicdate desc",
        @Query("rows") rows: Int = 20,
        @Query("page") page: Int = 1,
        @Query("output") output: String = "json"
    ): ArchiveSearchResponseDto

    @GET("metadata/{identifier}")
    suspend fun getItemMetadata(
        @Path("identifier") identifier: String
    ): ArchiveMetadataResponseDto
}
