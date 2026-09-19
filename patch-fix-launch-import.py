#!/usr/bin/env python3
"""
Fix: tambah import kotlinx.coroutines.launch di FindJamaah.kt
Error asli: "Unresolved reference: launch" di baris 219
"""
import sys, os, shutil

def detect_app():
    for p in ("android-app/app", "app"):
        if os.path.isdir(os.path.join(p, "src/main")):
            return p
    print("ERROR: jalankan dari root repo"); sys.exit(1)

def read(p):
    with open(p, "r", encoding="utf-8", newline="") as f:
        return f.read().replace("\r\n", "\n").replace("\r", "\n")

def write(p, c):
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(c)

APP = detect_app()
F = f"{APP}/src/main/java/com/imtiyaztour/app/FindJamaah.kt"

if not os.path.isfile(F):
    print(f"ERROR: {F} tidak ada"); sys.exit(1)

c = read(F)

# Cek apakah sudah ada
if "import kotlinx.coroutines.launch" in c:
    print("SKIP: import kotlinx.coroutines.launch sudah ada")
    sys.exit(0)

# Pola target: import Dispatchers diikuti delay
target = "import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.delay\n"
if target not in c:
    print("ERROR: pola import tidak ditemukan")
    print("  Cari: " + repr(target))
    sys.exit(2)

replacement = "import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.delay\nimport kotlinx.coroutines.launch\n"

shutil.copy(F, F + ".bak6")
print(f"Backup: {F}.bak6")

c = c.replace(target, replacement, 1)
write(F, c)
print("OK: import kotlinx.coroutines.launch ditambahkan")
print("\nSelanjutnya:")
print("  del /S /Q android-app\\app\\*.bak6")
print("  git add . && git commit -m \"fix: import launch di FindJamaah.kt\" && git push origin main")