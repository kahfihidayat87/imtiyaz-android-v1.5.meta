#!/usr/bin/env python3
"""
Patch otomatis Imtiyaz Tour -- 4 fitur baru (Adzan, Manasik, Journal, Reminder).
Backup otomatis ke *.bak2 sebelum mengubah.
"""
import sys, os, shutil

def detect_app():
    for p in ("android-app/app", "app"):
        if os.path.isdir(os.path.join(p, "src/main")):
            return p
    print("ERROR: Jalankan script ini dari root repo (yang berisi android-app/ atau app/)")
    sys.exit(1)

def read(p):
    with open(p, "r", encoding="utf-8", newline="") as f:
        return f.read().replace("\r\n", "\n").replace("\r", "\n")

def write(p, c):
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(c)

def patch(content, find, repl, label):
    if find not in content:
        print(f"ERROR [{label}]: pola tidak ditemukan.")
        print("  Mencari: " + repr(find[:80]))
        sys.exit(2)
    n = content.count(find)
    if n > 1:
        print(f"  WARN [{label}]: pola muncul {n}x, diganti yang pertama")
    return content.replace(find, repl, 1)

APP = detect_app()
JAVA = f"{APP}/src/main/java/com/imtiyaztour/app"
MAN = f"{APP}/src/main/AndroidManifest.xml"
PR  = f"{JAVA}/PrayerTimes.kt"
MA  = f"{JAVA}/MainActivity.kt"

print(f"App: {APP}\n")

for f in (MAN, PR, MA):
    if not os.path.isfile(f):
        print(f"ERROR: File tidak ditemukan: {f}")
        sys.exit(1)
    shutil.copy(f, f + ".bak2")
    print(f"Backup: {f}.bak2")
print()

# ===== AndroidManifest.xml =====
c = read(MAN)
c = patch(c,
    '    <uses-permission android:name="android.permission.RECORD_AUDIO" />',
    '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n'
    '    <!-- Fitur baru: Adzan Alert & Pengingat Ibadah -->\n'
    '    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n'
    '    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />\n'
    '    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />',
    "manifest-perm")
c = patch(c,
    '        </provider>\n    </application>',
    '        </provider>\n\n'
    '        <!-- Fitur Adzan Alert -->\n'
    '        <receiver android:name=".AdzanReceiver" android:exported="false" />\n'
    '        <receiver android:name=".BootReceiver" android:exported="true">\n'
    '            <intent-filter>\n'
    '                <action android:name="android.intent.action.BOOT_COMPLETED" />\n'
    '            </intent-filter>\n'
    '        </receiver>\n\n'
    '        <!-- Fitur Pengingat Ibadah -->\n'
    '        <receiver android:name=".ReminderReceiver" android:exported="false" />\n'
    '    </application>',
    "manifest-recv")
write(MAN, c)
print("OK AndroidManifest.xml")

# ===== PrayerTimes.kt =====
c = read(PR)

c = patch(c,
    'import android.content.pm.PackageManager\n',
    'import android.content.pm.PackageManager\nimport android.os.Build\n',
    "prayer-import")

c = patch(c,
    '        if (result == null) errorMsg = "Gagal menghitung jadwal untuk lokasi ini" else times = result',
    '        if (result == null) {\n'
    '            errorMsg = "Gagal menghitung jadwal untuk lokasi ini"\n'
    '        } else {\n'
    '            times = result\n'
    '            try { AdzanScheduler.scheduleAll(context, result) } catch (e: Exception) {}\n'
    '        }',
    "prayer-sched")

c = patch(c,
    '    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->\n'
    '        hasPermission = result.values.any { it }\n'
    '    }',
    '    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->\n'
    '        hasPermission = result.values.any { it }\n'
    '    }\n'
    '    val notifPermLauncher = rememberLauncherForActivityResult(\n'
    '        ActivityResultContracts.RequestPermission()\n'
    '    ) { }',
    "prayer-perm")

c = patch(c,
    '            Text("Estimasi Ummul Qura -- rujuk adzan Masjidil Haram/Nabawi setempat", fontSize = 8.sp, color = Color.LightGray)',
    '            Text("Estimasi Ummul Qura -- rujuk adzan Masjidil Haram/Nabawi setempat", fontSize = 8.sp, color = Color.LightGray)\n'
    '\n'
    '            Spacer(Modifier.height(10.dp))\n'
    '            Divider()\n'
    '            Row(\n'
    '                Modifier.fillMaxWidth().padding(top = 8.dp),\n'
    '                verticalAlignment = Alignment.CenterVertically,\n'
    '                horizontalArrangement = Arrangement.SpaceBetween\n'
    '            ) {\n'
    '                Column(Modifier.weight(1f)) {\n'
    '                    Text("Notifikasi Adzan", fontSize = 12.sp, fontWeight = FontWeight.Bold)\n'
    '                    Text("Dapat notifikasi otomatis di setiap waktu shalat",\n'
    '                        fontSize = 10.sp, color = Color.Gray)\n'
    '                }\n'
    '                var adzanOn by remember { mutableStateOf(AdzanPrefs.isEnabled(context)) }\n'
    '                Switch(\n'
    '                    checked = adzanOn,\n'
    '                    onCheckedChange = { v ->\n'
    '                        adzanOn = v\n'
    '                        AdzanPrefs.setEnabled(context, v)\n'
    '                        if (v) {\n'
    '                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&\n'
    '                                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)\n'
    '                                    != PackageManager.PERMISSION_GRANTED) {\n'
    '                                notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)\n'
    '                            }\n'
    '                            times?.let { AdzanScheduler.scheduleAll(context, it) }\n'
    '                        } else {\n'
    '                            AdzanScheduler.cancelAll(context)\n'
    '                        }\n'
    '                    }\n'
    '                )\n'
    '            }',
    "prayer-toggle")

write(PR, c)
print("OK PrayerTimes.kt")

# ===== MainActivity.kt =====
c = read(MA)

c = patch(c,
    '    var showJadwal by remember { mutableStateOf(false) }',
    '    var showJadwal by remember { mutableStateOf(false) }\n'
    '    var showManasik by remember { mutableStateOf(false) }\n'
    '    var showJournalList by remember { mutableStateOf(false) }\n'
    '    var showJournalEdit by remember { mutableStateOf<JournalEntry?>(null) }\n'
    '    var showJournalEditActive by remember { mutableStateOf(false) }\n'
    '    var showReminder by remember { mutableStateOf(false) }',
    "main-state")

old_bh = '''    androidx.activity.compose.BackHandler(
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
    }'''
new_bh = '''    androidx.activity.compose.BackHandler(
        enabled = selectedPaket != null || selectedDoa != null || selectedSurah != null ||
                  showItinerary || showRadio || showJadwal || showPembimbingList ||
                  selectedPembimbing != null || showManasik || showJournalList ||
                  showJournalEditActive || showReminder
    ) {
        when {
            selectedPaket != null -> selectedPaket = null
            selectedDoa != null -> selectedDoa = null
            selectedSurah != null -> selectedSurah = null
            selectedPembimbing != null -> selectedPembimbing = null
            showJournalEditActive -> { showJournalEditActive = false; showJournalEdit = null }
            showItinerary -> showItinerary = false
            showRadio -> showRadio = false
            showJadwal -> showJadwal = false
            showPembimbingList -> showPembimbingList = false
            showManasik -> showManasik = false
            showJournalList -> showJournalList = false
            showReminder -> showReminder = false
        }
    }'''
c = patch(c, old_bh, new_bh, "main-backhandler")

c = patch(c,
    '        launch { try { AppData.doaAudio = ApiClient.service.getDoaAudio() } catch (e: Exception) { /* tidak ada audio, tampilkan teks saja */ } }\n    }',
    '        launch { try { AppData.doaAudio = ApiClient.service.getDoaAudio() } catch (e: Exception) { /* tidak ada audio, tampilkan teks saja */ } }\n    }\n'
    '\n'
    '    LaunchedEffect(Unit) {\n'
    '        try { ReminderScheduler.rescheduleAll(context) } catch (e: Exception) {}\n'
    '    }',
    "main-reminder")

c = patch(c,
    '                showPembimbingList -> PembimbingListScreen(onPembimbingClick = { selectedPembimbing = it }, onBack = { showPembimbingList = false })\n                else -> when (selectedTab) {',
    '                showPembimbingList -> PembimbingListScreen(onPembimbingClick = { selectedPembimbing = it }, onBack = { showPembimbingList = false })\n'
    '                showManasik -> ManasikScreen(onBack = { showManasik = false })\n'
    '                showJournalEditActive -> JournalEditScreen(\n'
    '                    existing = showJournalEdit,\n'
    '                    onBack = { showJournalEditActive = false; showJournalEdit = null },\n'
    '                    onSaved = { showJournalEditActive = false; showJournalEdit = null; showJournalList = true }\n'
    '                )\n'
    '                showJournalList -> JournalListScreen(\n'
    '                    onBack = { showJournalList = false },\n'
    '                    onOpen = { entry -> showJournalEdit = entry; showJournalEditActive = true }\n'
    '                )\n'
    '                showReminder -> ReminderScreen(onBack = { showReminder = false })\n'
    '                else -> when (selectedTab) {',
    "main-nav")

c = patch(c,
    '                    0 -> BerandaScreen(onPaketClick = { selectedPaket = it }, onItineraryClick = { showItinerary = true }, onJadwalClick = { showJadwal = true }, onRadioClick = { showRadio = true }, onPembimbingClick = { showPembimbingList = true })',
    '                    0 -> BerandaScreen(\n'
    '                        onPaketClick = { selectedPaket = it },\n'
    '                        onItineraryClick = { showItinerary = true },\n'
    '                        onJadwalClick = { showJadwal = true },\n'
    '                        onRadioClick = { showRadio = true },\n'
    '                        onPembimbingClick = { showPembimbingList = true },\n'
    '                        onManasikClick = { showManasik = true },\n'
    '                        onJournalClick = { showJournalList = true },\n'
    '                        onReminderClick = { showReminder = true }\n'
    '                    )',
    "main-beranda-call")

c = patch(c,
    'fun BerandaScreen(onPaketClick: (PaketUmrah) -> Unit, onItineraryClick: () -> Unit, onJadwalClick: () -> Unit, onRadioClick: () -> Unit, onPembimbingClick: () -> Unit) {',
    'fun BerandaScreen(\n'
    '    onPaketClick: (PaketUmrah) -> Unit,\n'
    '    onItineraryClick: () -> Unit,\n'
    '    onJadwalClick: () -> Unit,\n'
    '    onRadioClick: () -> Unit,\n'
    '    onPembimbingClick: () -> Unit,\n'
    '    onManasikClick: () -> Unit,\n'
    '    onJournalClick: () -> Unit,\n'
    '    onReminderClick: () -> Unit\n'
    ') {',
    "main-beranda-sig")

new_cards = '''            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth().clickable { onManasikClick() }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Checklist, contentDescription = null, tint = Color(0xFF0F7A5A))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Panduan Manasik", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Langkah demi langkah + centang progres", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF0F7A5A))
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth().clickable { onJournalClick() }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = Color(0xFF0F7A5A))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Catatan Perjalanan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Tulis momen & doa selama umrah (privat)", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF0F7A5A))
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth().clickable { onReminderClick() }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFF0F7A5A))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Pengingat Ibadah & Kesehatan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Atur pengingat minum obat, air, istirahat", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF0F7A5A))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Pilih Paket Umrah", fontWeight = FontWeight.Bold, fontSize = 18.sp)'''

c = patch(c,
    '            Spacer(Modifier.height(16.dp))\n            Text("Pilih Paket Umrah", fontWeight = FontWeight.Bold, fontSize = 18.sp)',
    new_cards,
    "main-cards")

write(MA, c)
print("OK MainActivity.kt")

print("\n== SEMUA PATCH BERHASIL ==")
print("\nSelanjutnya:")
print(f"  cd {APP}")
print("  ./gradlew :app:assembleDebug --no-daemon --stacktrace")
print("\nRollback (kalau perlu):")
print("  untuk setiap *.bak2, rename balik ke nama aslinya")