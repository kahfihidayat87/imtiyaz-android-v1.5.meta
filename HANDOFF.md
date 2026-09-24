# Imtiyaz Tour — Project Handoff Document

**Terakhir update:** 24 Sep 2026
**Repo utama:** https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta
**Local path:** C:\Users\DELL\Documents\GitHub\imtiyaz-android-v1.5.meta

---

## Status Saat Ini

| Item | Nilai |
|---|---|
| Versi aktif (App Jamaah) | **2.12.0** (versionCode 14) |
| Commit terakhir | `aca2c24` chore: bump versi ke 2.12.0 |
| Branch | `main` |
| CI | GitHub Actions hijau |
| Total file Kotlin | 15 file (~7.500 baris) |
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

## File Kotlin di App Jamaah

| File | Fungsi |
|---|---|
| MainActivity.kt | Entry point, navigasi 6 tab, splash |
| ApiConfig.kt | Base URL API |
| PrayerTimes.kt | Jadwal shalat Ummul Qura |
| Adzan.kt | Notifikasi adzan otomatis |
| Quran.kt | 114 surat via equran.id v2 |
| AudioPlayer.kt | MediaPlayer shared helper |
| Manasik.kt | 14 langkah interaktif + progress |
| Journal.kt | Catatan perjalanan lokal |
| Reminder.kt | Pengingat ibadah & kesehatan |
| Announcement.kt | Info dari admin + notif |
| Radio.kt | Voice note TL (polling 2.5s) |
| LocateService.kt | Foreground service ntfy tracking |
| LocationHelper.kt | GPS multi-sample + adaptive battery |
| FindJamaah.kt | UI TL cari jamaah + tombol peta |
| LokasiMapView.kt | Peta inline OSM (osmdroid) |
| Pembimbing.kt | Profil ustadz + audio ceramah |
| Itinerary.kt | Rencana perjalanan 9 hari |
| JadwalKeberangkatan.kt | Trip real-time WP Travel Engine |

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

---

## Riwayat TAHAP

### TAHAP 1 - Fix CI Workflow (SELESAI)
- File: `.github/workflows/build-apk.yml`
- Masalah: GitHub Actions menolak workflow karena `secrets` dipakai di `if:` level step
- Solusi: Pindahkan ke `env:` di level job (`HAS_RELEASE_KEYSTORE`)
- Status: Selesai

### TAHAP 2 - Polish Tombol Peta (SELESAI)
- File: `FindJamaah.kt`
- Perubahan: Tombol tunggal "Buka di Peta" jadi tombol "Buka di Google Maps"
- Helper baru: `openInMapsApp(context, lat, lon, label)` dengan fallback browser silent
- Commit: `9a2b0fe`
- Catatan: Tombol Waze & Browser dihapus, fallback browser tetap ada (silent)

### TAHAP 2.5 - Bump Versi (SELESAI)
- File: `android-app/app/build.gradle`
- Perubahan: versionCode 13 jadi 14, versionName 2.11.2 jadi 2.12.0
- Commit: `aca2c24`

### TAHAP 3 - Belum Ditentukan
Kandidat:
- A. Quick action di layar hasil Find Jamaah (copy koordinat, share WA)
- B. RBAC & keamanan App Admin
- C. Offline resilience (cache shalat/manasik, retry queue)
- D. Polish UI menyeluruh
- E. Lanjut ke repo imtiyaz-admin-android

---

## File Kritis

- MainActivity.kt (Jamaah) - semua navigasi & layar
- MainActivity.kt (Admin) - RBAC filter tab
- PrayerTimes.kt - hitung jadwal astronomis
- LocateService.kt - service ntfy tracking
- app.js (Node) - proxy utama
- imtiyaz-connector.php - plugin WP utama
- .env (server) - credentials WP, RAHASIA

---

## Environment

- Local: Windows 11, Git Bash (MINGW64)
- Repo path: C:/Users/DELL/Documents/GitHub/imtiyaz-android-v1.5.meta
- Remote: https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta.git
- Server: Hostinger (Node.js + WordPress)
- Tools: Git Bash, Python, gh CLI

---

## Konvensi

1. Script patch disimpan lokal (patch-*.py), TIDAK di-commit
2. File backup *.bak-*, TIDAK di-commit
3. Line endings: LF (via .gitattributes)
4. Signing release: env variable (GitHub Secrets)
5. Version bump setiap rilis

---

## Catatan untuk Percakapan Berikutnya

1. Konteks hilang setiap mulai chat baru - kirim file ini + repo link
2. Format jawaban disukai: Bahasa Indonesia, tabel ringkas, step-by-step Git Bash
3. Preferensi: commit kecil per tahap, tunggu CI hijau sebelum lanjut
4. Hindari: emoji berlebihan, penjelasan bertele-tele
5. Selalu tanya konteks TAHAP 1 kalau belum jelas
