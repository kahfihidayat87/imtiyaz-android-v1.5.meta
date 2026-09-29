# HANDOFF Sesi — 29 September 2026

**Sesi:** Refactor besar (jamaah + admin + server)
**Durasi:** ~72 turn
**Status akhir:** Semua batch sukses, CI hijau, data skrining teruji end-to-end

---

## Ringkasan Eksekutif

| Area | Yang Selesai |
|---|---|
| App Jamaah | Drop ABI (66-36MB), Deprecate LocateService, Mode Aman FGS, Skrining 8 section, Rapikan UI Radio |
| App Admin | Invoice polish (logo, Payment Due, mobile), Script restart, Handle EADDRINUSE |
| Node.js | FCM collapseKey, Cleanup duplikat proses, 1 PID sehat |
| WordPress | Metabox skrining baru (imtiyaz_render_skrining_metabox) |

---

## REPO 1 - App Jamaah (imtiyaz-android-v1.5.meta)

**Lokal:** C:/Users/DELL/Documents/GitHub/imtiyaz-android-v1.5.meta
**Versi:** 2.12.0 (versionCode 14) - belum bump ke 2.13.0
**Ukuran APK:** ~36 MB

### Commit di Sesi Ini

| Commit | Isi |
|---|---|
| d05ecc4 | Bersih-bersih patch scripts + koreksi HANDOFF 26-25 |
| 0b596a3 | Deprecate LocateService.kt (ntfy legacy) |
| 265dbd5 | Script restart Node + dokumentasi |
| 3eee1ee | Catat patch FCM collapseKey |
| e1faa11 | Mode Aman FGS + rapikan UI Radio |
| c5afcbf | Skrining multi-step 8 section (A-H) |
| + 5 commit fixes | Skrining (validate, scroll, saveable, import, wildcards) |
| + 1 commit | Drop ABI x86 (66-36 MB) |

### File Kotlin (27 total)

Baru:
- SkriningModels.kt (281 baris)
- SkriningFormV2.kt (505 baris)

Dimodifikasi:
- MainActivity.kt (hapus 80 baris SkriningForm lama)
- build.gradle (abiFilters arm64-v8a + armeabi-v7a)
- AndroidManifest.xml (daftar ModeAmanService)
- Radio.kt (rapikan UI tombol Start/Stop)

### Tugas Belum Selesai

- Bump versi ke 2.13.0 + changelog rilis
- Optimasi APK minify (Batch 1b) - hemat ~5 MB lagi
- Test radio LiveKit end-to-end

---

## REPO 2 - App Admin (imtiyaz-admin-android)

**Lokal:** C:/Users/DELL/Documents/GitHub/imtiyaz-admin-android
**HEAD:** 1ac1c99
**Versi:** 1.0.0 (versionCode 1) - belum bump

### TAHAP 18 Selesai - Invoice Polish

| Perubahan | Lokasi |
|---|---|
| Logo perkecil 80-64 pt | invoice-routes.js:45 |
| Mobile kedua 081999876546 | invoice-routes.js:51 |
| Amount Due anti-wrap (x: 350-310, width 60-100) | invoice-routes.js:168-170 |
| Footer konfirmasi ke Admin Keuangan | invoice-routes.js:191 |
| Field Payment Due + DatePicker | InvoiceScreen.kt |
| Default value InvoicePayment.date/amount | AdminModels.kt |

### File Baru

- scripts/restart-node-admin.sh (restart server, kill parent + child)

### Tugas Belum Selesai

- Password plaintext di git history (AdminImtiyaz2026!) - rotate + purge
- PDF cache buster (&v=timestamp di pdf_url)

---

## SERVER - Node.js (api.pastiumrah.com)

**SSH:** ssh -p 65002 u120369480@153.92.10.222
**App dir:** ~/domains/api.pastiumrah.com/hbuilds/versions/01a0c969-934d-723f-9aba-f2cf0517d7a8/nodejs/

### Prosedur Restart WAJIB

Gunakan script: bash scripts/restart-node-admin.sh (di repo admin).

Manual (ringkas):
1. Kill parent + child: for pid in $(ps aux | grep -E "api.pastiumrah|node app.js" | grep -v grep | awk '{print $2}'); do kill -9 $pid; done
2. sleep 5, cek bersih
3. source /opt/alt/alt-nodejs24/enable
4. set -a; source ~/domains/api.pastiumrah.com/hbuilds/config/.env; set +a
5. setsid nohup node app.js >> console.log 2>> stderr.log < /dev/null &
6. sleep 6, cek proses + stderr kosong

### Pelajaran Kritis

| Gejala | Penyebab | Fix |
|---|---|---|
| EADDRINUSE :::3000 | Filter grep tidak tangkap child node app.js | Pakai -E "api.pastiumrah|node app.js" |
| 3 proses sekaligus | LiteSpeed wrapper auto-respawn + spawn manual | Kill semua, spawn 1 |
| PDF tidak berubah | File PDF nama sama - browser cache | Tambah &v=timestamp (belum) |

### PID State Sekarang

- node app.js - 1 PID sehat (verifikasi ulang sebelum debug)
- lsnode wrapper - bisa muncul/hilang, normal (idle kalau port sudah dipakai)

---

## SERVER - WordPress (pastiumrah.com)

**Plugin:** connector-app/imtiyaz-connector.php

### Metabox Baru (TAHAP 17)

Fungsi: imtiyaz_render_skrining_metabox - render _skrining_data (JSON) jadi tabel rapi.

Test:
1. Buka wp-admin/edit.php?post_type=skrining_kesehatan
2. Klik post Skrining - nama
3. Scroll ke bawah - tabel Data Skrining Kesehatan muncul

### Data Skrining Tersimpan

- Post ID 1482 (Skrining - gjkk)
- _jamaah_id: 1454
- _skrining_data: JSON lengkap

Verifikasi WP-CLI:
ssh -p 65002 u120369480@153.92.10.222 "cd ~/domains/pastiumrah.com/public_html && wp --allow-root post list --post_type=skrining_kesehatan --posts_per_page=5"

---

## Prioritas Sesi Berikutnya

### WAJIB (dulu)
1. Bump versi 2.13.0 - semua perubahan sesi ini menumpuk tanpa label
2. Test radio LiveKit end-to-end (voice note + Live manual)
3. Update changelog rilis di HANDOFF.md

### SEDANG
4. Optimasi APK minify (Batch 1b) - hemat ~5 MB
5. PDF cache buster di invoice-routes.js
6. Rotate password admin + purge git history

### LANJUTAN
7. WP metabox admin untuk skrining (asesmen medis)
8. LiveKit self-host di VPS (kalau kuota free tier habis)
9. Auto-start permission guide untuk HP Xiaomi/Oppo

---

## Konvensi Kerja

1. Git Bash untuk semua eksekusi
2. Commit kecil, tunggu CI hijau sebelum lanjut batch
3. Verifikasi brace balance sebelum commit Kotlin
4. Update HANDOFF setiap batch fitur
5. Bahasa Indonesia, tabel ringkas, step-by-step
6. Jangan paste token/password ke chat
7. Retrofit: Map<String, @JvmSuppressWildcards Any> untuk Map nilai campuran
8. LazyColumn: hindari nested scroll, pakai rememberSaveable bukan remember
9. Node restart: kill parent + child, spawn manual, verifikasi stderr kosong
10. Peringatkan kalau kuota chat ~80%

---

## Verifikasi Akhir Sesi

- [x] Form skrining buka tanpa crash
- [x] Multi-step 1-7 jalan
- [x] Validasi per step
- [x] Preview ringkasan
- [x] Submit ke server berhasil
- [x] Data tersimpan di WP (post 1482)
- [x] Metabox tampil di WP Admin
- [x] Server Node.js sehat (1 PID, stderr kosong)
- [x] Semua CI hijau
- [x] Semua commit sudah push ke GitHub

Sesi ini SELESAI. Lanjut di chat baru dengan baca HANDOFF.md + HANDOFF-RADIO.md + file ini.
