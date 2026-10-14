package com.imtiyaztour.app

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// ============================================================================
// v2.15.0: GELOMBANG RADIO
// TL buat kode gelombang, jamaah join via kode. Berlaku 24 jam.
// ============================================================================

data class GelombangCreateResp(
    val kode: String? = null,
    val nama: String? = null,
    val expires_at: Long? = null,
    val tl_nama: String? = null,
    val error: String? = null
)

data class GelombangJoinResp(
    val kode: String? = null,
    val nama: String? = null,
    val tl_nama: String? = null,
    val is_tl: Boolean? = null,
    val jumlah_peserta: Int? = null,
    val error: String? = null
)

data class GelombangInfoResp(
    val kode: String? = null,
    val nama: String? = null,
    val tl_nama: String? = null,
    val jumlah_peserta: Int? = null,
    val expires_at: Long? = null,
    val error: String? = null
)

data class GelombangSimpleResp(
    val success: Boolean? = null,
    val error: String? = null,
    val message: String? = null
)

interface GelombangApiService {
    @POST("api/radio/gelombang/create")
    suspend fun create(@Body body: Map<String, String>): GelombangCreateResp

    @POST("api/radio/gelombang/join")
    suspend fun join(@Body body: Map<String, String>): GelombangJoinResp

    @POST("api/radio/gelombang/leave")
    suspend fun leave(@Body body: Map<String, String>): GelombangSimpleResp

    @POST("api/radio/gelombang/revoke")
    suspend fun revoke(@Body body: Map<String, String>): GelombangSimpleResp

    @GET("api/radio/gelombang/info/{kode}")
    suspend fun info(@Path("kode") kode: String): GelombangInfoResp
}

object GelombangApiClient {
    val service: GelombangApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GelombangApiService::class.java)
    }
}
