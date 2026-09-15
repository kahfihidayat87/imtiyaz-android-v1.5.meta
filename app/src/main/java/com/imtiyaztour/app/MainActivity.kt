
package com.imtiyaztour.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// DATA - 5 PAKET NATIVE TANPA WEBVIEW
data class PaketUmrah(val id: String, val nama: String, val kategori: String, val durasi: String, val harga: String, val fasilitas: String, val badge: String = "")
data class Dokumen(val id: String, val nama: String, val deskripsi: String, var checked: Boolean = false)
data class Doa(val id: String, val judul: String, val arab: String, val latin: String, val arti: String)

val listPaket = listOf(
    PaketUmrah("slamet", "Paket Slamet", "Ekonomis", "9 Hari", "Rp 28,9 jt", "Bus Jogja-Jakarta, Hotel *3 250m/750m, 45 Pax, Scoot/AirAsia"),
    PaketUmrah("ayem", "Paket Ayem Tentrem", "Hemat", "9 Hari", "Rp 29,9 jt", "Pesawat YIA-CGK, Hotel *3, 35-45 Pax, Oman Air/Lion Air"),
    PaketUmrah("linuwih", "Paket Linuwih", "Reguler", "9 Hari", "Rp 37,4 jt", "Garuda/Saudia Langsung, Hotel Bintang 4 ±250m, Max 25 Jamaah, Grup 55+ Eksklusif", "Paling Diminati"),
    PaketUmrah("kamulyan", "Paket Kamulyan", "Nyaman Lansia", "9 Hari", "Rp 41,9 jt", "Garuda/Saudia Langsung, Hotel Bintang 5 ±250m, Max 20, Kursi Roda & Pendamping", "Premium"),
    PaketUmrah("plus", "Paket Plus", "Plus Wisata", "13 Hari", "Rp 40,9 jt", "Umrah Plus Mesir/Turki (Kairo-Alexandria), Hotel *4/*5, 13 Hari")
)

val listDokumen = listOf(
    Dokumen("ktp", "KTP", "Kartu Tanda Penduduk asli & fotokopi"),
    Dokumen("kk", "Kartu Keluarga", "KK asli & fotokopi"),
    Dokumen("paspor", "Paspor", "Masa berlaku min 12 bulan"),
    Dokumen("vaksin", "Vaksin Meningitis", "Sertifikat vaksin meningitis"),
    Dokumen("foto", "Foto 4x6", "Background putih, 4 lembar"),
    Dokumen("nikah", "Buku Nikah", "Jika berangkat pasangan")
)

val listDoa = listOf(
    Doa("niat", "Niat Umrah", "نَوَيْتُ الْعُمْرَةَ وَأَحْرَمْتُ بِهَا لِلَّهِ تَعَالَى", "Nawaitul 'umrata wa ahramtu bihaa lillaahi ta'aalaa", "Aku niat umrah dan berihram karena Allah Ta'ala"),
    Doa("tawaf", "Doa Tawaf", "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "Rabbanaa aatinaa fid-dunyaa hasanah wa fil-aakhirati hasanah wa qinaa 'adzaaban-naar", "Ya Tuhan kami, berilah kami kebaikan di dunia dan akhirat, dan peliharalah kami dari siksa neraka"),
    Doa("sai", "Doa Sa'i", "إِنَّ الصَّفَا وَالْمَرْوَةَ مِنْ شَعَائِرِ اللَّهِ", "Innas-shafaa wal-marwata min sya'aa-irillaah", "Sesungguhnya Shafa dan Marwah adalah sebagian dari syi'ar Allah"),
    Doa("arafah", "Doa di Arafah", "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ", "Laa ilaaha illallaahu wahdahu laa syariika lah", "Tidak ada Tuhan selain Allah semata, tidak ada sekutu bagi-Nya"),
    Doa("ziarah", "Doa Ziarah Madinah", "السَّلَامُ عَلَيْكَ يَا رَسُولَ اللَّهِ", "As-salaamu 'alaika yaa Rasuulallaah", "Salam sejahtera atasmu wahai Rasulullah"),
    Doa("harian", "Doa Sehari-hari", "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا", "Allaahumma innii as-aluka 'ilman naafi'an", "Ya Allah, sesungguhnya aku memohon ilmu yang bermanfaat")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ImtiyazApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImtiyazApp() {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedPaket by remember { mutableStateOf<PaketUmrah?>(null) }
    var selectedDoa by remember { mutableStateOf<Doa?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Imtiyaz Tour", fontWeight = FontWeight.Bold, color = Color(0xFF0F7A5A)); Text("PPIU U383/2021 • Pilih Paket Umrah", fontSize = 11.sp, color = Color.Gray) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0; selectedPaket = null; selectedDoa = null }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Beranda", fontSize = 9.sp) })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1; selectedPaket = null; selectedDoa = null }, icon = { Icon(Icons.Default.List, null) }, label = { Text("Paket", fontSize = 9.sp) })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2; selectedPaket = null; selectedDoa = null }, icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Dokumen", fontSize = 9.sp) })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3; selectedPaket = null; selectedDoa = null }, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Doa", fontSize = 9.sp) })
                NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4; selectedPaket = null; selectedDoa = null }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Saya", fontSize = 9.sp) })
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                selectedPaket != null -> DetailPaketScreen(paket = selectedPaket!!, onBack = { selectedPaket = null })
                selectedDoa != null -> DetailDoaScreen(doa = selectedDoa!!, onBack = { selectedDoa = null })
                else -> when (selectedTab) {
                    0 -> BerandaScreen(onPaketClick = { selectedPaket = it })
                    1 -> PaketListScreen(onPaketClick = { selectedPaket = it })
                    2 -> DokumenScreen()
                    3 -> DoaListScreen(onDoaClick = { selectedDoa = it })
                    4 -> SayaScreen()
                }
            }
        }
    }
}

@Composable
fun BerandaScreen(onPaketClick: (PaketUmrah) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Assalamualaikum,", color = Color(0xFFD1FAE5), fontSize = 14.sp)
                    Text("Imtiyaz Tour Jogja", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Melayani Seperti Keluarga", color = Color(0xFFFFD700), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Pilih Paket Umrah", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("5 pilihan, profesional & amanah", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
        }
        items(listPaket) { paket -> PaketCard(paket = paket, onClick = { onPaketClick(paket) }) }
    }
}

@Composable
fun PaketListScreen(onPaketClick: (PaketUmrah) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Pilih Paket Umrah", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("Tanpa WebView, semua di APK", fontSize = 11.sp, color = Color.Gray); Spacer(Modifier.height(8.dp)) }
        items(listPaket) { paket -> PaketCard(paket = paket, onClick = { onPaketClick(paket) }) }
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
                    Text("Form Pendaftaran (Fitur 1 - WA Langsung)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F7A5A))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = hp, onValueChange = { hp = it }, label = { Text("No HP / WA") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val pesan = "Assalamualaikum, saya mau daftar ${paket.nama} ${paket.harga} - Nama: $nama - HP: $hp"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/628112776543?text=${Uri.encode(pesan)}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Daftar via WhatsApp", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp))
                    Text("Akan membuka WhatsApp ke Admin 0811-277-6543", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DokumenScreen() {
    var dokumenList by remember { mutableStateOf(listDokumen.map { it.copy() }) }
    val progress = dokumenList.count { it.checked }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Checklist Dokumen (Fitur 3)", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("6 dokumen wajib umrah", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = progress / 6f, modifier = Modifier.fillMaxWidth().height(8.dp).padding(horizontal = 4.dp), color = Color(0xFF0F7A5A))
        Spacer(Modifier.height(4.dp))
        Text("$progress / 6 selesai - ${((progress/6f)*100).toInt()}%", fontSize = 11.sp, color = Color(0xFF0F7A5A), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dokumenList.size) { index ->
                val doc = dokumenList[index]
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = doc.checked, onCheckedChange = { checked -> dokumenList = dokumenList.toMutableList().also { it[index] = doc.copy(checked = checked) } })
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(doc.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(doc.deskripsi, fontSize = 11.sp, color = Color.Gray) }
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
            Text("Panduan Doa Offline (Fitur 4)", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Bisa dibaca tanpa internet", fontSize = 12.sp, color = Color.Gray)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Text("✅ Offline Mode - Tanpa internet di pesawat / Masjidil Haram", fontSize = 10.sp, color = Color(0xFF0F7A5A), modifier = Modifier.padding(8.dp)) }
            Spacer(Modifier.height(8.dp))
        }
        items(listDoa) { doa ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth().clickable { onDoaClick(doa) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(doa.judul, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(doa.latin.take(40) + "...", fontSize = 11.sp, color = Color.Gray, maxLines = 1) }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF0F7A5A))
                }
            }
        }
    }
}

@Composable
fun DetailDoaScreen(doa: Doa, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("← Kembali") }
            Spacer(Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text(doa.judul, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF0F7A5A))
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

@Composable
fun SayaScreen() {
    var total by remember { mutableStateOf("Rp 37.400.000") }
    var sudah by remember { mutableStateOf("Rp 10.000.000") }
    var sisa by remember { mutableStateOf("Rp 27.400.000") }
    var status by remember { mutableStateOf("Belum Lunas") }
    var showSkrining by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Saya", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            // FITUR 2 - Status Pembayaran
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Status Pembayaran (Fitur 2)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Paket: Linuwih - 9 Hari", fontSize = 11.sp, color = Color.Gray)
                    Text("Total: $total", fontSize = 13.sp)
                    Text("Sudah Dibayar: $sudah", fontSize = 13.sp)
                    Text("Sisa: $sisa", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 14.sp)
                    Text("Status: $status", fontWeight = FontWeight.Bold, color = if (status == "Lunas") Color(0xFF0F7A5A) else Color(0xFFDC2626), fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {}, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("Galeri", fontSize = 12.sp) }
                        Button(onClick = {}, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(8.dp)) { Text("Kamera", fontSize = 12.sp) }
                    }
                    Text("Upload bukti transfer - akan diverifikasi admin", fontSize = 10.sp, color = Color.Gray)
                }
            }
            // FITUR 8 - Skrining
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Skrining Kesehatan Lansia (Fitur 8)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("29 Pertanyaan A-H - Wajib untuk Kamulyan & Linuwih", fontSize = 11.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    if (!showSkrining) {
                        Button(onClick = { showSkrining = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A)), shape = RoundedCornerShape(12.dp)) { Text("Mulai Skrining - 29 Pertanyaan") }
                    } else {
                        SkriningForm(onClose = { showSkrining = false })
                    }
                }
            }
            // Profil
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Imtiyaz Tour Jogja", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F7A5A))
                    Text("PPIU U383/2021 • Jln. Pertapan, Tegal Cerme RT08, Baturetno, Banguntapan, Bantul", fontSize = 11.sp, color = Color.Gray)
                    Text("Kontak: 0811-277-6543 • pastiumrah.com", fontSize = 11.sp, color = Color(0xFF0F7A5A))
                }
            }
        }
    }
}

@Composable
fun SkriningForm(onClose: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    var nama by remember { mutableStateOf("") }
    var penyakit by remember { mutableStateOf("") }
    var obat by remember { mutableStateOf("") }
    var alergi by remember { mutableStateOf("") }
    var kontakDarurat by remember { mutableStateOf("") }

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
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (step > 1) { OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text("Kembali") } }
            if (step < 4) { Button(onClick = { step++ }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A))) { Text("Lanjut") } }
            else { Button(onClick = onClose, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7A5A))) { Text("Kirim ke Admin") } }
        }
    }
}
