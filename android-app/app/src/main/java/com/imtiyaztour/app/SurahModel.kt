data class SurahResponse(
    val code: Int,
    val message: String,
    val data: List<SurahItem>
)

data class SurahItem(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val tempatTurun: String,
    val arti: String
)