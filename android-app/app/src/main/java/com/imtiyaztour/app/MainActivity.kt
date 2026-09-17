@file:OptIn(ExperimentalMaterial3Api::class)

package com.imtiyaztour.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.FileOutputStream

// ============================================================================
// DATA MODELS
// ============================================================================
data class PaketUmrah(val id: String, val nama: String, val kategori: String, val durasi: String, val harga: String, val fasilitas: String, val badge: String = "", val kode_radio: String = "")
data class Dokumen(val id: String, val nama: String, val deskripsi: String, var checked: Boolean = false)
data class Doa(val id: String, val judul: String, val arab: String, val latin: String, val arti: String)

data class KontakInfo(val nama_travel: String = "Imtiyaz Tour Jogja", val alamat: String = "PPIU U383/2021 • Jln. Pertapan, Tegal Cerme RT08, Baturetno, Banguntapan, Bantul", val kontak: String = "0811-277-6543 • pastiumrah.com")

// FIX: sebelumnya paket, dokumen, dan kontak ini hardcode DAN dipakai langsung oleh
// layar Beranda/Paket/Dokumen/Saya — jadi walau admin sudah bisa mengubahnya lewat
// WP Admin, perubahan itu TIDAK PERNAH tampil di aplikasi. Sekarang nilai di bawah
// ini hanya jadi fallback offline; nilai aktif diambil dari AppData yang di-fetch
// dari server saat app dibuka (lihat LaunchedEffect di ImtiyazApp).
val defaultPaket = listOf(
    PaketUmrah("slamet", "Paket Slamet", "Ekonomis", "9 Hari", "Rp 28,9 jt", "Bus Jogja-Jakarta, Hotel *3 250m/750m, 45 Pax, Scoot/AirAsia"),
    PaketUmrah("ayem", "Paket Ayem Tentrem", "Hemat", "9 Hari", "Rp 29,9 jt", "Pesawat YIA-CGK, Hotel *3, 35-45 Pax, Oman Air/Lion Air"),
    PaketUmrah("linuwih", "Paket Linuwih", "Reguler", "9 Hari", "Rp 37,4 jt", "Garuda/Saudia Langsung, Hotel Bintang 4 ±250m, Max 25 Jamaah, Grup 55+ Eksklusif", "Paling Diminati"),
    PaketUmrah("kamulyan", "Paket Kamulyan", "Nyaman Lansia", "9 Hari", "Rp 41,9 jt", "Garuda/Saudia Langsung, Hotel Bintang 5 ±250m, Max 20, Kursi Roda & Pendamping", "Premium"),
    PaketUmrah("plus", "Paket Plus", "Plus Wisata", "13 Hari", "Rp 40,9 jt", "Umrah Plus Mesir/Turki (Kairo-Alexandria), Hotel *4/*5, 13 Hari")
)

val defaultDokumen = listOf(
    Dokumen("ktp", "KTP", "Kartu Tanda Penduduk asli & fotokopi"),
    Dokumen("kk", "Kartu Keluarga", "KK asli & fotokopi"),
    Dokumen("paspor", "Paspor", "Masa berlaku min 12 bulan"),
    Dokumen("vaksin", "Vaksin Meningitis", "Sertifikat vaksin meningitis"),
    Dokumen("foto", "Foto 4x6", "Background putih, 4 lembar"),
    Dokumen("nikah", "Buku Nikah", "Jika berangkat pasangan")
)

// Wadah state global untuk konten yang dikelola admin. Diisi sekali saat app dibuka
// (ImtiyazApp -> LaunchedEffect), dipakai oleh semua layar yang butuh. Kalau fetch
// gagal (offline/server down), tetap pakai nilai default di atas -- app tidak crash.
data class WaInfo(val wa_admin: String = "628112776543", val wa_link: String = "https://wa.me/628112776543")

object AppData {
    var paket by mutableStateOf(defaultPaket)
    var dokumen by mutableStateOf(defaultDokumen)
    var kontak by mutableStateOf(KontakInfo())
    var wa by mutableStateOf(WaInfo())
    // FIX: sebelumnya tidak ada cara sama sekali untuk memasukkan audio ke doa --
    // sekarang admin bisa isi URL mp3 per-doa lewat WP Admin (tab "Audio Doa" di
    // Pengaturan Aplikasi), diambil di sini, tanpa perlu update aplikasi.
    var doaAudio by mutableStateOf<Map<String, String>>(emptyMap())
}

val listDoa = listOf(
    Doa("keluar_rumah", "Doa Keluar Rumah", "بِسْمِ اللَّهِ تَوَكَّلْتُ عَلَى اللَّهِ وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "Bismillaahi tawakkaltu 'alallaah, wa laa haula wa laa quwwata illaa billaah", "Dengan nama Allah, aku bertawakal kepada Allah, tiada daya dan kekuatan kecuali dengan pertolongan Allah"),
    Doa("naik_kendaraan", "Doa Naik Kendaraan/Pesawat", "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَى رَبِّنَا لَمُنْقَلِبُونَ", "Subhaanalladzii sakhkhara lanaa hadzaa wa maa kunnaa lahu muqriniin, wa innaa ilaa rabbinaa lamunqalibuun", "Maha Suci Allah yang telah menundukkan kendaraan ini untuk kami, padahal kami sebelumnya tidak mampu menguasainya, dan sesungguhnya kami akan kembali kepada Tuhan kami (QS. Az-Zukhruf: 13-14)"),
    Doa("safar", "Doa Safar (Bepergian Jauh)", "اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَذَا الْبِرَّ وَالتَّقْوَى، وَمِنَ الْعَمَلِ مَا تَرْضَى، اللَّهُمَّ هَوِّنْ عَلَيْنَا سَفَرَنَا هَذَا وَاطْوِ عَنَّا بُعْدَهُ", "Allaahumma innaa nas-aluka fii safarinaa haadzal birra wat-taqwaa, wa minal 'amali maa tardhaa, Allaahumma hawwin 'alainaa safaranaa haadzaa wathwi 'annaa bu'dah", "Ya Allah, kami memohon kepada-Mu dalam perjalanan kami ini kebajikan dan ketakwaan, dan amal yang Engkau ridhai. Ya Allah, mudahkanlah perjalanan kami ini dan dekatkanlah jaraknya yang jauh (HR. Muslim)"),
    Doa("talbiyah", "Talbiyah", "لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ، إِنَّ الْحَمْدَ وَالنِّعْمَةَ لَكَ وَالْمُلْكَ، لَا شَرِيكَ لَكَ", "Labbaikallaahumma labbaik, labbaika laa syariika laka labbaik, innal hamda wan-ni'mata laka wal mulk, laa syariika lak", "Aku penuhi panggilan-Mu ya Allah, aku penuhi panggilan-Mu. Aku penuhi panggilan-Mu, tiada sekutu bagi-Mu, aku penuhi panggilan-Mu. Sesungguhnya segala puji, nikmat, dan kerajaan adalah milik-Mu, tiada sekutu bagi-Mu. Dibaca berulang-ulang sejak niat ihram hingga melihat Ka'bah"),
    Doa("niat", "Niat Umrah", "نَوَيْتُ الْعُمْرَةَ وَأَحْرَمْتُ بِهَا لِلَّهِ تَعَالَى", "Nawaitul 'umrata wa ahramtu bihaa lillaahi ta'aalaa", "Aku niat umrah dan berihram karena Allah Ta'ala"),
    Doa("masuk_masjid", "Doa Memasuki Masjidil Haram", "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ", "Allaahummaftah lii abwaaba rahmatik", "Ya Allah, bukakanlah untukku pintu-pintu rahmat-Mu (HR. Muslim, dibaca sambil melangkah kaki kanan masuk masjid)"),
    Doa("lihat_kabah", "Doa Melihat Ka'bah", "اللَّهُمَّ زِدْ هَذَا الْبَيْتَ تَشْرِيفًا وَتَعْظِيمًا وَتَكْرِيمًا وَمَهَابَةً، وَزِدْ مَنْ شَرَّفَهُ وَكَرَّمَهُ مِمَّنْ حَجَّهُ أَوِ اعْتَمَرَهُ تَشْرِيفًا وَتَكْرِيمًا وَتَعْظِيمًا وَبِرًّا", "Allaahumma zid haadzal baita tasyriifan wa ta'zhiiman wa takriiman wa mahaabah, wa zid man syarrafahu wa karramahu mimman hajjahu awi'tamarahu tasyriifan wa takriiman wa ta'zhiiman wa birraa", "Ya Allah, tambahkanlah kemuliaan, keagungan, kehormatan, dan kewibawaan bagi Baitullah ini, dan tambahkanlah pula bagi orang yang memuliakannya di antara mereka yang berhaji atau berumrah kemuliaan, kehormatan, keagungan, dan kebaikan"),
    Doa("mulai_tawaf", "Doa Memulai Tawaf", "بِسْمِ اللَّهِ، اللَّهُ أَكْبَرُ", "Bismillaah, Allaahu akbar", "Dengan nama Allah, Allah Maha Besar — diucapkan setiap kali sejajar dengan Hajar Aswad di awal setiap putaran tawaf (boleh disertai mengecup/mengisyaratkan tangan ke arah Hajar Aswad)"),
    Doa("rukun_yamani", "Doa di Antara Rukun Yamani-Hajar Aswad", "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "Rabbanaa aatinaa fid-dunyaa hasanah wa fil-aakhirati hasanah wa qinaa 'adzaaban-naar", "Ya Tuhan kami, berilah kami kebaikan di dunia dan akhirat, dan peliharalah kami dari siksa neraka — dibaca sepanjang jarak antara Rukun Yamani dan Hajar Aswad di setiap putaran tawaf (QS. Al-Baqarah: 201)"),
    Doa("sai", "Doa Sa'i (Naik ke Shafa/Marwah)", "إِنَّ الصَّفَا وَالْمَرْوَةَ مِنْ شَعَائِرِ اللَّهِ", "Innash-shafaa wal-marwata min sya'aa-irillaah", "Sesungguhnya Shafa dan Marwah adalah sebagian dari syi'ar Allah (QS. Al-Baqarah: 158), dibaca saat pertama kali mendekati bukit Shafa sebelum memulai sa'i"),
    Doa("tahallul", "Doa Tahallul (Mencukur Rambut)", "اللَّهُمَّ اغْفِرْ لِلْمُحَلِّقِينَ وَالْمُقَصِّرِينَ", "Allaahummaghfir lil-muhalliqiina wal-muqashshiriin", "Ya Allah, ampunilah orang-orang yang mencukur habis rambutnya dan orang-orang yang memendekkannya (HR. Bukhari-Muslim) — dibaca setelah selesai mencukur/memotong rambut sebagai penutup rangkaian umrah"),
    Doa("zamzam", "Doa Minum Air Zamzam", "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا وَاسِعًا، وَشِفَاءً مِنْ كُلِّ دَاءٍ", "Allaahumma innii as-aluka 'ilman naafi'an, wa rizqan waasi'an, wa syifaa-an min kulli daa'", "Ya Allah, sesungguhnya aku memohon kepada-Mu ilmu yang bermanfaat, rezeki yang luas, dan kesembuhan dari segala penyakit — air zamzam diminum sesuai niat, doa ini salah satu yang masyhur diamalkan"),
    Doa("ziarah", "Doa Ziarah Madinah", "السَّلَامُ عَلَيْكَ يَا رَسُولَ اللَّهِ", "As-salaamu 'alaika yaa Rasuulallaah", "Salam sejahtera atasmu wahai Rasulullah — diucapkan pelan saat ziarah ke makam Nabi Muhammad ﷺ di Masjid Nabawi"),
    Doa("harian", "Doa Sehari-hari", "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا", "Allaahumma innii as-aluka 'ilman naafi'an", "Ya Allah, sesungguhnya aku memohon ilmu yang bermanfaat")
)

// ============================================================================
// NETWORKING
// Sebelumnya TIDAK ADA sama sekali di file asli — app.js/plugin PHP tidak pernah
// dipanggil oleh Android app. Base URL mengikuti dokumentasi di dashboard plugin
// WordPress ("api.pastiumrah.com"). GANTI sesuai domain aktual server Node.js Anda.
// ============================================================================
object ApiConfig {
    const val BASE_URL = "https://api.pastiumrah.com/" // TODO: sesuaikan domain produksi
}

data class UploadBuktiResponse(val success: Boolean? = null, val bukti_url: String? = null, val status: String? = null, val message: String? = null, val error: String? = null)
data class ChecklistResponse(val success: Boolean? = null, val message: String? = null, val error: String? = null)
data class SkriningResponse(val success: Boolean? = null, val id: Any? = null, val message: String? = null, val error: String? = null)
data class ChecklistRequest(val jamaah_id: String, val token: String, val checklist: Map<String, Boolean>)
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val success: Boolean? = null, val jamaah_id: Int? = null, val nama: String? = null, val token: String? = null, val error: String? = null)
data class LogoutRequest(val jamaah_id: String, val token: String)
data class MeRequest(val jamaah_id: String, val token: String)
data class JamaahProfile(
    val id: Int? = null,
    val nama: String? = null,
    val paket_id: String? = null,
    val total_tagihan: String? = null,
    val sudah_dibayar: String? = null,
    val sisa_tagihan: String? = null,
    val status_pembayaran: String? = null,
    val bukti_transfer: String? = null,
    val checklist_dokumen: Map<String, Boolean>? = null
)

/** Format angka mentah dari server ("37400000") jadi "Rp 37.400.000". Tanpa dependency locale. */
fun formatRupiah(raw: String?): String {
    val n = raw?.toLongOrNull()
    if (n == null) return "Belum diisi Admin"
    val digits = n.toString()
    val grouped = StringBuilder()
    for ((i, c) in digits.reversed().withIndex()) {
        if (i > 0 && i % 3 == 0) grouped.append('.')
        grouped.append(c)
    }
    return "Rp " + grouped.reverse().toString()
}

interface ApiService {
    // FIX: sebelumnya tidak ada fungsi fetch untuk paket/dokumen/kontak sama sekali,
    // jadi walau admin edit lewat WP, Android tidak pernah tahu.
    @GET("api/paket")
    suspend fun getPaket(): List<PaketUmrah>

    @GET("api/dokumen")
    suspend fun getDokumen(): Map<String, String>

    @GET("api/kontak")
    suspend fun getKontak(): KontakInfo

    @GET("api/wa-admin")
    suspend fun getWaAdmin(): WaInfo

    @GET("api/doa-audio")
    suspend fun getDoaAudio(): Map<String, String>

    // FIX: sebelumnya TIDAK ADA login sama sekali. Username & password dibuat Admin
    // lewat WP Admin (lihat imtiyaz-connector.php), bukan didaftarkan sendiri oleh jamaah.
    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/logout")
    suspend fun logout(@Body body: LogoutRequest): Map<String, Boolean>

    @POST("api/me")
    suspend fun getMe(@Body body: MeRequest): JamaahProfile

    @Multipart
    @POST("api/upload-bukti")
    suspend fun uploadBukti(
        @Part("jamaah_id") jamaahId: okhttp3.RequestBody,
        @Part("token") token: okhttp3.RequestBody,
        @Part bukti: MultipartBody.Part
    ): UploadBuktiResponse

    @POST("api/update-checklist")
    suspend fun updateChecklist(@Body body: ChecklistRequest): ChecklistResponse

    @POST("api/skrining")
    suspend fun submitSkrining(@Body body: Map<String, String>): SkriningResponse
}

object ApiClient {
    val service: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// ============================================================================
// PENYIMPANAN LOKAL SEDERHANA (SharedPreferences)
// Sebelumnya checklist & data skrining hilang setiap kali layar berpindah
// karena hanya disimpan di `remember { mutableStateOf(...) }`.
// FIX: sebelumnya ID Jamaah diisi bebas oleh siapa saja tanpa password -- sekarang
// yang disimpan adalah hasil LOGIN (jamaah_id + token dari server), bukan input bebas.
// ============================================================================
object Prefs {
    private const val NAME = "imtiyaz_prefs"
    fun get(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getJamaahId(context: Context): String = get(context).getString("jamaah_id", "") ?: ""
    fun getToken(context: Context): String = get(context).getString("jamaah_token", "") ?: ""
    fun getNama(context: Context): String = get(context).getString("jamaah_nama", "") ?: ""
    fun isLoggedIn(context: Context): Boolean = getJamaahId(context).isNotBlank() && getToken(context).isNotBlank()

    fun saveLogin(context: Context, jamaahId: String, token: String, nama: String) {
        get(context).edit()
            .putString("jamaah_id", jamaahId)
            .putString("jamaah_token", token)
            .putString("jamaah_nama", nama)
            .apply()
    }
    fun clearLogin(context: Context) {
        get(context).edit().remove("jamaah_id").remove("jamaah_token").remove("jamaah_nama").apply()
    }

    fun getChecklist(context: Context, dokumenList: List<Dokumen>): MutableMap<String, Boolean> {
        val prefs = get(context)
        return dokumenList.associate { it.id to prefs.getBoolean("doc_${it.id}", false) }.toMutableMap()
    }
    fun setChecklistItem(context: Context, key: String, value: Boolean) =
        get(context).edit().putBoolean("doc_$key", value).apply()
}

// Menyalin konten Uri (misal dari galeri) ke file cache sementara agar bisa di-upload sebagai Multipart
fun uriToTempFile(context: Context, uri: Uri): File {
    val dir = File(context.cacheDir, "bukti").apply { mkdirs() }
    val outFile = File(dir, "bukti_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(outFile).use { output -> input.copyTo(output) }
    }
    return outFile
}

fun createCameraImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "bukti").apply { mkdirs() }
    val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

suspend fun uploadBuktiFile(context: Context, jamaahId: String, token: String, file: File): Result<UploadBuktiResponse> = withContext(Dispatchers.IO) {
    try {
        val idBody = jamaahId.toRequestBody("text/plain".toMediaTypeOrNull())
        val tokenBody = token.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("bukti", file.name, reqFile)
        val response = ApiClient.service.uploadBukti(idBody, tokenBody, part)
        Result.success(response)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tema splash (logo di windowBackground) hanya untuk menutup jeda cold-start.
        // Begitu Activity ini hidup, langsung kembali ke tema biasa -- splash Compose
        // di bawah (SplashScreen composable) yang mengatur durasi tampil logo sesungguhnya.
        setTheme(android.R.style.Theme_Material_Light_NoActionBar)
        setContent { ImtiyazApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImtiyazApp() {
    var showSplash by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedPaket by remember { mutableStateOf<PaketUmrah?>(null) }
    var selectedDoa by remember { mutableStateOf<Doa?>(null) }
    var selectedSurah by remember { mutableStateOf<Int?>(null) }
    var showItinerary by remember { mutableStateOf(false) }
    var showRadio by remember { mutableStateOf(false) }
    var showJadwal by remember { mutableStateOf(false) }
    var showPembimbingList by remember { mutableStateOf(false) }
    var selectedPembimbing by remember { mutableStateOf<Pembimbing?>(null) }
    val context = LocalContext.current

    // FIX (bug): sebelumnya TIDAK ADA satu pun layar di aplikasi ini yang menangani
    // tombol/gestur back sistem Android -- menekannya di layar detail/sub-halaman
    // manapun (Detail Paket, Detail Doa, Surat Quran, Itinerary, Radio, Jadwal
    // Keberangkatan) langsung MENUTUP SELURUH APLIKASI, karena tidak ada back-stack
    // yang di-pop (defaultnya keluar dari Activity). Sekarang back sistem mundur satu
    // langkah dulu (balik ke Beranda/tab sebelumnya), sama seperti tombol "← Kembali"
    // yang sudah ada di tiap layar -- baru di level Beranda/tab, back sistem berlaku
    // seperti biasa (keluar aplikasi), sesuai perilaku standar Android.
    androidx.activity.compose.BackHandler(
        enabled = selectedPaket != null || selectedDoa != null || selectedSurah != null || showItinerary || showRadio || showJadwal || showPembimbingList || selectedPembimbing != null
    ) {
        when {
            selectedPaket != null -> selectedPaket = null
            selectedDoa != null -> selectedDoa = null
            selectedSurah != null -> selectedSurah = null
            selectedPembimbing != null -> selectedPembimbing = null
            showItinerary -> showItinerary = false
            showRadio -> showRadio = false
            showJadwal -> showJadwal = false
            showPembimbingList -> showPembimbingList = false
        }
    }

    // Izin lokasi diminta SEKALI di awal (sebelum masuk ke halaman utama), bukan
    // lagi ditunda sampai user membuka fitur jadwal shalat -- sesuai permintaan.
    // Catatan: Android tidak mengizinkan permission dangerous (termasuk lokasi)
    // diberikan saat proses instal APK -- ini sudah tidak berlaku sejak Android 6.0
    // dan berlaku untuk semua aplikasi, bukan keterbatasan aplikasi ini. Yang bisa
    // dilakukan (dan sudah diterapkan di sini) adalah meminta izin itu paling awal
    // saat aplikasi pertama kali dibuka, sebelum layar utama muncul.
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(Unit) {
        val hasPermission = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            locationPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // Ambil konten yang dikelola admin (paket, dokumen wajib, kontak, audio doa) sekali
    // saat app dibuka. Kalau gagal (offline/server down), AppData tetap berisi nilai
    // default offline -- tidak melempar error ke pengguna, cukup diam-diam pakai fallback.
    LaunchedEffect(Unit) {
        launch { try { AppData.paket = ApiClient.service.getPaket() } catch (e: Exception) { /* pakai defaultPaket */ } }
        launch {
            try {
                val map = ApiClient.service.getDokumen()
                AppData.dokumen = map.map { (key, label) -> Dokumen(key, label, "") }
            } catch (e: Exception) { /* pakai defaultDokumen */ }
        }
        launch { try { AppData.kontak = ApiClient.service.getKontak() } catch (e: Exception) { /* pakai KontakInfo() default */ } }
        launch { try { AppData.wa = ApiClient.service.getWaAdmin() } catch (e: Exception) { /* pakai WaInfo() default */ } }
        launch { try { AppData.doaAudio = ApiClient.service.getDoaAudio() } catch (e: Exception) { /* tidak ada audio, tampilkan teks saja */ } }
    }

    // Splash Compose -- durasi tampil logo dikontrol pasti (bukan cuma jeda cold-start
    // sekilas dari tema Android). Lihat juga windowBackground di AndroidManifest untuk
    // splash native yang menutup jeda sebelum Compose sempat menggambar frame pertama.
    LaunchedEffect(Unit) {
        delay(1400)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Imtiyaz Tour", fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A)); Text("PPIU U383/2021 • Travel Umrah Nyaman Lansia", fontSize = 11.sp, color = Color.Gray) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0; selectedPaket = null; selectedDoa = null; selectedSurah = null }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Beranda", fontSize = 9.sp) })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1; selectedPaket = null; selectedDoa = null; selectedSurah = null }, icon = { Icon(Icons.Default.List, null) }, label = { Text("Paket", fontSize = 9.sp) })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2; selectedPaket = null; selectedDoa = null; selectedSurah = null }, icon = { Icon(Icons.Default.MenuBook, null) }, label = { Text("Quran", fontSize = 9.sp) })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3; selectedPaket = null; selectedDoa = null; selectedSurah = null }, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Doa", fontSize = 9.sp) })
                NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4; selectedPaket = null; selectedDoa = null; selectedSurah = null }, icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Dokumen", fontSize = 9.sp) })
                NavigationBarItem(selected = selectedTab == 5, onClick = { selectedTab = 5; selectedPaket = null; selectedDoa = null; selectedSurah = null }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Saya", fontSize = 9.sp) })
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                selectedPaket != null -> DetailPaketScreen(paket = selectedPaket!!, onBack = { selectedPaket = null })
                selectedDoa != null -> DetailDoaScreen(doa = selectedDoa!!, onBack = { selectedDoa = null })
                selectedSurah != null -> SurahDetailScreen(nomor = selectedSurah!!, onBack = { selectedSurah = null })
                showItinerary -> ItineraryScreen(onBack = { showItinerary = false })
                showRadio -> RadioScreen(onBack = { showRadio = false })
                showJadwal -> JadwalKeberangkatanScreen(onBack = { showJadwal = false })
                selectedPembimbing != null -> PembimbingDetailScreen(pembimbing = selectedPembimbing!!, onBack = { selectedPembimbing = null })
                showPembimbingList -> PembimbingListScreen(onPembimbingClick = { selectedPembimbing = it }, onBack = { showPembimbingList = false })
                else -> when (selectedTab) {
                    0 -> BerandaScreen(onPaketClick = { selectedPaket = it }, onItineraryClick = { showItinerary = true }, onJadwalClick = { showJadwal = true }, onRadioClick = { showRadio = true }, onPembimbingClick = { showPembimbingList = true })
                    1 -> PaketListScreen(onPaketClick = { selectedPaket = it })
                    2 -> QuranScreen(onSurahClick = { selectedSurah = it })
                    3 -> DoaListScreen(onDoaClick = { selectedDoa = it })
                    4 -> DokumenScreen()
                    5 -> SayaScreen(onRadioClick = { showRadio = true })
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "Imtiyaz Tour & Travel",
                modifier = Modifier.size(220.dp)
            )
        }
    }
}

@Composable
fun BerandaScreen(onPaketClick: (PaketUmrah) -> Unit, onItineraryClick: () -> Unit, onJadwalClick: () -> Unit, onRadioClick: () -> Unit, onPembimbingClick: () -> Unit) {
    val context = LocalContext.current
    val namaJamaah = Prefs.getNama(context)
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    // FIX: sebelumnya selalu menampilkan nama travel meski jamaah sudah login --
                    // sekarang tampil nama jamaah yang sedang login (mis. "Yasir Ismail"),
                    // dan baru jatuh kembali ke nama travel kalau belum login.
                    Text("Assalamualaikum,", color = Color(0xFFD1FAE5), fontSize = 14.sp)
                    Text(
                        namaJamaah.ifBlank { AppData.kontak.nama_travel },
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp
                    )
                    Text("Pilih Paket Umrah", color = Color(0xFFFFD700), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            PrayerTimesCard()
            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth().clickable { onItineraryClick() }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF0F7A5A))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Itinerary Umrah 9 Hari", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Ramadhan & Reguler -- jadwal harian umum", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF0F7A5A))
                }
            }
            Card(
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth().clickable { onJadwalClick() }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF0F7A5A))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Jadwal Keberangkatan Terbaru", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Tanggal & harga live dari sistem", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF0F7A5A))
                }
            }
            Spacer(Modifier.height(16.dp))
            // FIX: sebelumnya Radio hanya bisa diakses lewat tombol di tab Saya, yang
            // mensyaratkan login JAMAAH lebih dulu -- Tour Leader yang tidak punya akun
            // jamaah jadi TIDAK BISA sama sekali mencapai layar Radio. Sekarang ada
            // jalur langsung dari Beranda, terbuka untuk siapa saja (login jamaah ATAU
            // login TL diminta di dalam layar Radio itu sendiri, bukan sebelum masuk).
            Card(
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth().clickable { onRadioClick() }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF0F7A5A))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Radio Tour Leader", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Dengarkan arahan TL secara langsung, atau masuk sebagai TL", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF0F7A5A))
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth().clickable { onPembimbingClick() }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0F7A5A))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Pembimbing Umrah", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Profil ustadz & materi ceramah singkat", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF0F7A5A))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Pilih Paket Umrah", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("${AppData.paket.size} pilihan, profesional & amanah", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
        }
        items(AppData.paket) { paket -> PaketCard(paket = paket, onClick = { onPaketClick(paket) }) }
    }
}

@Composable
fun PaketListScreen(onPaketClick: (PaketUmrah) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Pilih Paket Umrah", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("Sesuaikan dengan kebutuhan", fontSize = 11.sp, color = Color.Gray); Spacer(Modifier.height(8.dp)) }
        items(AppData.paket) { paket -> PaketCard(paket = paket, onClick = { onPaketClick(paket) }) }
    }
}

@Composable
fun PaketCard(paket: PaketUmrah, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) { Text(paket.nama, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(paket.kategori + " • " + paket.durasi, fontSize = 12.sp, color = Color.Gray) }
                if (paket.badge.isNotEmpty()) { Badge(containerColor = if (paket.badge == "Premium") Color(0xFFFFD700) else Color(0xFF0F7A5A)) { Text(paket.badge, fontSize = 10.sp, color = if (paket.badge == "Premium") Color.Black else Color.White) } }
            }
            Spacer(Modifier.height(8.dp))
            Text(paket.harga, fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A), fontSize = 14.sp)
            Text(paket.fasilitas, fontSize = 11.sp, color = Color(0xFF374151), maxLines = 2)
        }
    }
}

@Composable
fun DetailPaketScreen(paket: PaketUmrah, onBack: () -> Unit) {
    val context = LocalContext.current
    var nama by remember { mutableStateOf("") }
    var hp by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("← Kembali") }
            Spacer(Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(20.dp)) {
                    if (paket.badge.isNotEmpty()) { Badge { Text(paket.badge) }; Spacer(Modifier.height(8.dp)) }
                    Text(paket.nama, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(paket.kategori + " • " + paket.durasi, fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    Text(paket.harga, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF0F7A5A))
                    Divider(Modifier.padding(vertical = 12.dp))
                    Text("Fasilitas:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(paket.fasilitas, fontSize = 13.sp, color = Color(0xFF374151))
                    Divider(Modifier.padding(vertical = 16.dp))
                    Text("Form Pendaftaran", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F7A5A))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = hp, onValueChange = { hp = it }, label = { Text("No HP / WA") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val pesan = "Assalamualaikum, saya mau daftar ${paket.nama} ${paket.harga} - Nama: $nama - HP: $hp"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${AppData.wa.wa_admin}?text=${Uri.encode(pesan)}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Daftar via WhatsApp", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp))
                    Text("Akan membuka WhatsApp ke Admin ${AppData.wa.wa_admin}", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

// FITUR 3 - sekarang tersimpan permanen (SharedPreferences) dan disinkron ke server
// jika ID Jamaah sudah diisi di tab "Saya". Sebelumnya reset setiap pindah layar
// dan tidak pernah memanggil /api/update-checklist.
// FIX: sebelumnya checklist ini bisa diedit LANGSUNG oleh jamaah di HP-nya sendiri
// (tersimpan di SharedPreferences lokal) SEKALIGUS oleh Admin di WP Admin -- dua
// sumber yang saling menimpa, itulah sebabnya data di Admin dan di aplikasi tidak
// pernah sinkron. Sekarang status dokumen HANYA diisi oleh Admin (dokumen fisik kan
// memang diperiksa admin/petugas, bukan diakui sendiri oleh jamaah); aplikasi cuma
// menampilkan keterangan "Lengkap/Tidak/Belum Diperiksa" per dokumen -- dan karena
// ini data pribadi jamaah, layar ini sekarang digerbang login juga.
@Composable
fun DokumenScreen() {
    val context = LocalContext.current

    if (!Prefs.isLoggedIn(context)) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Checklist Dokumen hanya bisa diakses setelah login", fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Silakan login lewat tab Saya terlebih dahulu.", fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        return
    }

    val jamaahId = Prefs.getJamaahId(context)
    val token = Prefs.getToken(context)
    var profile by remember { mutableStateOf<JamaahProfile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(jamaahId) {
        loading = true; errorMsg = ""
        try {
            profile = withContext(Dispatchers.IO) { ApiClient.service.getMe(MeRequest(jamaahId, token)) }
        } catch (e: Exception) {
            errorMsg = "Gagal memuat status dokumen -- periksa koneksi internet"
        }
        loading = false
    }

    val checklist = profile?.checklist_dokumen ?: emptyMap()
    val totalDokumen = AppData.dokumen.size
    val lengkapCount = AppData.dokumen.count { checklist[it.id] == true }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Checklist Dokumen", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Diperiksa & diperbarui oleh Admin -- bukan diisi sendiri", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else if (errorMsg.isNotEmpty()) {
            Text(errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626))
        } else {
            val fraction = if (totalDokumen > 0) lengkapCount / totalDokumen.toFloat() else 0f
            LinearProgressIndicator(progress = fraction, modifier = Modifier.fillMaxWidth().height(8.dp).padding(horizontal = 4.dp), color = Color(0xFF0F7A5A))
            Spacer(Modifier.height(4.dp))
            Text("$lengkapCount / $totalDokumen lengkap - ${(fraction*100).toInt()}%", fontSize = 11.sp, color = Color(0xFF0F7A5A), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AppData.dokumen) { doc ->
                    val status = checklist[doc.id] // null = belum diperiksa, true = lengkap, false = tidak lengkap
                    val (label, labelColor) = when (status) {
                        true -> "Lengkap" to Color(0xFF0F7A5A)
                        false -> "Tidak Lengkap" to Color(0xFFDC2626)
                        else -> "Belum Diperiksa" to Color.Gray
                    }
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(doc.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(doc.deskripsi, fontSize = 11.sp, color = Color.Gray) }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.background(labelColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoaListScreen(onDoaClick: (Doa) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Panduan Doa", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Bisa dibaca tanpa internet", fontSize = 12.sp, color = Color.Gray)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Text("✅ Offline Mode - Tanpa internet di pesawat / Masjidil Haram", fontSize = 10.sp, color = Color(0xFF0F7A5A), modifier = Modifier.padding(8.dp)) }
            Spacer(Modifier.height(8.dp))
        }
        items(listDoa) { doa ->
            val hasAudio = AppData.doaAudio.containsKey(doa.id)
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth().clickable { onDoaClick(doa) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(doa.judul, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (hasAudio) { Spacer(Modifier.width(6.dp)); Icon(Icons.Default.VolumeUp, contentDescription = "Ada audio", tint = Color(0xFF0F7A5A), modifier = Modifier.size(14.dp)) }
                        }
                        Text(doa.latin.take(40) + "...", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF0F7A5A))
                }
            }
        }
    }
}

@Composable
fun DetailDoaScreen(doa: Doa, onBack: () -> Unit) {
    val audioUrl = AppData.doaAudio[doa.id]
    val playing = AudioPlayerManager.currentlyPlayingUrl == audioUrl
    DisposableEffect(Unit) { onDispose { AudioPlayerManager.stop() } }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("← Kembali") }
            Spacer(Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(doa.judul, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF0F7A5A), modifier = Modifier.weight(1f))
                        if (audioUrl != null) {
                            IconButton(onClick = { AudioPlayerManager.toggle(audioUrl) }, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    if (playing) Icons.Default.Stop else Icons.Default.PlayCircle,
                                    contentDescription = "Putar audio",
                                    tint = Color(0xFF0F7A5A),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    if (AudioPlayerManager.isLoading && playing) { LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp)) }
                    AudioPlayerManager.errorMessage?.let { if (playing || AudioPlayerManager.currentlyPlayingUrl == null) Text(it, fontSize = 10.sp, color = Color(0xFFDC2626)) }
                    Divider(Modifier.padding(vertical = 12.dp))
                    Text("Arab:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Text(doa.arab, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(Modifier.height(16.dp))
                    Text("Latin:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Text(doa.latin, fontSize = 14.sp, color = Color(0xFF374151))
                    Spacer(Modifier.height(16.dp))
                    Text("Arti:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Text(doa.arti, fontSize = 13.sp, color = Color(0xFF0F7A5A))
                }
            }
        }
    }
}

// FIX KEAMANAN: sebelumnya kolom "ID Jamaah" adalah teks bebas tanpa password --
// siapa pun yang tahu/menebak nomor jamaah bisa upload bukti/checklist/skrining atas
// nama orang lain. Sekarang jamaah WAJIB login dengan username+password yang dibuat
// Admin (lihat imtiyaz-connector.php) sebelum bisa mengakses fitur-fitur ini.
@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onCancel: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        // FIX (bug): sebelumnya layar ini sama sekali tidak punya tombol keluar --
        // satu-satunya cara "keluar" adalah pindah tab lain (tidak jelas/tidak terlihat)
        // atau menutup aplikasi. Sekarang ada tombol "← Kembali" eksplisit kalau layar
        // ini dibuka dari alur yang punya tujuan "kembali" yang jelas (mis. dari Radio).
        if (onCancel != null) {
            TextButton(onClick = onCancel) { Text("← Kembali") }
            Spacer(Modifier.height(8.dp))
        }
        Text("Login Jamaah", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF0F7A5A))
        Text("Username & password diberikan oleh Admin Imtiyaz Tour saat pendaftaran -- bukan dibuat sendiri.", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
        )
        Spacer(Modifier.height(16.dp))
        if (errorMsg.isNotEmpty()) { Text(errorMsg, fontSize = 12.sp, color = Color(0xFFDC2626)); Spacer(Modifier.height(8.dp)) }
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) { errorMsg = "Username dan password wajib diisi"; return@Button }
                isLoading = true; errorMsg = ""
                scope.launch {
                    try {
                        val resp = withContext(Dispatchers.IO) { ApiClient.service.login(LoginRequest(username.trim(), password)) }
                        if (resp.success == true && resp.token != null && resp.jamaah_id != null) {
                            Prefs.saveLogin(context, resp.jamaah_id.toString(), resp.token, resp.nama ?: "")
                            onLoggedIn()
                        } else {
                            errorMsg = resp.error ?: "Username atau password salah"
                        }
                    } catch (e: Exception) {
                        errorMsg = "Username atau password salah, atau tidak ada koneksi internet"
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
            shape = RoundedCornerShape(12.dp)
        ) { if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Masuk", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        Text("Belum punya akun? Hubungi Admin Imtiyaz Tour via tab Paket untuk mendaftar dan mendapatkan username/password.", fontSize = 10.sp, color = Color.Gray)
    }
}

// FITUR 2 - tombol Galeri/Kamera sekarang benar-benar meng-upload ke /api/upload-bukti.
// Sebelumnya onClick = {} (kosong total).
@Composable
fun SayaScreen(onRadioClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loggedIn by remember { mutableStateOf(Prefs.isLoggedIn(context)) }

    if (!loggedIn) {
        LoginScreen(onLoggedIn = { loggedIn = true })
        return
    }

    val jamaahId = Prefs.getJamaahId(context)
    val token = Prefs.getToken(context)
    val nama = Prefs.getNama(context)

    var uploadStatus by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var showSkrining by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // FIX: sebelumnya angka-angka ini hardcode ("Rp 37.400.000", "Paket Linuwih", dst)
    // -- tidak pernah diganti data jamaah yang benar-benar login, jadi terlihat salah/
    // tidak sesuai. Sekarang diambil dari server lewat endpoint /api/me yang memverifikasi
    // token, bukan sekadar dipercaya dari input jamaah_id.
    var profile by remember { mutableStateOf<JamaahProfile?>(null) }
    var profileLoading by remember { mutableStateOf(true) }
    var profileError by remember { mutableStateOf("") }

    fun handleUnauthorized(message: String?): Boolean {
        // Kalau server bilang token tidak valid (kedaluwarsa / dipakai di perangkat lain
        // setelah login ulang), paksa logout supaya jamaah login ulang -- jangan biarkan
        // app tetap "kelihatan login" padahal sesinya sudah tidak berlaku di server.
        if (message?.contains("Token login", ignoreCase = true) == true) {
            Prefs.clearLogin(context); loggedIn = false; return true
        }
        return false
    }

    suspend fun loadProfile() {
        profileLoading = true; profileError = ""
        try {
            profile = withContext(Dispatchers.IO) { ApiClient.service.getMe(MeRequest(jamaahId, token)) }
        } catch (e: Exception) {
            if (!handleUnauthorized(e.message)) profileError = "Gagal memuat data akun -- periksa koneksi internet"
        }
        profileLoading = false
    }
    LaunchedEffect(jamaahId) { loadProfile() }

    fun doUpload(file: File) {
        isUploading = true
        scope.launch {
            val result = uploadBuktiFile(context, jamaahId, token, file)
            isUploading = false
            result.fold(
                onSuccess = { uploadStatus = it.message ?: "Berhasil diupload"; loadProfile() },
                onFailure = { if (!handleUnauthorized(it.message)) uploadStatus = "Gagal upload: ${it.message}" }
            )
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) doUpload(uriToTempFile(context, uri))
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraUri != null) {
            // salin dari content:// FileProvider uri ke File nyata untuk di-upload
            doUpload(uriToTempFile(context, cameraUri!!))
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createCameraImageUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            uploadStatus = "Izin kamera ditolak"
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Saya", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    if (nama.isNotBlank()) Text(nama, fontSize = 12.sp, color = Color.Gray)
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        try { withContext(Dispatchers.IO) { ApiClient.service.logout(LogoutRequest(jamaahId, token)) } } catch (e: Exception) {}
                        Prefs.clearLogin(context); loggedIn = false
                    }
                }) { Text("Keluar", fontSize = 12.sp) }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Status Pembayaran", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    if (profileLoading) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else if (profileError.isNotEmpty()) {
                        Text(profileError, fontSize = 12.sp, color = Color(0xFFDC2626))
                    } else if (profile == null || profile?.paket_id.isNullOrBlank()) {
                        Text(
                            "Data pendaftaran Anda belum diisi Admin. Hubungi Admin Imtiyaz Tour untuk memastikan pendaftaran Anda tercatat.",
                            fontSize = 12.sp, color = Color(0xFF374151)
                        )
                    } else {
                        val p = profile!!
                        val paketName = AppData.paket.find { it.id == p.paket_id }?.nama ?: p.paket_id ?: "-"
                        val statusText = p.status_pembayaran?.takeIf { it.isNotBlank() } ?: "Belum Lunas"
                        Text("Paket: $paketName", fontSize = 11.sp, color = Color.Gray)
                        Text("Total: ${formatRupiah(p.total_tagihan)}", fontSize = 13.sp)
                        Text("Sudah Dibayar: ${formatRupiah(p.sudah_dibayar)}", fontSize = 13.sp)
                        Text("Sisa: ${formatRupiah(p.sisa_tagihan)}", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 14.sp)
                        Text("Status: $statusText", fontWeight = FontWeight.Bold, color = if (statusText == "Lunas") Color(0xFF0F7A5A) else Color(0xFFDC2626), fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            enabled = !isUploading,
                            modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)
                        ) { Text("Galeri", fontSize = 12.sp) }
                        Button(
                            onClick = {
                                val granted = context.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    val uri = createCameraImageUri(context)
                                    cameraUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            enabled = !isUploading,
                            modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)
                        ) { Text("Kamera", fontSize = 12.sp) }
                    }
                    if (isUploading) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    if (uploadStatus.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text(uploadStatus, fontSize = 11.sp, color = Color(0xFF0F7A5A)) }
                    Text("Upload bukti transfer - akan diverifikasi admin", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Skrining Kesehatan Lansia", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("29 Pertanyaan A-H - Wajib untuk Kamulyan & Linuwih", fontSize = 11.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    if (!showSkrining) {
                        Button(onClick = { showSkrining = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(12.dp)) { Text("Mulai Skrining - 29 Pertanyaan") }
                    } else {
                        SkriningForm(jamaahId = jamaahId, token = token, onClose = { showSkrining = false }, onUnauthorized = { handleUnauthorized("Token login") })
                    }
                }
            }
        }

        item {
            // Radio Tour Leader (walkie-talkie) -- hanya muncul setelah login, sesuai
            // permintaan; SayaScreen ini sendiri sudah menjadi gerbang login.
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Radio Tour Leader", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Dengarkan arahan Tour Leader secara langsung selama prosesi umrah", fontSize = 11.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRadioClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(12.dp)) { Text("Buka Radio") }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(AppData.kontak.nama_travel, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F7A5A))
                    Text(AppData.kontak.alamat, fontSize = 11.sp, color = Color.Gray)
                    Text("Kontak: ${AppData.kontak.kontak}", fontSize = 11.sp, color = Color(0xFF0F7A5A))
                }
            }
        }
    }
}

// FITUR 8 - tombol "Kirim ke Admin" sekarang benar-benar POST ke /api/skrining, disertai
// token login jamaah (sebelumnya hanya jamaah_id polos tanpa bukti kepemilikan token).
@Composable
fun SkriningForm(jamaahId: String, token: String, onClose: () -> Unit, onUnauthorized: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(1) }
    var nama by remember { mutableStateOf("") }
    var penyakit by remember { mutableStateOf("") }
    var obat by remember { mutableStateOf("") }
    var alergi by remember { mutableStateOf("") }
    var kontakDarurat by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var sendResult by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Langkah $step / 4", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F7A5A))
        LinearProgressIndicator(progress = step / 4f, modifier = Modifier.fillMaxWidth())
        when (step) {
            1 -> {
                Text("A. Data Diri & Kontak Darurat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = kontakDarurat, onValueChange = { kontakDarurat = it }, label = { Text("Kontak Darurat (Nama & HP)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            2 -> {
                Text("B. Riwayat Penyakit & Obat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(value = penyakit, onValueChange = { penyakit = it }, label = { Text("Riwayat Penyakit (Hipertensi, Diabetes, Jantung, dll)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = obat, onValueChange = { obat = it }, label = { Text("Obat Rutin") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
            3 -> {
                Text("C. Alergi & Mobilitas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(value = alergi, onValueChange = { alergi = it }, label = { Text("Alergi Obat/Makanan") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                Text("Apakah butuh kursi roda? Bisa jalan berapa meter?", fontSize = 11.sp, color = Color.Gray)
            }
            4 -> {
                Text("D. Konfirmasi & Kirim", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Nama: $nama\nPenyakit: $penyakit\nObat: $obat\nAlergi: $alergi\nDarurat: $kontakDarurat", fontSize = 11.sp, color = Color(0xFF374151))
                Text("Data akan dikirim ke Admin untuk asesmen medis pra-berangkat (Linuwih & Kamulyan wajib)", fontSize = 10.sp, color = Color.Gray)
                if (isSending) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                if (sendResult.isNotEmpty()) { Text(sendResult, fontSize = 11.sp, color = Color(0xFF0F7A5A)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (step > 1) { OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text("Kembali") } }
            if (step < 4) {
                Button(onClick = { step++ }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A))) { Text("Lanjut") }
            } else {
                Button(
                    onClick = {
                        isSending = true
                        scope.launch {
                            try {
                                val body = mapOf(
                                    "jamaah_id" to jamaahId,
                                    "token" to token,
                                    "nama_lengkap" to nama,
                                    "kontak_darurat" to kontakDarurat,
                                    "riwayat_penyakit" to penyakit,
                                    "obat_rutin" to obat,
                                    "alergi" to alergi
                                )
                                val resp = withContext(Dispatchers.IO) { ApiClient.service.submitSkrining(body) }
                                sendResult = resp.message ?: "Terkirim"
                                isSending = false
                                onClose()
                            } catch (e: Exception) {
                                isSending = false
                                val msg = e.message ?: ""
                                if (msg.contains("401") || msg.contains("Token", ignoreCase = true)) { onUnauthorized() }
                                else sendResult = "Gagal mengirim: $msg"
                            }
                        }
                    },
                    enabled = !isSending,
                    modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A))
                ) { Text("Kirim ke Admin") }
            }
        }
    }
}
