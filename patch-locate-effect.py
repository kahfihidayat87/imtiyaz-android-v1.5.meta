#!/usr/bin/env python3
"""
Menambahkan LaunchedEffect LocateService.start() di MainActivity.kt
Idempotent -- aman dijalankan berulang kali.
Menangani beberapa varian isi LaunchedEffect yang sudah ada.
"""
import sys, os, shutil

def detect_app():
    for p in ("android-app/app", "app"):
        if os.path.isdir(os.path.join(p, "src/main")):
            return p
    print("ERROR: Jalankan dari root repo"); sys.exit(1)

def read(p):
    with open(p, "r", encoding="utf-8", newline="") as f:
        return f.read().replace("\r\n", "\n").replace("\r", "\n")

def write(p, c):
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(c)

APP = detect_app()
MA = f"{APP}/src/main/java/com/imtiyaztour/app/MainActivity.kt"

if not os.path.isfile(MA):
    print(f"ERROR: {MA} tidak ada"); sys.exit(1)

print(f"File: {MA}")

c = read(MA)

# Cek apakah sudah ada
if "LocateService.start(context)" in c:
    print("SKIP: LaunchedEffect LocateService sudah ada")
    sys.exit(0)

# Varian target yang mungkin ada -- coba satu per satu
targets = [
    # Varian dengan checkAnnouncements (v2.11.0 + announcements)
    ('''    LaunchedEffect(Unit) {
        try { ReminderScheduler.rescheduleAll(context) } catch (e: Exception) {}
        try { checkAnnouncements(context) } catch (e: Exception) {}
    }''',
     "with-announcements"),
    # Varian basic
    ('''    LaunchedEffect(Unit) {
        try { ReminderScheduler.rescheduleAll(context) } catch (e: Exception) {}
    }''',
     "basic"),
    # Varian dengan komentar
    ('''    LaunchedEffect(Unit) {
        try { ReminderScheduler.rescheduleAll(context) } catch (e: Exception) {}
        // Fallback comment
    }''',
     "with-comment"),
]

matched_target = None
matched_label = None
for t, label in targets:
    if t in c:
        matched_target = t
        matched_label = label
        break

if matched_target is None:
    print("ERROR: Tidak menemukan varian LaunchedEffect ReminderScheduler yang cocok.")
    print("\nJalankan perintah ini dan kirim hasilnya ke developer:")
    print('  powershell -Command "Get-Content MainActivity.kt | Select-Object -Skip 375 -First 20"')
    sys.exit(2)

print(f"Varian match: {matched_label}")

replacement = matched_target + '''

    // Fitur Tracking Lansia: nyalakan LocateService saat login
    // (ditambahkan otomatis oleh patch-locate-effect.py)
    LaunchedEffect(Unit) {
        if (Prefs.isLoggedIn(context)) {
            try { LocateService.start(context) } catch (e: Exception) { }
        }
    }'''

shutil.copy(MA, MA + ".bak5")
print(f"Backup: {MA}.bak5")

c = c.replace(matched_target, replacement, 1)
write(MA, c)
print("OK: LaunchedEffect LocateService.start() ditambahkan")
print("\nSelanjutnya:")
print("  del /S /Q android-app\\app\\*.bak5")
print("  git add . && git commit -m \"feat: tracking lansia v2.11.0\" && git push origin main")