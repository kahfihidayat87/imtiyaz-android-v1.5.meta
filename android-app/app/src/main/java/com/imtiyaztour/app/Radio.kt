                onStart = {
                    if (kodeGelombang.isBlank()) {
                        gelombangError = "Masukkan kode gelombang terlebih dahulu"
                        return@onStart
                    }
                    if (!hasMicPermission) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        RadioLiveKit.mulai(context, jamaahId, jamaahToken, kodeGelombang) { s ->
                            liveStatus = s
                            liveActive = RadioLiveKit.sedangAktif()
                        }
                    }
                },
                onStop = {
                    RadioLiveKit.berhenti()
                    liveStatus = ""
                    liveActive = false
                },
                onCreateGelombang = { nama, kode -> handleCreateGelombang(nama, kode) },
                onJoinGelombang = { kode -> handleJoinGelombang(kode) },
                onLeaveGelombang = { handleLeaveGelombang() },
                onRevokeGelombang = { handleRevokeGelombang() }
            )
        }
    }
}
