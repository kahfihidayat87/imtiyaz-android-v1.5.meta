import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface QuranApi {
    @GET("surat")
    suspend fun getListSurah(): Response<SurahResponse>
}