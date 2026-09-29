package com.imtiyaztour.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ============================================================================
// v2.13.0 (TAHAP 17): Form Skrining Kesehatan multi-step (8 section A-H).
// Samakan 100% dengan: https://pastiumrah.com/formulir-skrining-kesehatan-calon-jamaah-umrah/
// Draft auto-save di SharedPreferences.
// ============================================================================

private const val HIJAU = 0xFF0F7A5A
private const val DRAFT_PREF = "skrining_draft_v2"
private const val TOTAL_STEP = 7

@Composable
fun SkriningFormV2(
    jamaahId: String,
    token: String,
    onClose: () -> Unit,
    onUnauthorized: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load draft (jika ada)
    var data by remember { mutableStateOf(loadDraft(context)) }
    var step by remember { mutableStateOf(1) }
    var isSending by remember { mutableStateOf(false) }
    var sendResult by remember { mutableStateOf("") }
    var sendError by remember { mutableStateOf(false) }
    var validationMsg by remember { mutableStateOf("") }

    // Auto-save saat data berubah
    LaunchedEffect(data) { saveDraft(context, data) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress
        Text("Langkah $step / $TOTAL_STEP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(HIJAU))
        LinearProgressIndicator(progress = step / TOTAL_STEP.toFloat(), modifier = Modifier.fillMaxWidth())

        if (validationMsg.isNotEmpty()) {
            Text(validationMsg, fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
        }

        when (step) {
            1 -> StepA(data) { data = it }
            2 -> StepB(data) { data = it }
            3 -> StepC(data) { data = it }
            4 -> StepD(data) { data = it }
            5 -> StepE(data) { data = it }
            6 -> StepG(data) { data = it }
            7 -> StepH(data) { data = it }
        }

        // Preview di step terakhir
        if (step == TOTAL_STEP) {
            Spacer(Modifier.height(8.dp))
            Divider()
            Text("Ringkasan Jawaban", fontWeight = FontWeight.Bold, color = Color(HIJAU), fontSize = 13.sp)
            PreviewRingkas(data)
        }

        if (isSending) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (sendResult.isNotEmpty()) {
            Text(sendResult, fontSize = 11.sp,
                color = if (sendError) Color(0xFFDC2626) else Color(HIJAU),
                fontWeight = FontWeight.Bold)
        }

        // Tombol navigasi
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (step > 1) {
                OutlinedButton(
                    onClick = { validationMsg = ""; step-- },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Kembali") }
            }
            if (step < TOTAL_STEP) {
                Button(
                    onClick = {
                        val errs = data.validateStep(step)
                        if (errs.isNotEmpty()) {
                            validationMsg = "⚠ " + errs.joinToString(" • ")
                        } else {
                            validationMsg = ""
                            step++
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(HIJAU)),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Lanjut") }
            } else {
                Button(
                    onClick = {
                        val errs = (1..7).flatMap { data.validateStep(it) }
                        if (errs.isNotEmpty()) {
                            validationMsg = "⚠ " + errs.joinToString(" • ")
                        } else {
                            validationMsg = ""
                            isSending = true
                            sendResult = ""
                            scope.launch {
                                try {
                                    val body = data.toMap(jamaahId, token)
                                    val resp = withContext(Dispatchers.IO) {
                                        ApiClient.service.submitSkrining(body)
                                    }
                                    sendResult = resp.message ?: "Skrining terkirim"
                                    sendError = false
                                    // Hapus draft setelah sukses
                                    clearDraft(context)
                                    isSending = false
                                    kotlinx.coroutines.delay(1200)
                                    onClose()
                                } catch (e: Exception) {
                                    isSending = false
                                    sendError = true
                                    val msg = e.message ?: ""
                                    if (msg.contains("401") || msg.contains("Token", ignoreCase = true)) {
                                        onUnauthorized()
                                    } else {
                                        sendResult = "Gagal mengirim: $msg"
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSending,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(HIJAU)),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Kirim ke Admin") }
            }
        }
    }
}

// ============================================================================
// STEP A — Data Diri & Pendamping
// ============================================================================
@Composable
private fun StepA(d: SkriningData, onUpdate: (SkriningData) -> Unit) {
    SectionHeader("A. Data Diri & Pendamping")
    TextFieldField(
        label = "Email",
        value = d.email,
        onChange = { onUpdate(d.copy(email = it)) },
        keyboardType = KeyboardType.Email
    )
    TextFieldField(
        label = "Nama Lengkap",
        value = d.namaLengkap,
        onChange = { onUpdate(d.copy(namaLengkap = it)) }
    )
    TextFieldField(
        label = "Usia (tahun)",
        value = d.usia,
        onChange = { onUpdate(d.copy(usia = it.filter { c -> c.isDigit() })) },
        keyboardType = KeyboardType.Number
    )
    TextFieldField(
        label = "Nama Pendamping yang ikut berangkat",
        value = d.pendampingNama,
        onChange = { onUpdate(d.copy(pendampingNama = it)) }
    )
    TextFieldField(
        label = "No. HP Pendamping",
        value = d.pendampingHp,
        onChange = { onUpdate(d.copy(pendampingHp = it)) },
        keyboardType = KeyboardType.Phone
    )
    TextFieldField(
        label = "Nama & kontak pendamping lain (opsional, isi 'Tidak ada' jika sendiri)",
        value = d.pendampingIkut,
        onChange = { onUpdate(d.copy(pendampingIkut = it)) }
    )
}

// ============================================================================
// STEP B — Riwayat Umrah
// ============================================================================
@Composable
private fun StepB(d: SkriningData, onUpdate: (SkriningData) -> Unit) {
    SectionHeader("B. Riwayat Umrah")
    RadioGroupField(
        label = "Apakah Anda pernah berangkat umrah?",
        options = SkriningOptions.PERNAH_UMRAH,
        selected = d.pernahUmrah,
        onSelect = { onUpdate(d.copy(pernahUmrah = it)) }
    )
    if (d.pernahUmrah.startsWith("Pernah, dengan kendala")) {
        TextFieldField(
            label = "Jelaskan kendala kesehatan saat umrah sebelumnya",
            value = d.pernahUmrahKendala,
            onChange = { onUpdate(d.copy(pernahUmrahKendala = it)) },
            minLines = 3
        )
    }
}

// ============================================================================
// STEP C — Riwayat Kesehatan
// ============================================================================
@Composable
private fun StepC(d: SkriningData, onUpdate: (SkriningData) -> Unit) {
    SectionHeader("C. Riwayat Kesehatan")
    CheckboxGroupField(
        label = "Riwayat penyakit (pilih semua yang sesuai)",
        options = SkriningOptions.RIWAYAT_PENYAKIT,
        selectedList = d.riwayatPenyakit,
        onToggle = { opt, checked ->
            val newList = if (checked) {
                // Kalau pilih "Tidak ada", hapus semua yang lain
                if (opt == "Tidak ada riwayat penyakit di atas") listOf(opt)
                else (d.riwayatPenyakit - "Tidak ada riwayat penyakit di atas") + opt
            } else {
                d.riwayatPenyakit - opt
            }
            onUpdate(d.copy(riwayatPenyakit = newList))
        }
    )
    TextFieldField(
        label = "Penyakit lain yang tidak tercantum (opsional)",
        value = d.riwayatPenyakitLain,
        onChange = { onUpdate(d.copy(riwayatPenyakitLain = it)) },
        minLines = 2
    )
    TextFieldField(
        label = "Pengobatan rutin saat ini (isi '-' jika tidak ada)",
        value = d.pengobatanRutin,
        onChange = { onUpdate(d.copy(pengobatanRutin = it)) },
        minLines = 2
    )
    RadioGroupField(
        label = "Pernah dirawat inap di RS dalam 1 tahun terakhir?",
        options = SkriningOptions.DIRAWAT_RS,
        selected = d.dirawatRs,
        onSelect = { onUpdate(d.copy(dirawatRs = it)) }
    )
    if (d.dirawatRs.startsWith("Pernah")) {
        TextFieldField(
            label = "Jelaskan kondisi dirawat",
            value = d.dirawatRsKondisi,
            onChange = { onUpdate(d.copy(dirawatRsKondisi = it)) },
            minLines = 2
        )
    }
}

// ============================================================================
// STEP D — Mobilitas & Fisik
// ============================================================================
@Composable
private fun StepD(d: SkriningData, onUpdate: (SkriningData) -> Unit) {
    SectionHeader("D. Mobilitas & Fisik")
    RadioGroupField("Kemampuan berjalan mandiri",
        SkriningOptions.JALAN_MANDIRI, d.jalanMandiri) { onUpdate(d.copy(jalanMandiri = it)) }
    RadioGroupField("Durasi berjalan tanpa lelah",
        SkriningOptions.DURASI_JALAN, d.durasiJalan) { onUpdate(d.copy(durasiJalan = it)) }
    RadioGroupField("Riwayat jatuh dalam 6 bulan terakhir",
        SkriningOptions.PERNAH_JATUH, d.pernahJatuh) { onUpdate(d.copy(pernahJatuh = it)) }
    RadioGroupField("Kemampuan naik turun tangga",
        SkriningOptions.NAIK_TURUN_TANGGA, d.naikTurunTangga) { onUpdate(d.copy(naikTurunTangga = it)) }
    RadioGroupField("Kemampuan duduk/berdiri dari toilet",
        SkriningOptions.DUDUK_BERDIRI_TOILET, d.dudukBerdiriToilet) { onUpdate(d.copy(dudukBerdiriToilet = it)) }
}

// ============================================================================
// STEP E — Aktivitas Harian & Kognitif
// ============================================================================
@Composable
private fun StepE(d: SkriningData, onUpdate: (SkriningData) -> Unit) {
    SectionHeader("E. Aktivitas Harian")
    RadioGroupField("Mandi sendiri",
        SkriningOptions.MANDI_MANDIRI, d.mandiMandiri) { onUpdate(d.copy(mandiMandiri = it)) }
    RadioGroupField("Makan sendiri",
        SkriningOptions.MAKAN_MANDIRI, d.makanMandiri) { onUpdate(d.copy(makanMandiri = it)) }
    RadioGroupField("Minum obat rutin",
        SkriningOptions.BANTUAN_OBAT, d.bantuanObat) { onUpdate(d.copy(bantuanObat = it)) }

    Spacer(Modifier.height(8.dp))
    SectionHeader("F. Kognitif & Orientasi")
    RadioGroupField("Bingung di lingkungan baru",
        SkriningOptions.BINGUNG_LINGKUNGAN_BARU, d.bingungLingkunganBaru) { onUpdate(d.copy(bingungLingkunganBaru = it)) }
    RadioGroupField("Riwayat tersesat",
        SkriningOptions.PERNAH_TERSESAT, d.pernahTersesat) { onUpdate(d.copy(pernahTersesat = it)) }
    RadioGroupField("Kemampuan mengikuti instruksi",
        SkriningOptions.IKUTI_INSTRUKSI, d.ikutiInstruksi) { onUpdate(d.copy(ikutiInstruksi = it)) }
}

// ============================================================================
// STEP G — Kesiapan Ibadah
// ============================================================================
@Composable
private fun StepG(d: SkriningData, onUpdate: (SkriningData) -> Unit) {
    SectionHeader("G. Kesiapan Ibadah")
    RadioGroupField("Kesiapan thawaf",
        SkriningOptions.SANGGUP_THAWAF, d.sanggupThawaf) { onUpdate(d.copy(sanggupThawaf = it)) }
    RadioGroupField("Kesiapan sai",
        SkriningOptions.SANGGUP_SAI, d.sanggupSai) { onUpdate(d.copy(sanggupSai = it)) }
    RadioGroupField("Sesak napas saat aktivitas",
        SkriningOptions.SESAK_NAPAS_AKTIVITAS, d.sesakNapasAktivitas) { onUpdate(d.copy(sesakNapasAktivitas = it)) }
}

// ============================================================================
// STEP H — Diet, Asuransi & Persetujuan
// ============================================================================
@Composable
private fun StepH(d: SkriningData, onUpdate: (SkriningData) -> Unit) {
    SectionHeader("H. Diet & Asuransi")
    TextFieldField(
        label = "Pantangan makanan (isi '-' jika tidak ada)",
        value = d.pantanganMakanan,
        onChange = { onUpdate(d.copy(pantanganMakanan = it)) }
    )
    TextFieldField(
        label = "Diet khusus (isi '-' jika tidak ada)",
        value = d.dietKhusus,
        onChange = { onUpdate(d.copy(dietKhusus = it)) }
    )
    TextFieldField(
        label = "Obat pribadi yang dibawa (isi '-' jika tidak ada)",
        value = d.obatPribadi,
        onChange = { onUpdate(d.copy(obatPribadi = it)) },
        minLines = 2
    )
    RadioGroupField("Status asuransi kesehatan",
        SkriningOptions.ASURANSI_AKTIF, d.asuransiAktif) { onUpdate(d.copy(asuransiAktif = it)) }
    RadioGroupField("Kesediaan membuat surat keterangan sehat",
        SkriningOptions.BERSEDIA_SURAT_SEHAT, d.bersediaSuratSehat) { onUpdate(d.copy(bersediaSuratSehat = it)) }

    Spacer(Modifier.height(8.dp))
    SectionHeader("Persetujuan")
    Row(verticalAlignment = Alignment.Top) {
        Checkbox(
            checked = d.persetujuanKeluarga.isNotEmpty(),
            onCheckedChange = { checked ->
                onUpdate(d.copy(persetujuanKeluarga = if (checked) SkriningOptions.PERSETUJUAN_KELUARGA else ""))
            }
        )
        Spacer(Modifier.width(6.dp))
        Text(
            SkriningOptions.PERSETUJUAN_KELUARGA,
            fontSize = 11.sp,
            color = Color(0xFF374151),
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

// ============================================================================
// Ringkasan sebelum submit
// ============================================================================
@Composable
private fun PreviewRingkas(d: SkriningData) {
    val items = listOf(
        "Email" to d.email,
        "Nama" to d.namaLengkap,
        "Usia" to d.usia,
        "Pendamping" to "${d.pendampingNama} (${d.pendampingHp})",
        "Riwayat umrah" to d.pernahUmrah,
        "Riwayat penyakit" to d.riwayatPenyakit.joinToString(", "),
        "Pengobatan rutin" to d.pengobatanRutin,
        "Jalan mandiri" to d.jalanMandiri,
        "Durasi jalan" to d.durasiJalan,
        "Asuransi" to d.asuransiAktif,
        "Persetujuan" to (if (d.persetujuanKeluarga.isNotEmpty()) "✅ Disetujui" else "❌ Belum")
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (k, v) ->
            Row {
                Text("$k: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151), modifier = Modifier.width(110.dp))
                Text(v.ifBlank { "-" }, fontSize = 11.sp, color = Color(0xFF374151))
            }
        }
    }
}

// ============================================================================
// Komponen helper
// ============================================================================
@Composable
private fun SectionHeader(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(HIJAU))
}

@Composable
private fun TextFieldField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = (minLines == 1),
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun RadioGroupField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
        options.forEach { opt ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RadioButton(
                    selected = (selected == opt),
                    onClick = { onSelect(opt) }
                )
                Spacer(Modifier.width(4.dp))
                Text(opt, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun CheckboxGroupField(
    label: String,
    options: List<String>,
    selectedList: List<String>,
    onToggle: (String, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
        options.forEach { opt ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = selectedList.contains(opt),
                    onCheckedChange = { checked -> onToggle(opt, checked) }
                )
                Spacer(Modifier.width(4.dp))
                Text(opt, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

// ============================================================================
// Draft auto-save (SharedPreferences + Gson)
// ============================================================================
private val gsonSkrining = Gson()

private fun loadDraft(context: Context): SkriningData {
    return try {
        val prefs = context.getSharedPreferences(DRAFT_PREF, Context.MODE_PRIVATE)
        val json = prefs.getString("draft", null) ?: return SkriningData()
        gsonSkrining.fromJson(json, SkriningData::class.java) ?: SkriningData()
    } catch (e: Exception) {
        SkriningData()
    }
}

private fun saveDraft(context: Context, data: SkriningData) {
    try {
        val prefs = context.getSharedPreferences(DRAFT_PREF, Context.MODE_PRIVATE)
        prefs.edit().putString("draft", gsonSkrining.toJson(data)).apply()
    } catch (_: Exception) { }
}

private fun clearDraft(context: Context) {
    try {
        val prefs = context.getSharedPreferences(DRAFT_PREF, Context.MODE_PRIVATE)
        prefs.edit().remove("draft").apply()
    } catch (_: Exception) { }
}
