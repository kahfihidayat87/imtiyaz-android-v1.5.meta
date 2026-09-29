package com.imtiyaztour.app

// ============================================================================
// v2.13.0 (TAHAP 17): Model data Skrining Kesehatan
// Disamakan 100% dengan form web:
// https://pastiumrah.com/formulir-skrining-kesehatan-calon-jamaah-umrah/
// 8 Section (A-H), 32 pertanyaan, semua wajib.
// ============================================================================

data class SkriningData(
    // A. Data Diri & Pendamping
    val email: String = "",
    val namaLengkap: String = "",
    val usia: String = "",
    val pendampingNama: String = "",
    val pendampingHp: String = "",
    val pendampingIkut: String = "",

    // B. Riwayat Umrah
    val pernahUmrah: String = "",
    val pernahUmrahKendala: String = "",

    // C. Riwayat Kesehatan
    val riwayatPenyakit: List<String> = emptyList(),
    val riwayatPenyakitLain: String = "",
    val pengobatanRutin: String = "",
    val dirawatRs: String = "",
    val dirawatRsKondisi: String = "",

    // D. Mobilitas & Fisik
    val jalanMandiri: String = "",
    val durasiJalan: String = "",
    val pernahJatuh: String = "",
    val naikTurunTangga: String = "",
    val dudukBerdiriToilet: String = "",

    // E. Aktivitas Harian
    val mandiMandiri: String = "",
    val makanMandiri: String = "",
    val bantuanObat: String = "",

    // F. Kognitif & Orientasi
    val bingungLingkunganBaru: String = "",
    val pernahTersesat: String = "",
    val ikutiInstruksi: String = "",

    // G. Kesiapan Ibadah
    val sanggupThawaf: String = "",
    val sanggupSai: String = "",
    val sesakNapasAktivitas: String = "",

    // H. Diet, Asuransi & Persetujuan
    val pantanganMakanan: String = "",
    val dietKhusus: String = "",
    val obatPribadi: String = "",
    val asuransiAktif: String = "",
    val bersediaSuratSehat: String = "",
    val persetujuanKeluarga: String = "",
) {
    /**
     * Validasi step saat ini. Return list error (kosong = valid).
     */
    fun validateStep(step: Int): List<String> {
        val errors = mutableListOf<String>()
        when (step) {
            1 -> {
                if (email.isBlank() || !email.contains("@")) errors.add("Email tidak valid")
                if (namaLengkap.isBlank()) errors.add("Nama lengkap wajib")
                if (usia.isBlank() || usia.toIntOrNull() !in 1..120) errors.add("Usia harus 1-120")
                if (pendampingNama.isBlank()) errors.add("Nama pendamping wajib")
                if (pendampingHp.isBlank()) errors.add("No. HP pendamping wajib")
            }
            2 -> {
                if (pernahUmrah.isBlank()) errors.add("Riwayat umrah wajib dipilih")
            }
            3 -> {
                if (riwayatPenyakit.isEmpty()) errors.add("Pilih minimal 1 riwayat penyakit (atau 'Tidak ada')")
                if (pengobatanRutin.isBlank()) errors.add("Pengobatan rutin wajib diisi")
                if (dirawatRs.isBlank()) errors.add("Riwayat dirawat RS wajib dipilih")
            }
            4 -> {
                if (jalanMandiri.isBlank()) errors.add("Kemampuan jalan wajib dipilih")
                if (durasiJalan.isBlank()) errors.add("Durasi jalan wajib dipilih")
                if (pernahJatuh.isBlank()) errors.add("Riwayat jatuh wajib dipilih")
                if (naikTurunTangga.isBlank()) errors.add("Kemampuan naik tangga wajib dipilih")
                if (dudukBerdiriToilet.isBlank()) errors.add("Kemampuan toilet wajib dipilih")
            }
            5 -> {
                if (mandiMandiri.isBlank()) errors.add("Kemandirian mandi wajib dipilih")
                if (makanMandiri.isBlank()) errors.add("Kemandirian makan wajib dipilih")
                if (bantuanObat.isBlank()) errors.add("Bantuan obat wajib dipilih")
                if (bingungLingkunganBaru.isBlank()) errors.add("Orientasi lingkungan wajib dipilih")
                if (pernahTersesat.isBlank()) errors.add("Riwayat tersesat wajib dipilih")
                if (ikutiInstruksi.isBlank()) errors.add("Kemampuan ikuti instruksi wajib dipilih")
            }
            6 -> {
                if (sanggupThawaf.isBlank()) errors.add("Kesiapan thawaf wajib dipilih")
                if (sanggupSai.isBlank()) errors.add("Kesiapan sai wajib dipilih")
                if (sesakNapasAktivitas.isBlank()) errors.add("Sesak napas wajib dipilih")
                if (pantanganMakanan.isBlank()) errors.add("Pantangan makanan wajib diisi (isi '-' jika tidak ada)")
                if (dietKhusus.isBlank()) errors.add("Diet khusus wajib diisi (isi '-' jika tidak ada)")
                if (obatPribadi.isBlank()) errors.add("Obat pribadi wajib diisi (isi '-' jika tidak ada)")
            }
            7 -> {
                if (asuransiAktif.isBlank()) errors.add("Status asuransi wajib dipilih")
                if (bersediaSuratSehat.isBlank()) errors.add("Kesediaan surat sehat wajib dipilih")
                if (persetujuanKeluarga.isBlank()) errors.add("Persetujuan keluarga wajib dicentang")
            }
        }
        return errors
    }

    /**
     * Konversi ke Map untuk dikirim ke server (mirip struktur POST form web).
     */
    fun toMap(jamaahId: String, token: String): Map<String, Any> = mapOf(
        "jamaah_id" to jamaahId,
        "token" to token,
        "email" to email.trim(),
        "nama_lengkap" to namaLengkap.trim(),
        "usia" to (usia.toIntOrNull() ?: 0),
        "pendamping_nama" to pendampingNama.trim(),
        "pendamping_hp" to pendampingHp.trim(),
        "pendamping_ikut" to pendampingIkut.trim(),
        "pernah_umrah" to pernahUmrah,
        "pernah_umrah_kendala" to pernahUmrahKendala.trim(),
        "riwayat_penyakit" to riwayatPenyakit,
        "riwayat_penyakit_lain" to riwayatPenyakitLain.trim(),
        "pengobatan_rutin" to pengobatanRutin.trim(),
        "dirawat_rs" to dirawatRs,
        "dirawat_rs_kondisi" to dirawatRsKondisi.trim(),
        "jalan_mandiri" to jalanMandiri,
        "durasi_jalan" to durasiJalan,
        "pernah_jatuh" to pernahJatuh,
        "naik_turun_tangga" to naikTurunTangga,
        "duduk_berdiri_toilet" to dudukBerdiriToilet,
        "mandi_mandiri" to mandiMandiri,
        "makan_mandiri" to makanMandiri,
        "bantuan_obat" to bantuanObat,
        "bingung_lingkungan_baru" to bingungLingkunganBaru,
        "pernah_tersesat" to pernahTersesat,
        "ikuti_instruksi" to ikutiInstruksi,
        "sanggup_thawaf" to sanggupThawaf,
        "sanggup_sai" to sanggupSai,
        "sesak_napas_aktivitas" to sesakNapasAktivitas,
        "pantangan_makanan" to pantanganMakanan.trim(),
        "diet_khusus" to dietKhusus.trim(),
        "obat_pribadi" to obatPribadi.trim(),
        "asuransi_aktif" to asuransiAktif,
        "bersedia_surat_sehat" to bersediaSuratSehat,
        "persetujuan_keluarga" to persetujuanKeluarga,
    )
}

// ============================================================================
// Opsi jawaban per field (samakan dengan form web)
// ============================================================================
object SkriningOptions {
    val PERNAH_UMRAH = listOf(
        "Belum pernah",
        "Pernah, tanpa kendala kesehatan",
        "Pernah, dengan kendala kesehatan (jelaskan di pertanyaan berikutnya)"
    )

    val RIWAYAT_PENYAKIT = listOf(
        "Hipertensi (darah tinggi)",
        "Diabetes / kencing manis",
        "Penyakit jantung (termasuk pernah operasi/pasang ring)",
        "Stroke",
        "Penyakit paru (asma, PPOK, sesak napas)",
        "Penyakit ginjal (termasuk cuci darah/dialisis)",
        "Gangguan sendi/tulang (osteoporosis, arthritis, pernah patah tulang)",
        "Gangguan pendengaran/penglihatan signifikan",
        "Demensia / gangguan memori",
        "Tidak ada riwayat penyakit di atas"
    )

    val DIRAWAT_RS = listOf(
        "Tidak pernah",
        "Pernah (jelaskan di pertanyaan berikutnya)"
    )

    val JALAN_MANDIRI = listOf(
        "Ya, sepenuhnya mandiri",
        "Bisa, tapi perlu pendampingan/pegangan",
        "Tidak, perlu alat bantu (tongkat/walker)",
        "Tidak bisa berjalan, perlu kursi roda"
    )

    val DURASI_JALAN = listOf(
        "Lebih dari 15 menit",
        "5-15 menit",
        "Kurang dari 5 menit"
    )

    val PERNAH_JATUH = listOf(
        "Tidak pernah",
        "Pernah 1 kali",
        "Pernah 2 kali atau lebih"
    )

    val NAIK_TURUN_TANGGA = listOf(
        "Ya, mampu sendiri",
        "Mampu, tapi perlu pegangan/bantuan",
        "Tidak mampu"
    )

    val DUDUK_BERDIRI_TOILET = listOf(
        "Ya, mandiri",
        "Perlu bantuan ringan",
        "Perlu bantuan penuh"
    )

    val MANDI_MANDIRI = listOf(
        "Ya, sepenuhnya mandiri",
        "Perlu bantuan sebagian",
        "Perlu bantuan penuh"
    )

    val MAKAN_MANDIRI = listOf(
        "Ya, mandiri",
        "Perlu bantuan"
    )

    val BANTUAN_OBAT = listOf(
        "Tidak perlu, bisa mandiri",
        "Perlu diingatkan",
        "Perlu dibantu penuh"
    )

    val BINGUNG_LINGKUNGAN_BARU = listOf(
        "Tidak",
        "Kadang-kadang",
        "Sering"
    )

    val PERNAH_TERSESAT = listOf(
        "Tidak pernah",
        "Pernah 1 kali",
        "Pernah lebih dari 1 kali"
    )

    val IKUTI_INSTRUKSI = listOf(
        "Ya, dengan baik",
        "Kadang perlu diulang",
        "Sering tidak paham instruksi"
    )

    val SANGGUP_THAWAF = listOf(
        "Sanggup berjalan penuh",
        "Sanggup sebagian, sisanya perlu kursi roda",
        "Perlu kursi roda sepenuhnya"
    )

    val SANGGUP_SAI = listOf(
        "Sanggup berjalan penuh",
        "Sanggup sebagian, sisanya perlu kursi roda",
        "Perlu kursi roda sepenuhnya"
    )

    val SESAK_NAPAS_AKTIVITAS = listOf(
        "Tidak pernah",
        "Kadang-kadang",
        "Sering"
    )

    val ASURANSI_AKTIF = listOf(
        "Ya, BPJS aktif",
        "Ya, asuransi swasta aktif",
        "Keduanya aktif",
        "Tidak ada yang aktif"
    )

    val BERSEDIA_SURAT_SEHAT = listOf(
        "Ya, bersedia",
        "Sudah punya surat keterangan sehat",
        "Belum bersedia / perlu diskusi lebih lanjut"
    )

    const val PERSETUJUAN_KELUARGA = "Ya, kami setujui dan tanggung jawab penuh atas kebenaran data ini"
}
