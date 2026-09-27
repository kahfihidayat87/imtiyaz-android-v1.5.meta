# Imtiyaz Tour — Project Handoff Document

**Terakhir update:** 27 Sep 2026
**Repo utama:** https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta
**Local path:** C:\Users\DELL\Documents\GitHub\imtiyaz-android-v1.5.meta

---

## Status Saat Ini

| Item | Nilai |
|---|---|
| Versi aktif (App Jamaah) | **2.12.0** (versionCode 14) |
| Commit terakhir | `1c47343` fix(quiz): drag ghost overlay + workflow release APK |
| Branch | `main` |
| CI | GitHub Actions hijau |
| Total file Kotlin | **22 file** |
| Warna tema | Hijau `#0F7A5A` |
| Distribusi | Play Store (AAB) + APK Release signed (langsung) |

---

## Struktur Project (4 Komponen)

### 1. App Jamaah — `imtiyaz-android-v1.5.meta`
- Package: `com.imtiyaztour.app`
- Distribusi: Play Store (via AAB) + APK signed via GitHub Release
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

## File Kotlin di App Jamaah (22 file, per 27 Sep 2026)

| File | Fungsi |
|---|---|
| MainActivity.kt | Entry point, navigasi 6 tab, splash |
| Adzan.kt | Notifikasi adzan otomatis |
| AlMatsurat.kt | Al-Ma'tsurat Sughra |
| Announcement.kt | Info dari admin + notif |
| AudioPlayer.kt | MediaPlayer shared helper |
| FindJamaah.kt | UI TL cari jamaah + tombol peta |
| HafalanQuran.kt | Quiz Tebak Kata + Susun Ayat (Juz 30) |
| Invoice.kt | Daftar invoice PDF jamaah |
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
| SusunAyat.kt | Susun potongan ayat per ronde (drag & drop) |

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
15. Susun Ayat — susun potongan ayat per ronde (drag & drop)
16. Quiz Hafalan Juz 30
17. Al-Ma'tsurat Sughra — card di DoaListScreen
18. Tracking Istiqamah — 8 kategori + alarm Dzikir/Sedekah/Muhasabah
19. Dokumen ke Saya — akses invoice PDF jamaah
20. Peta Multi-Jamaah — semua jamaah di kanal, skip marker tanpa lokasi

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
- versionCode 13 → 14, versionName 2.11.2 → 2.12.0
- Commit: `aca2c24`

### TAHAP 3 - Find Jamaah Scrollable (SELESAI)
- Commit: `fdee216`

### TAHAP 4 - Invoice Jamaah PDF (SELESAI)
- Commit: `1d0a56e`, `d6c8c4d`

### TAHAP 5 - Peta Multi-Jamaah (SELESAI)
- Commit: `e7cc984`

### TAHAP 6 - Tracking Istiqamah (SELESAI)
- Commit: `bd0a1d7`, `97fcaf0`, `cbf6012`

### TAHAP 7 - Quiz + Al-Ma'tsurat (SELESAI)
- Commit: `9b0acf2`

### TAHAP 8 - Hotfix ColumnScope (SELESAI)
- Commit: `944a669`
- File: `SusunAyat.kt` — tambah `ColumnScope.` di signature `RondeSusunAyat`

### TAHAP 9 - Update HANDOFF.md (SELESAI)
- Commit: `6b82eef`
- Update 22 file Kotlin, TAHAP 3-8

### TAHAP 10 - Quiz Polish (SELESAI)
- Commit: `8860c05`, `c074a4e`, `1c47343`
- **Fix 1:** Scroll area potongan ayat (wrapper `Column` + `verticalScroll`)
- **Fix 2:** Tebak Kata skip kata awal/akhir (hanya index 1..n-2, skip ayat <4 kata)
- **Fix 3:** Deteksi drop pakai posisi jari (bukan pusat item) → hindari clip scroll
- **Fix 4:** Drag ghost overlay — item didrag muncul di depan slot/placeholder

### TAHAP 11 - Build APK Release Signed (SELESAI)
- File: `.github/workflows/build-apk.yml`
- Tambah 3 step: `Build Release APK`, `Upload Release APK`, `Create GitHub Release`
- APK rilis otomatis di-upload ke GitHub Releases sebagai artifact publik
- Link download permanen: `https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta/releases/latest`

### TAHAP 12 - Belum Ditentukan
Kandidat:
- A. Quick action layar Find Jamaah (copy koordinat, share WA)
- B. RBAC & keamanan App Admin
- C. Offline resilience (cache shalat/manasik, retry queue)
- D. Polish UI menyeluruh
- E. Lanjut ke repo `imtiyaz-admin-android`
- F. Migrasi signing ke Play App Signing (kalau mau publish Play Store)

---

## File Kritis

- `MainActivity.kt` (Jamaah) - semua navigasi & layar
- `MainActivity.kt` (Admin) - RBAC filter tab
- `PrayerTimes.kt` - hitung jadwal astronomis
- `LocateService.kt` - service ntfy tracking
- `SusunAyat.kt` - quiz drag & drop (baru 27 Sep)
- `HafalanQuran.kt` - quiz Tebak Kata + Susun Ayat
- `app.js` (Node) - proxy utama
- `imtiyaz-connector.php` - plugin WP utama
- `.github/workflows/build-apk.yml` - CI: debug + AAB + APK release signed
- `.env` (server) - credentials WP, RAHASIA
- `imtiyaz-release-key.jks` (LOKAL, JANGAN COMMIT) - keystore signing

---

## Distribution & Release

### Play Store (AAB)
- Upload AAB dari artifact `Imtiyaz-AAB-PlayStore` di setiap GitHub Actions run

### APK Release (Langsung, Tanpa Play Store)
- APK signed otomatis dibuat setiap push ke `main`
- Link GitHub Release: https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta/releases/latest
- File: `app-release.apk` (signed dengan release keystore)
- Cocok untuk distribusi cepat ke jamaah via WhatsApp/Google Drive

### Secrets GitHub yang Diperlukan
- `RELEASE_KEYSTORE_BASE64` - keystore di-encode base64
- `RELEASE_KEYSTORE_PASSWORD` - password keystore
- `RELEASE_KEY_ALIAS` - alias key (`imtiyaz-key`)
- `RELEASE_KEY_PASSWORD` - password key

---

## Environment

- Local: Windows 11, Git Bash (MINGW64)
- Repo path: `C:/Users/DELL/Documents/GitHub/imtiyaz-android-v1.5.meta`
- Remote: `https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta.git`
- Server: Hostinger (Node.js + WordPress)
- Tools: Git Bash, Python, gh CLI, pyyaml

---

## Konvensi

1. Script patch disimpan lokal (`patch-*.py`), TIDAK di-commit
2. File backup `*.bak-*`, TIDAK di-commit
3. Line endings: LF (via `.gitattributes`)
4. Signing release: env variable (GitHub Secrets)
5. Version bump setiap rilis
6. Commit kecil per tahap, tunggu CI hijau sebelum lanjut
7. **JANGAN pernah paste token / password ke chat atau commit**
8. Update HANDOFF.md setiap batch fitur baru (jangan menumpuk >5 commit)

---

## Catatan untuk Percakapan Berikutnya

1. Konteks hilang setiap mulai chat baru - kirim file ini + repo link
2. Format jawaban disukai: Bahasa Indonesia, tabel ringkas, step-by-step Git Bash
3. Preferensi: commit kecil per tahap, tunggu CI hijau sebelum lanjut
4. Hindari: emoji berlebihan, penjelasan bertele-tele
5. **Jangan paste snippet Kotlin/XML langsung ke Git Bash** - selalu gunakan wrapper `cat > patch.py << 'PYEOF'`
6. Setiap edit kode Kotlin: verifikasi brace balance (`python -c "..."`) sebelum commit
7. Checklist verifikasi saat mulai chat:
   - `git log --oneline -10`
   - `gh run list --limit 5`
   - `gh secret list`
   - `sed -n '1,40p' HANDOFF.md`

---

## Link Penting

- GitHub Actions: https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta/actions
- GitHub Releases (APK download): https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta/releases
- GitHub Secrets: https://github.com/kahfihidayat87/imtiyaz-android-v1.5.meta/settings/secrets/actions
