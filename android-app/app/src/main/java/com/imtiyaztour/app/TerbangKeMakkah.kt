package com.imtiyaztour.app

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

// ============================================================================
// SIMULASI PENERBANGAN UMRAH "TERBANG KE MAKKAH"
// IMTIYAZ AIRLINE — flight IZ-6711 · YIA Yogyakarta → JED King Abdul Aziz
// 9 jam 50 menit · 38.000 kaki · rute via Selat Sunda, Malaka, Andaman,
// Teluk Benggala, India, Arab Saudi. Tanpa "kecelakaan" — pesawat selalu
// mendarat dengan selamat. Fokus: pengalaman spiritual perjalanan umrah.
// ============================================================================

private val SKY_TOP = Color(0xFF1E1B4B)
private val SKY_MID1 = Color(0xFF7E22CE)
private val SKY_MID2 = Color(0xFFEC4899)
private val SKY_BOTTOM = Color(0xFFFED7AA)
private val SILHOUETTE_NEAR = Color(0xFF0F172A)
private val SILHOUETTE_FAR = Color(0xFF312E81)
private val METEOR_RED = Color(0xFFDC2626)
private val METEOR_ORANGE = Color(0xFFF97316)
private val METEOR_YELLOW = Color(0xFFFCD34D)
private val SHIELD_COLOR = Color(0xFF06B6D4)
private val COIN_COLOR = Color(0xFFFBBF24)
private val COIN_EDGE = Color(0xFFB45309)
private val BRAND_GREEN = Color(0xFF0F7A5A)
private val BRAND_BLUE = Color(0xFF1E40AF)
private val WARNING_RED = Color(0xFFDC2626)
private val GOLD = Color(0xFFFFD700)
private val INFO_BLUE = Color(0xFF0EA5E9)

private const val FLIGHT_TOTAL_SECONDS = 35400f
private const val FLIGHT_TOTAL_KM = 8000f
private const val REAL_TO_GAME_TIME = 120f
private const val ALTITUDE_FT = 38000
private const val PLANE_Y = 0.75f
private const val PLANE_R = 0.035f
private const val FLIGHT_NO = "IZ-6711"

private data class Meteor(var x: Float, var y: Float, val radius: Float, var nearMissed: Boolean = false)
private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float, val phase: Float)
private data class Mountain(val x: Float, val height: Float)
private data class Cloud(var x: Float, var y: Float, val size: Float)
private data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val maxLife: Float, val color: Color, val radius: Float)
private data class Coin(var x: Float, var y: Float, var taken: Boolean)
private data class Badge(val id: String, val emoji: String, val title: String, val minKm: Int)
private data class Waypoint(val km: Float, val nama: String, val tts: String)

private val BADGES = listOf(
    Badge("penerbang_muda", "🛫", "Penerbang Muda", 1000),
    Badge("penerbang_handal", "✈️", "Penerbang Handal", 3000),
    Badge("penerbang_ulung", "🚀", "Penerbang Ulung", 6000),
    Badge("sampai_makkah", "🕋", "Sampai Makkah", 8000),
)

// Rute YIA → JED (10 waypoint)
private val ROUTE = listOf(
    Waypoint(0f, "YIA Yogyakarta", "Pesawat IMTIYAZ AIRLINE lepas landas dari Yogyakarta menuju Jeddah. Selamat terbang, semoga menjadi umrah yang mabrur."),
    Waypoint(400f, "Selat Sunda", "Melewati Selat Sunda."),
    Waypoint(1200f, "Selat Malaka", "Melewati Selat Malaka, perairan Indonesia."),
    Waypoint(2400f, "Laut Andaman", "Memasuki wilayah udara internasional, melewati Laut Andaman."),
    Waypoint(3500f, "Teluk Benggala", "Melewati Teluk Benggala."),
    Waypoint(4800f, "India Selatan", "Melewati langit India Selatan."),
    Waypoint(6200f, "Laut Arab", "Melewati Laut Arab menuju Jazirah Arab."),
    Waypoint(7300f, "Oman (Maskat)", "Memasuki wilayah udara Oman."),
    Waypoint(7700f, "Arab Saudi", "Memasuki wilayah udara Kerajaan Arab Saudi."),
    Waypoint(7950f, "Jeddah Mendekat", "Pesawat mulai menurun. Bersiap mendarat di Bandara Internasional King Abdul Aziz, Jeddah."),
)

private enum class GameState {
    MENU,
    BANDARA_PINTU, BANDARA_CHECKIN, BANDARA_SCREENING, BANDARA_IMIGRASI, BANDARA_BOARDING,
    PLAYING, PAUSED, FINISHED
}

private object TkmPrefs {
    private const val NAME = "terbang_ke_makkah"
    private fun get(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun getHighScore(ctx: Context): Int = get(ctx).getInt("highscore", 0)
    fun saveHighScore(ctx: Context, score: Int) { get(ctx).edit().putInt("highscore", score).apply() }
    fun getBadges(ctx: Context): Set<String> = get(ctx).getStringSet("badges", emptySet()) ?: emptySet()
    fun unlockBadge(ctx: Context, id: String) {
        get(ctx).edit().putStringSet("badges", getBadges(ctx) + id).apply()
    }
}

private fun fmtTime(gameSeconds: Float): String {
    val totalMin = (gameSeconds / 60f).toInt()
    return "${totalMin / 60}j ${(totalMin % 60).toString().padStart(2, '0')}m"
}

@Composable
fun TerbangKeMakkahScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // TTS init sekali
    DisposableEffect(Unit) {
        TerbangKeMakkahTts.init(context)
        onDispose { TerbangKeMakkahTts.stop() }
    }

    var state by remember { mutableStateOf(GameState.MENU) }
    var highScore by remember { mutableIntStateOf(TkmPrefs.getHighScore(context)) }
    var badges by remember { mutableStateOf(TkmPrefs.getBadges(context)) }
    var newBadge by remember { mutableStateOf<Badge?>(null) }
    var distanceReached by remember { mutableIntStateOf(0) }
    var coinsCollected by remember { mutableIntStateOf(0) }
    var scoreExperience by remember { mutableIntStateOf(0) }

    // ============ BANDARA states ============
    // (state machine di dalam layar komponen bandara — lihat AirportFaseScreen)

    // ============ FLIGHT states ============
    var gameSeconds by remember { mutableFloatStateOf(0f) }
    var planeX by remember { mutableFloatStateOf(0.5f) }
    var targetX by remember { mutableFloatStateOf(0.5f) }
    var shieldUntil by remember { mutableLongStateOf(0L) }
    var nextShieldKm by remember { mutableFloatStateOf(500f) }
    var shakeUntil by remember { mutableLongStateOf(0L) }
    var lastWaypointIdx by remember { mutableIntStateOf(-1) }
    var announcement by remember { mutableStateOf<String?>(null) }
    var announcementUntil by remember { mutableLongStateOf(0L) }
    var warningUntil by remember { mutableLongStateOf(0L) }
    var nextWarningAt by remember { mutableFloatStateOf(50f) }
    var comboText by remember { mutableStateOf<String?>(null) }
    var comboUntil by remember { mutableLongStateOf(0L) }

    val meteors = remember { mutableStateListOf<Meteor>() }
    val coins = remember { mutableStateListOf<Coin>() }
    val particles = remember { mutableStateListOf<Particle>() }

    var bgScroll by remember { mutableFloatStateOf(0f) }
    var midScroll by remember { mutableFloatStateOf(0f) }
    var fgScroll by remember { mutableFloatStateOf(0f) }

    val stars = remember {
        List(70) {
            Star(
                x = Random.nextFloat(), y = Random.nextFloat() * 0.62f,
                radius = 0.8f + Random.nextFloat() * 2f,
                alpha = 0.3f + Random.nextFloat() * 0.6f,
                phase = Random.nextFloat() * 6.28f
            )
        }
    }
    val mountains = remember {
        List(20) { Mountain(x = it / 19f, height = 0.3f + Random.nextFloat() * 0.7f) }
    }
    val clouds = remember {
        List(6) {
            Cloud(x = Random.nextFloat(), y = 0.1f + Random.nextFloat() * 0.45f,
                size = 0.08f + Random.nextFloat() * 0.12f)
        }
    }

    fun resetFlight() {
        gameSeconds = 0f; planeX = 0.5f; targetX = 0.5f
        shieldUntil = 0L; nextShieldKm = 500f; shakeUntil = 0L
        lastWaypointIdx = -1; nextWarningAt = 50f
        meteors.clear(); coins.clear(); particles.clear()
        bgScroll = 0f; midScroll = 0f; fgScroll = 0f
        warningUntil = 0L; announcement = null; announcementUntil = 0L
        comboText = null; comboUntil = 0L
        coinsCollected = 0; scoreExperience = 0
    }

    fun finishFlight() {
        distanceReached = 8000
        state = GameState.FINISHED
        scoreExperience = coinsCollected * 50
        if (scoreExperience > highScore) {
            highScore = scoreExperience
            TkmPrefs.saveHighScore(context, scoreExperience)
        }
        BADGES.firstOrNull { !badges.contains(it.id) && distanceReached >= it.minKm }?.let { b ->
            TkmPrefs.unlockBadge(context, b.id)
            badges = TkmPrefs.getBadges(context)
            newBadge = b
        }
        TerbangKeMakkahTts.speak("Alhamdulillah, pesawat mendarat dengan selamat di Bandara Internasional King Abdul Aziz, Jeddah. Selamat datang di Tanah Suci. Semoga ibadah Anda diterima Allah Subhanahu wa Ta'ala.")
    }

    // Flight loop
    LaunchedEffect(state) {
        if (state != GameState.PLAYING) return@LaunchedEffect
        var last = 0L
        while (state == GameState.PLAYING) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                last = now
                if (dt <= 0f || dt > 0.1f) return@withFrameNanos

                gameSeconds += dt * REAL_TO_GAME_TIME
                val progress = (gameSeconds / FLIGHT_TOTAL_SECONDS).coerceIn(0f, 1f)
                val distanceKm = progress * FLIGHT_TOTAL_KM
                val nowMs = System.currentTimeMillis()

                // Plane movement
                val maxSpeed = 1.5f
                planeX = (planeX + (targetX - planeX).coerceIn(-maxSpeed * dt, maxSpeed * dt))
                    .coerceIn(0.08f, 0.92f)

                // Parallax
                bgScroll = (bgScroll + dt * 0.02f) % 1f
                midScroll = (midScroll + dt * 0.10f) % 1f
                fgScroll = (fgScroll + dt * 0.28f) % 1f
                clouds.forEach { c ->
                    c.y += dt * 0.18f
                    if (c.y > 1.15f) { c.y = -0.12f; c.x = Random.nextFloat() }
                }

                // Waypoint milestone
                val idx = ROUTE.indexOfLast { distanceKm >= it.km }
                if (idx > lastWaypointIdx) {
                    lastWaypointIdx = idx
                    val wp = ROUTE[idx]
                    announcement = "✈️ ${wp.nama}"
                    announcementUntil = nowMs + 3500
                    TerbangKeMakkahTts.speak(wp.tts)
                }

                // Shield auto
                if (distanceKm >= nextShieldKm && shieldUntil < nowMs) {
                    shieldUntil = nowMs + 5000
                    nextShieldKm += 1500f
                    announcement = "🛡️ Shield aktif 5 detik!"
                    announcementUntil = nowMs + 2000
                }

                // Meteors
                val mSpeed = 0.40f + progress * 0.55f
                meteors.forEach { m -> m.y += mSpeed * dt * (1f + m.radius * 3f) }
                meteors.removeAll { it.y > 1.2f }

                val spawnRate = 0.014f + progress * 0.025f
                if (Random.nextFloat() < spawnRate) {
                    meteors.add(Meteor(
                        x = 0.05f + Random.nextFloat() * 0.9f,
                        y = -0.08f,
                        radius = 0.03f + Random.nextFloat() * 0.03f
                    ))
                }

                // Coins
                if (Random.nextFloat() < 0.012f) {
                    coins.add(Coin(x = 0.05f + Random.nextFloat() * 0.9f, y = -0.05f, taken = false))
                }
                coins.forEach { c -> c.y += (mSpeed * 0.9f) * dt }
                coins.filter { !it.taken }.forEach { c ->
                    if (abs(c.x - planeX) < 0.06f && abs(c.y - PLANE_Y) < 0.06f) {
                        c.taken = true; coinsCollected++
                        comboText = "⭐ +50 Pahala"
                        comboUntil = nowMs + 700
                        repeat(6) {
                            particles.add(Particle(
                                x = c.x, y = c.y,
                                vx = (Random.nextFloat() - 0.5f) * 0.5f,
                                vy = -0.2f - Random.nextFloat() * 0.3f,
                                life = 0.55f, maxLife = 0.55f,
                                color = COIN_COLOR, radius = 0.009f
                            ))
                        }
                    }
                }
                coins.removeAll { it.y > 1.15f || it.taken }

                // Turbulensi (meteor tabrakan) — goyang, bukan mati
                val shieldActive = shieldUntil > nowMs
                if (!shieldActive) {
                    val hit = meteors.firstOrNull { m ->
                        hypot((m.x - planeX).toDouble(), (m.y - PLANE_Y).toDouble()) < (m.radius + PLANE_R)
                    }
                    if (hit != null) {
                        shakeUntil = nowMs + 700
                        announcement = "⚠️ TURBULENSI! Mohon kenakan sabuk pengaman."
                        announcementUntil = nowMs + 2500
                        TerbangKeMakkahTts.speak("Perhatian. Pesawat sedang mengalami turbulensi. Mohon tetap duduk dan kenakan sabuk pengaman.")
                        repeat(10) {
                            particles.add(Particle(
                                x = planeX, y = PLANE_Y,
                                vx = (Random.nextFloat() - 0.5f) * 0.9f,
                                vy = (Random.nextFloat() - 0.5f) * 0.9f,
                                life = 0.65f, maxLife = 0.65f,
                                color = if (Random.nextBoolean()) METEOR_ORANGE else Color.White,
                                radius = 0.011f
                            ))
                        }
                        meteors.removeAll { m ->
                            hypot((m.x - planeX).toDouble(), (m.y - PLANE_Y).toDouble()) < 0.15
                        }
                    }
                } else {
                    meteors.removeAll { m ->
                        hypot((m.x - planeX).toDouble(), (m.y - PLANE_Y).toDouble()) < (m.radius + PLANE_R + 0.04f)
                    }
                }

                // Near-miss
                meteors.forEach { m ->
                    if (!m.nearMissed && m.y in 0f..1f) {
                        val d = hypot((m.x - planeX).toDouble(), (m.y - PLANE_Y).toDouble())
                        val thr = (m.radius + PLANE_R)
                        if (d > thr && d < thr + 0.05f) {
                            m.nearMissed = true
                            comboText = "✨ Near miss! +10"
                            comboUntil = nowMs + 900
                            coinsCollected++  // bonus kecil
                        }
                    }
                }

                // Particles
                particles.forEach { p ->
                    p.x += p.vx * dt; p.y += p.vy * dt
                    p.vy += 0.3f * dt; p.life -= dt
                }
                particles.removeAll { it.life <= 0f }

                // Meteor warning
                if (gameSeconds >= nextWarningAt && warningUntil < nowMs) {
                    warningUntil = nowMs + 6000
                    nextWarningAt = gameSeconds + 140f
                    repeat(8) {
                        meteors.add(Meteor(
                            x = 0.05f + Random.nextFloat() * 0.9f,
                            y = -0.1f - Random.nextFloat() * 0.3f,
                            radius = 0.03f + Random.nextFloat() * 0.03f
                        ))
                    }
                    TerbangKeMakkahTts.speak("Perhatian seluruh penumpang. Diperkirakan akan terjadi hujan meteor. Mohon kenakan sabuk pengaman dan jangan menggunakan kamar mandi selama hujan meteor berlangsung.")
                }

                if (gameSeconds >= FLIGHT_TOTAL_SECONDS) finishFlight()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (state) {
            GameState.MENU -> MenuScreen(
                highScore = highScore, badges = badges,
                onStart = {
                    resetFlight()
                    state = GameState.BANDARA_PINTU
                },
                onBack = onBack
            )
            GameState.BANDARA_PINTU, GameState.BANDARA_CHECKIN,
            GameState.BANDARA_SCREENING, GameState.BANDARA_IMIGRASI,
            GameState.BANDARA_BOARDING -> AirportFaseScreen(
                state = state,
                onBack = onBack,
                onNext = { st -> state = st }
            )
            GameState.PLAYING -> {
                // Canvas game
                Canvas(
                    modifier = Modifier.fillMaxSize().pointerInput(state) {
                        if (state != GameState.PLAYING) return@pointerInput
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press || event.type == PointerEventType.Move) {
                                    val x = event.changes.firstOrNull()?.position?.x
                                    if (x != null) targetX = (x / size.width).coerceIn(0.08f, 0.92f)
                                }
                            }
                        }
                    }
                ) {
                    val W = size.width; val H = size.height
                    drawSky()
                    drawStarsT(stars, W, H)
                    drawMoon(W, H)
                    drawMountainsLayer(mountains, midScroll, W, H, SILHOUETTE_FAR, 0.78f, 0.055f)
                    drawMountainsLayer(mountains, fgScroll, W, H, SILHOUETTE_NEAR, 0.85f, 0.095f)
                    drawCloudsC(clouds, W, H)
                    drawCoinsC(coins, W, H)
                    drawMeteorsC(meteors, W, H)
                    drawParticlesC(particles, W, H)
                    val shake = if (System.currentTimeMillis() < shakeUntil) {
                        ((System.currentTimeMillis() % 40) - 20) / 8f
                    } else 0f
                    drawPlaneV(planeX + shake / W * 0.02f, PLANE_Y, W, H, shieldUntil > System.currentTimeMillis())
                }

                PlayingHud(
                    distanceKm = (gameSeconds / FLIGHT_TOTAL_SECONDS * FLIGHT_TOTAL_KM).toInt(),
                    gameSeconds = gameSeconds,
                    coins = coinsCollected,
                    shieldActive = shieldUntil > System.currentTimeMillis(),
                    onPause = { state = GameState.PAUSED }
                )

                if (announcement != null && System.currentTimeMillis() < announcementUntil) {
                    AnnouncementBanner(announcement!!)
                }
                if (System.currentTimeMillis() < warningUntil) {
                    MeteorWarningBanner()
                }
                if (comboText != null && System.currentTimeMillis() < comboUntil) {
                    ComboBanner(comboText!!)
                }
            }
            GameState.PAUSED -> PausedOverlay(
                onResume = { state = GameState.PLAYING },
                onMenu = { state = GameState.MENU },
                onBack = onBack
            )
            GameState.FINISHED -> FinishedOverlay(
                coins = coinsCollected,
                score = scoreExperience,
                highScore = highScore,
                newBadge = newBadge,
                onMenu = { newBadge = null; state = GameState.MENU },
                onBack = onBack
            )
        }
    }
}

// ============ FASE BANDARA ============
@Composable
private fun AirportFaseScreen(
    state: GameState,
    onBack: () -> Unit,
    onNext: (GameState) -> Unit
) {
    val context = LocalContext.current
    val pages = listOf(
        AirportPage(
            title = "YOGYAKARTA INTERNATIONAL AIRPORT",
            subtitle = "Pintu Masuk · YIA",
            icon = "🛫",
            bullets = listOf(
                "Selamat datang, Jamaah Imtiyaz Tour",
                "Harap siapkan paspor, tiket, dan dokumen vaksin",
                "Check-in dibuka 3 jam sebelum keberangkatan",
                "Bagasi kabin maks 7 kg · bagasi utama maks 30 kg"
            ),
            tts = "Selamat datang di Bandara Internasional Yogyakarta. Jamaah Imtiyaz Tour, mohon siapkan paspor, tiket, dan dokumen vaksin Anda.",
            nextState = GameState.BANDARA_CHECKIN
        ),
        AirportPage(
            title = "CHECK-IN COUNTER",
            subtitle = "Konfirmasi Keberangkatan",
            icon = "🎫",
            bullets = listOf(
                "Serahkan paspor dan tiket kepada petugas",
                "Konfirmasi nomor kursi dan bagasi",
                "Simpan boarding pass dengan aman",
                "Jangan titipkan barang berharga di bagasi"
            ),
            tts = "Silakan menuju konter check-in. Serahkan paspor dan tiket Anda kepada petugas, dan konfirmasi nomor kursi Anda.",
            nextState = GameState.BANDARA_SCREENING
        ),
        AirportPage(
            title = "SCREENING & X-RAY",
            subtitle = "Pemeriksaan Keamanan",
            icon = "🔍",
            bullets = listOf(
                "LEPAS: jaket, ikat pinggang, jam tangan, topi, sepatu",
                "KELUARKAN dari tas: laptop, HP, tablet, power bank",
                "AIR MINUM: maksimal 100 ml per wadah · total 1 liter",
                "DILARANG: benda tajam, gunting, korek api, petasan",
                "OBAT: sertakan resep dokter untuk obat keras",
                "Makanan padat diperbolehkan"
            ),
            tts = "Mohon lepaskan jaket, ikat pinggang, jam tangan, topi, dan sepatu Anda. Keluarkan laptop, telepon seluler, dan power bank dari tas. Air minum maksimal 100 mililiter per wadah. Benda tajam dan korek api dilarang. Terima kasih atas kerja samanya.",
            nextState = GameState.BANDARA_IMIGRASI
        ),
        AirportPage(
            title = "IMIGRASI",
            subtitle = "Verifikasi Identitas",
            icon = "🛂",
            bullets = listOf(
                "Serahkan paspor dan visa umrah",
                "Verifikasi sidik jari dan wajah",
                "Jawab pertanyaan petugas dengan jujur",
                "Pastikan stempel keluar sudah tercetak"
            ),
            tts = "Silakan menuju konter imigrasi. Serahkan paspor dan visa umrah Anda. Ikuti proses verifikasi sidik jari dan wajah.",
            nextState = GameState.BANDARA_BOARDING
        ),
        AirportPage(
            title = "BOARDING GATE",
            subtitle = "Menuju Pesawat",
            icon = "🚪",
            bullets = listOf(
                "Penerbangan: IMTIYAZ AIRLINE $FLIGHT_NO",
                "Gate boarding sesuai papan informasi",
                "Antri sesuai grup kursi (A/B/C/D)",
                "Prioritas: lansia, ibu hamil, disabilitas",
                "Selamat jalan, semoga menjadi umrah mabrur"
            ),
            tts = "Penerbangan IMTIYAZ AIRLINE nomor IZ 6711 telah siap boarding. Silakan menuju gate sesuai papan informasi. Prioritas diberikan kepada lansia, ibu hamil, dan penumpang dengan kebutuhan khusus. Selamat jalan, semoga menjadi umrah yang mabrur.",
            nextState = GameState.PLAYING
        ),
    )

    val pageIdx = when (state) {
        GameState.BANDARA_PINTU -> 0
        GameState.BANDARA_CHECKIN -> 1
        GameState.BANDARA_SCREENING -> 2
        GameState.BANDARA_IMIGRASI -> 3
        GameState.BANDARA_BOARDING -> 4
        else -> 0
    }
    val page = pages[pageIdx]

    // TTS auto per halaman
    LaunchedEffect(pageIdx) {
        TerbangKeMakkahTts.speak(page.tts)
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(SKY_TOP, SKY_MID1, BRAND_BLUE)))
    ) {
        Column(
            Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Text(page.icon, fontSize = 72.sp)
            Spacer(Modifier.height(12.dp))
            Text(page.title, color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = 22.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(page.subtitle, color = GOLD, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    page.bullets.forEach { b ->
                        Row(Modifier.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.Top) {
                            Text("✓", color = BRAND_GREEN, fontWeight = FontWeight.Bold,
                                fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(b, fontSize = 12.5.sp, color = Color(0xFF1F2937))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onNext(page.nextState) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BRAND_GREEN),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (page.nextState == GameState.PLAYING) "NAIK PESAWAT" else "LANJUT",
                    fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Kembali", color = Color.White)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

private data class AirportPage(
    val title: String, val subtitle: String, val icon: String,
    val bullets: List<String>, val tts: String, val nextState: GameState
)

// ============ MENU ============
@Composable
private fun MenuScreen(
    highScore: Int, badges: Set<String>,
    onStart: () -> Unit, onBack: () -> Unit
) {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(SKY_TOP, SKY_MID1, SKY_MID2, SKY_BOTTOM))),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✈️", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text("IMTIYAZ AIRLINE", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text("TERBANG KE MAKKAH", color = GOLD,
                fontWeight = FontWeight.Bold, fontSize = 26.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text("YIA Yogyakarta → King Abdul Aziz Jeddah",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp, textAlign = TextAlign.Center)
            Text("Flight $FLIGHT_NO · 9 jam 50 menit · 38.000 kaki",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text("Simulasi perjalanan umrah — dari bandara,\nterbang melewati langit dunia,\nsampai mendarat di Tanah Suci.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆 BEST PAHALA", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    Text("$highScore", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("Badge", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BADGES.filter { badges.contains(it.id) }.forEach { b ->
                        Text(b.emoji, fontSize = 22.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BRAND_GREEN),
                shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("MULAI PERJALANAN", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Kembali", color = Color.White)
            }
        }
    }
}

// ============ HUD & OVERLAYS ============
@Composable
private fun PlayingHud(
    distanceKm: Int, gameSeconds: Float, coins: Int,
    shieldActive: Boolean, onPause: () -> Unit
) {
    val progress = (distanceKm.toFloat() / FLIGHT_TOTAL_KM).coerceIn(0f, 1f)
    val timeStr = fmtTime(gameSeconds)
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("IMTIYAZ AIRLINE", fontSize = 9.sp, color = GOLD, fontWeight = FontWeight.Bold)
                Text("$FLIGHT_NO · ${ALTITUDE_FT / 1000},000 ft",
                    fontSize = 10.sp, color = Color.White)
            }
            if (shieldActive) { Spacer(Modifier.width(8.dp)); Text("🛡️", fontSize = 14.sp) }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("YIA ✈ JED", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Pause, contentDescription = "Pause",
                    tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$distanceKm km", fontSize = 11.sp, color = Color.White,
                fontWeight = FontWeight.Bold)
            Text("$timeStr / 9j 50m", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
            Text("${(progress * 100).toInt()}%", fontSize = 11.sp, color = Color.White,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(5.dp)
            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
        ) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight()
                .background(BRAND_GREEN, RoundedCornerShape(3.dp)))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("⭐ Pahala: ${coins * 50}", fontSize = 12.sp, color = GOLD,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AnnouncementBanner(text: String) {
    Box(Modifier.fillMaxSize().padding(top = 150.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter) {
        Surface(color = Color.Black.copy(alpha = 0.78f), shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📢", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(text, color = Color.White, fontSize = 11.5.sp)
            }
        }
    }
}

@Composable
private fun MeteorWarningBanner() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(color = WARNING_RED.copy(alpha = 0.92f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(20.dp)) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️ PERINGATAN HUJAN METEOR", color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Mohon kenakan sabuk pengaman.\nJangan gunakan kamar mandi\nselama hujan meteor berlangsung.",
                    color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ComboBanner(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(color = BRAND_GREEN.copy(alpha = 0.92f), shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(bottom = 80.dp)) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit, onMenu: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⏸️", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text("PENERBANGAN DIJEDA", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onResume, modifier = Modifier.width(220.dp).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BRAND_GREEN),
                shape = RoundedCornerShape(12.dp)) {
                Text("LANJUTKAN", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onMenu, modifier = Modifier.width(220.dp).height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                Text("MENU UTAMA", color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack) {
                Text("Keluar", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FinishedOverlay(
    coins: Int, score: Int, highScore: Int, newBadge: Badge?,
    onMenu: () -> Unit, onBack: () -> Unit
) {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(SKY_TOP, SKY_MID1, SKY_MID2))),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🕋", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text("ALHAMDULILLAH", color = GOLD, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text("Pesawat mendarat dengan selamat\ndi Bandara Internasional King Abdul Aziz",
                color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("9 jam 50 menit · 8.000 km · 38.000 kaki",
                color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
            Spacer(Modifier.height(18.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⭐ PAHALA TERKUMPUL", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                    Text("$score", color = GOLD, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("🏆 Best: $highScore", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    Text("⭐ ${coins} kenangan terkumpul", color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp)
                }
            }
            if (newBadge != null) {
                Spacer(Modifier.height(14.dp))
                Card(colors = CardDefaults.cardColors(containerColor = GOLD.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(newBadge.emoji, fontSize = 30.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("BADGE BARU!", color = Color.Black.copy(alpha = 0.6f), fontSize = 10.sp)
                            Text(newBadge.title, color = Color.Black,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Selamat datang di Tanah Suci.\nSemoga ibadah Anda diterima Allah SWT.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onMenu, modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BRAND_GREEN),
                shape = RoundedCornerShape(12.dp)) {
                Text("KEMBALI KE MENU", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack) {
                Text("Keluar", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

// ============ DRAW HELPERS ============
private fun DrawScope.drawSky() {
    drawRect(brush = Brush.verticalGradient(
        colors = listOf(SKY_TOP, SKY_MID1, SKY_MID2, SKY_BOTTOM),
        startY = 0f, endY = size.height
    ))
}

private fun DrawScope.drawStarsT(stars: List<Star>, W: Float, H: Float) {
    val t = System.currentTimeMillis() / 1000f
    stars.forEach { s ->
        val twinkle = (0.5f + 0.5f * sin(t * 1.7f + s.phase)).coerceIn(0f, 1f)
        val a = (s.alpha * (0.5f + 0.5f * twinkle)).coerceIn(0f, 1f)
        drawCircle(Color.White.copy(alpha = a), s.radius, Offset(s.x * W, s.y * H))
    }
}

private fun DrawScope.drawMoon(W: Float, H: Float) {
    val c = Offset(W * 0.82f, H * 0.14f)
    drawCircle(Color.White.copy(alpha = 0.10f), 46.dp.toPx(), c)
    drawCircle(Color.White, 26.dp.toPx(), c)
    drawCircle(SKY_MID1.copy(alpha = 0.55f), 8.dp.toPx(),
        Offset(c.x - 6.dp.toPx(), c.y - 4.dp.toPx()))
    drawCircle(SKY_MID1.copy(alpha = 0.4f), 4.dp.toPx(),
        Offset(c.x + 7.dp.toPx(), c.y + 5.dp.toPx()))
}

private fun DrawScope.drawMountainsLayer(
    mountains: List<Mountain>, offset: Float, W: Float, H: Float,
    color: Color, baseY: Float, peakScale: Float
) {
    val horizonY = H * baseY
    val path = Path().apply {
        moveTo(0f, H)
        lineTo(0f, horizonY)
        for (i in -1..mountains.size) {
            val m = mountains[((i % mountains.size) + mountains.size) % mountains.size]
            val x = ((i + offset) / mountains.size.toFloat()) * W * 2f - W * 0.5f
            lineTo(x, horizonY - m.height * H * peakScale)
        }
        lineTo(W, horizonY)
        lineTo(W, H)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawCloudsC(clouds: List<Cloud>, W: Float, H: Float) {
    clouds.forEach { c ->
        val cx = c.x * W; val cy = c.y * H; val r = c.size * W
        drawCircle(Color.White.copy(alpha = 0.20f), r, Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.15f), r * 0.7f, Offset(cx - r * 0.6f, cy + r * 0.15f))
        drawCircle(Color.White.copy(alpha = 0.15f), r * 0.8f, Offset(cx + r * 0.7f, cy + r * 0.1f))
    }
}

private fun DrawScope.drawCoinsC(coins: List<Coin>, W: Float, H: Float) {
    coins.forEach { c ->
        if (c.taken) return@forEach
        val cx = c.x * W; val cy = c.y * H
        val r = 9.dp.toPx()
        drawCircle(COIN_EDGE, r, Offset(cx, cy))
        drawCircle(COIN_COLOR, r * 0.85f, Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.4f), r * 0.3f, Offset(cx - r * 0.25f, cy - r * 0.25f))
    }
}

private fun DrawScope.drawMeteorsC(meteors: List<Meteor>, W: Float, H: Float) {
    meteors.forEach { m ->
        val cx = m.x * W; val cy = m.y * H; val r = m.radius * W
        for (i in 1..6) {
            drawCircle(
                color = METEOR_ORANGE.copy(alpha = (0.5f - i * 0.07f).coerceAtLeast(0f)),
                radius = r * (1f - i * 0.10f),
                center = Offset(cx, cy - i * 7.dp.toPx())
            )
        }
        drawCircle(METEOR_RED, r, Offset(cx, cy))
        drawCircle(METEOR_ORANGE, r * 0.72f, Offset(cx, cy))
        drawCircle(METEOR_YELLOW, r * 0.32f, Offset(cx, cy))
    }
}

private fun DrawScope.drawParticlesC(particles: List<Particle>, W: Float, H: Float) {
    particles.forEach { p ->
        val a = (p.life / p.maxLife).coerceIn(0f, 1f)
        drawCircle(p.color.copy(alpha = a), p.radius * W, Offset(p.x * W, p.y * H))
    }
}

private fun DrawScope.drawPlaneV(x: Float, y: Float, W: Float, H: Float, shield: Boolean) {
    val pcx = x * W; val pcy = y * H; val ps = 22.dp.toPx()

    drawCircle(Color.White.copy(alpha = 0.25f), ps * 0.4f, Offset(pcx, pcy + ps * 1.15f))
    drawCircle(Color.White.copy(alpha = 0.15f), ps * 0.3f, Offset(pcx, pcy + ps * 1.45f))
    drawCircle(Color.White.copy(alpha = 0.08f), ps * 0.2f, Offset(pcx, pcy + ps * 1.75f))

    if (shield) {
        drawCircle(SHIELD_COLOR.copy(alpha = 0.30f), ps * 1.8f, Offset(pcx, pcy))
        drawCircle(SHIELD_COLOR.copy(alpha = 0.9f), ps * 1.8f, Offset(pcx, pcy),
            style = Stroke(width = 2.5.dp.toPx()))
    }

    drawRoundRect(
        color = Color.White,
        topLeft = Offset(pcx - ps * 0.20f, pcy - ps * 1.00f),
        size = Size(ps * 0.40f, ps * 2.00f),
        cornerRadius = CornerRadius(ps * 0.20f, ps * 0.20f)
    )

    val nose = Path().apply {
        moveTo(pcx, pcy - ps * 1.18f)
        lineTo(pcx - ps * 0.20f, pcy - ps * 0.75f)
        lineTo(pcx + ps * 0.20f, pcy - ps * 0.75f)
        close()
    }
    drawPath(nose, Color.White)

    val wingL = Path().apply {
        moveTo(pcx - ps * 0.20f, pcy)
        lineTo(pcx - ps * 1.15f, pcy + ps * 0.45f)
        lineTo(pcx - ps * 0.20f, pcy + ps * 0.30f)
        close()
    }
    drawPath(wingL, Color(0xFFF1F5F9))

    val wingR = Path().apply {
        moveTo(pcx + ps * 0.20f, pcy)
        lineTo(pcx + ps * 1.15f, pcy + ps * 0.45f)
        lineTo(pcx + ps * 0.20f, pcy + ps * 0.30f)
        close()
    }
    drawPath(wingR, Color(0xFFF1F5F9))

    val tailL = Path().apply {
        moveTo(pcx - ps * 0.20f, pcy + ps * 0.75f)
        lineTo(pcx - ps * 0.60f, pcy + ps * 0.98f)
        lineTo(pcx - ps * 0.20f, pcy + ps * 1.05f)
        close()
    }
    drawPath(tailL, Color(0xFFF1F5F9))

    val tailR = Path().apply {
        moveTo(pcx + ps * 0.20f, pcy + ps * 0.75f)
        lineTo(pcx + ps * 0.60f, pcy + ps * 0.98f)
        lineTo(pcx + ps * 0.20f, pcy + ps * 1.05f)
        close()
    }
    drawPath(tailR, Color(0xFFF1F5F9))

    drawCircle(BRAND_GREEN, ps * 0.18f, Offset(pcx, pcy - ps * 0.45f))

    repeat(3) { i ->
        drawCircle(Color(0x553B82F6), ps * 0.055f,
            Offset(pcx, pcy - ps * 0.05f + i * ps * 0.28f))
    }

    drawCircle(GOLD, ps * 0.08f, Offset(pcx, pcy + ps * 0.88f))
}
