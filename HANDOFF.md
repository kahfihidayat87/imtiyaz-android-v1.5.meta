# Imtiyaz Tour — Project Handoff Document

**Terakhir update:** 26 Sep 2026
**Repo utama:** https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta
**Local path:** C:\Users\DELL\Documents\GitHub\imtiyaz-android-v1.5.meta

---

## Status Saat Ini

| Item | Nilai |
|---|---|
| Versi aktif (App Jamaah) | **2.12.0** (versionCode 14) |
| Commit terakhir | `944a669` fix: add ColumnScope receiver to RondeSusunAyat |
| Branch | `main` |
| CI | GitHub Actions hijau |
| Total file Kotlin | **22 file** |
| Warna tema | Hijau `#0F7A5A` |

---

## Struktur Project (4 Komponen)

### 1. App Jamaah — `imtiyaz-android-v1.5.meta`
- Package: `com.imtiyaztour.app`
- Distribusi: Play Store (via AAB release)
- Warna: Hijau `#0F7A5A`
- Status: Sedang dikerjakan aktif

### 2. App Admin — `imtiyaz-admin-android`
- Package: `com.imtiyaztour.admin`
- Distribusi: Private APK
- Warna: Biru `#1E3A8A`
- Status: Belum dikirim ke konteks ini

### 3. WordPress Plugin — `pastiumrah.com`
- Path: `public_html/wp-content/plugins/connector-app/`
- File utama: `imtiyaz-connector.php`, `imtiyaz-admin-endpoints.php`, `imtiyaz-admin-ui-trait.php`
- Custom Tables: `wpfq_imtiyaz_locate_req`, `wpfq_imtiyaz_locate_res`, `wpfq_imtiyaz_admin`
- Endpoint REST: ~30 endpoint

### 4. Node.js Proxy — `api.pastiumrah.com`
- File: `app.js` v2.12.0 (~700 baris)
- Host: Hostinger (~/domains/api.pastiumrah.com/)
- Fungsi: Proxy + cache + upload handler
- Cache: 5 menit konten publik, 30 detik announcement
- Endpoint kunci: `/api/find`, `/api/track-ping`, `/api/kanal-jamaah`, `/api/radio/send`, `/api/radio/poll`

---

## File Kotlin di App Jamaah (22 file, per 26 Sep 2026)

| File | Fungsi |
|---|---|
| MainActivity.kt | Entry point, navigasi 6 tab, splash |
| Adzan.kt | Notifikasi adzan otomatis |
| AlMatsurat.kt | Al-Ma'tsurat Sughra (NEW) |
| Announcement.kt | Info dari admin + notif |
| AudioPlayer.kt | MediaPlayer shared helper |
| FindJamaah.kt | UI TL cari jamaah + tombol peta |
| HafalanQuran.kt | Quiz Hafalan Juz 30 (NEW) |
| Invoice.kt | Daftar invoice PDF jamaah (NEW) |
| Itinerary.kt | Rencana perjalanan 9 hari |
| JadwalKeberangkatan.kt | Trip real-time WP Travel Engine |
| Journal.kt | Catatan perjalanan lokal |
| LocateService.kt | Foreground service ntfy tracking |
| LocationHelper.kt | GPS multi-sample + adaptive battery |
| LokasiMapView.kt | Peta inline OSM (osmdroid) |
| Manasik.kt | 14 langkah interaktif + progress |
| Pembimbing.kt | Profil ustadz + audio ceramah |
| PrayerTimes.kt | Jadwal shalat Ummul Qura |
| Quran.kt | 114 surat via equran.id v2 |
| Radio.kt | Voice note TL (polling 2.5s) |
| Reminder.kt | Pengingat ibadah & kesehatan |
| SusunAyat.kt | Fitur susun potongan ayat per ronde (NEW) |

Catatan: fitur Tracking Istiqamah (8 kategori) terintegrasi di MainActivity/Reminder, tidak ada file terpisah.

---

## Fitur di App Jamaah

1. Jadwal Shalat (Ummul Qura + notifikasi adzan)
2. Al-Quran (114 surat + audio 6 qari)
3. Doa (14 doa + audio opsional dari admin)
4. Manasik (14 langkah + progress)
5. Journal (catatan perjalanan lokal)
6. Reminder (ibadah + kesehatan)
7. Pengumuman (dari admin)
8. Radio Tour Leader (push-to-talk via polling)
9. Tracking Lansia (TL cari lokasi jamaah)
10. Pembimbing (profil + audio ceramah)
11. Itinerary & Jadwal Keberangkatan
12. Checklist Dokumen (diisi admin)
13. Upload Bukti Transfer
14. Skrining Kesehatan 29 pertanyaan
15. **Susun Ayat** — susun potongan ayat per ronde (NEW)
16. **Quiz Hafalan Juz 30** (NEW)
17. **Al-Ma'tsurat Sughra** — card di DoaListScreen (NEW)
18. **Tracking Istiqamah** — 8 kategori + alarm Dzikir/Sedekah/Muhasabah (NEW)
19. **Dokumen ke Saya** — akses invoice PDF jamaah (NEW)
20. **Peta Multi-Jamaah** — semua jamaah di kanal, skip marker tanpa lokasi (NEW)

---

## Riwayat TAHAP

### TAHAP 1 - Fix CI Workflow (SELESAI)
- File: `.github/workflows/build-apk.yml`
- Masalah: `secrets` dipakai di `if:` level step
- Solusi: Pindah ke `env:` di level job (`HAS_RELEASE_KEYSTORE`)

### TAHAP 2 - Polish Tombol Peta (SELESAI)
- File: `FindJamaah.kt`
- Tombol "Buka di Google Maps" + helper `openInMapsApp(context, lat, lon, label)`
- Commit: `9a2b0fe`

### TAHAP 2.5 - Bump Versi 2.12.0 (SELESAI)
- File: `android-app/app/build.gradle`
- versionCode 13 → 14, versionName 2.11.2 → 2.12.0
- Commit: `aca2c24`

### TAHAP 3 - Find Jamaah Scrollable (SELESAI)
- Commit: `fdee216`

### TAHAP 4 - Invoice Jamaah PDF (SELESAI)
- Commit: `1d0a56e`, `d6c8c4d`
- Hanya tampilkan invoice terbaru (sorted by date desc)

### TAHAP 5 - Peta Multi-Jamaah (SELESAI)
- Commit: `e7cc984`
- Tampilkan semua jamaah di kanal, skip marker tanpa lokasi

### TAHAP 6 - Tracking Istiqamah (SELESAI)
- Commit: `bd0a1d7`, `97fcaf0`, `cbf6012`
- Tab Tracking 8 kategori, Dokumen ke Saya, navigasi tab instan
- Toggle berjamaah sholat, konten islami, fix reactivity poin
- Toggle alarm "Ingatkan Aku" (Dzikir/Sedekah/Muhasabah)

### TAHAP 7 - Quiz + Al-Ma'tsurat (SELESAI)
- Commit: `9b0acf2`
- Quiz Hafalan Juz 30 + Al-Ma'tsurat Sughra

### TAHAP 8 - Hotfix ColumnScope (SELESAI)
- Commit: `944a669`
- File: `SusunAyat.kt`
- Masalah: `Modifier.weight()` error karena `RondeSusunAyat` tidak punya receiver `ColumnScope`
- Solusi: tambah `ColumnScope.` di signature fungsi (baris 94)
- CI: hijau

### TAHAP 9 - Belum Ditentukan
Kandidat:
- A. Update HANDOFF.md berkala (dokumen ini)
- B. Quick action layar Find Jamaah (copy koordinat, share WA)
- C. RBAC & keamanan App Admin
- D. Offline resilience (cache shalat/manasik, retry queue)
- E. Polish UI menyeluruh
- F. Lanjut ke repo `imtiyaz-admin-android`

---

## File Kritis

- `MainActivity.kt` (Jamaah) - semua navigasi & layar
- `MainActivity.kt` (Admin) - RBAC filter tab
- `PrayerTimes.kt` - hitung jadwal astronomis
- `LocateService.kt` - service ntfy tracking
- `app.js` (Node) - proxy utama
- `imtiyaz-connector.php` - plugin WP utama
- `.env` (server) - credentials WP, RAHASIA

---

## Environment

- Local: Windows 11, Git Bash (MINGW64)
- Repo path: `C:/Users/DELL/Documents/GitHub/imtiyaz-android-v1.5.meta`
- Remote: `https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta.git`
- Server: Hostinger (Node.js + WordPress)
- Tools: Git Bash, Python, gh CLI

---

## Konvensi

1. Script patch disimpan lokal (`patch-*.py`), TIDAK di-commit
2. File backup `*.bak-*`, TIDAK di-commit
3. Line endings: LF (via `.gitattributes`)
4. Signing release: env variable (GitHub Secrets)
5. Version bump setiap rilis
6. Commit kecil per tahap, tunggu CI hijau sebelum lanjut

---

## Catatan untuk Percakapan Berikutnya

1. Konteks hilang setiap mulai chat baru - kirim file ini + repo link
2. Format jawaban disukai: Bahasa Indonesia, tabel ringkas, step-by-step Git Bash
3. Preferensi: commit kecil per tahap, tunggu CI hijau sebelum lanjut
4. Hindari: emoji berlebihan, penjelasan bertele-tele
5. Update dokumen ini setiap kali ada batch fitur baru (jangan sampai 9 commit tertinggal lagi)
6. Checklist verifikasi saat mulai chat:
   - `git log --oneline -10`
   - `gh run list --limit 3`
   - `sed -n '1,30p' HANDOFF.md`
