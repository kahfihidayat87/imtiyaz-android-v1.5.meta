    // v2.15.0 FIX: fetch is_tour_leader sekali di awal supaya gate UI (TL vs Jamaah)
    // benar SEBELUM user join gelombang. Sebelumnya hanya di-set oleh poll loop,
    // padahal poll loop baru aktif setelah join — jadi TL selalu tampil sebagai jamaah.
    LaunchedEffect(Unit) {
        try {
            val resp = withContext(Dispatchers.IO) {
                RadioApiClient.service.poll(RadioPollRequest(jamaahId, jamaahToken, 0L))
            }
            isTourLeader = resp.is_tour_leader ?: false
            myId = resp.my_jamaah_id ?: ""
        } catch (e: Exception) {
            // Diam-diam skip — bukan blocker utama. User bisa retry join.
        }
    }

    // Defensive guard: prevent listener from entering live room without code.
    fun canStartLiveSession(): Boolean {
        if (kodeGelombang.isBlank()) {
            gelombangError = "Masukkan kode gelombang terlebih dahulu"
            return false
        }
        return true
    }

    // v2.15.0: Gelombang Radio
    var kodeGelombang by remember { mutableStateOf(Prefs.getActiveKodeGelombang(context)) }
