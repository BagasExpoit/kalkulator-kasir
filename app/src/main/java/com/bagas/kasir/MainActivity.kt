package com.bagas.kasir

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ======================================================
//  KALKULATOR KASIR
//  Dibuat dengan Kotlin by Bagas Orcarius
// ======================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KasirApp() }
    }
}

data class CartItem(val id: Long, val harga: Long, val qty: Int)

private val localeId: Locale = Locale.forLanguageTag("id-ID")

fun rupiah(n: Long): String =
    "Rp " + NumberFormat.getInstance(localeId).format(n)

// ---------------------------------------------------------
//  MODEL & PENYIMPANAN LAPORAN
// ---------------------------------------------------------
data class Transaksi(
    val id: Long,
    val waktu: Long,
    val bayar: Long,
    val items: List<CartItem>
) {
    val total: Long get() = items.sumOf { it.harga * it.qty }
    val totalBarang: Int get() = items.sumOf { it.qty }
    val kembali: Long get() = bayar - total
}

fun List<Transaksi>.toJson(): String {
    val arr = JSONArray()
    forEach { t ->
        val its = JSONArray()
        t.items.forEach { x -> its.put(JSONObject().put("h", x.harga).put("q", x.qty)) }
        arr.put(
            JSONObject()
                .put("id", t.id)
                .put("waktu", t.waktu)
                .put("bayar", t.bayar)
                .put("items", its)
        )
    }
    return arr.toString()
}

fun parseLaporan(json: String): List<Transaksi> = try {
    val arr = JSONArray(json)
    (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        val its = o.getJSONArray("items")
        Transaksi(
            id = o.getLong("id"),
            waktu = o.getLong("waktu"),
            bayar = o.getLong("bayar"),
            items = (0 until its.length()).map { j ->
                val x = its.getJSONObject(j)
                CartItem(j.toLong(), x.getLong("h"), x.getInt("q"))
            }
        )
    }
} catch (e: Exception) {
    emptyList()
}

fun formatWaktu(ms: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", localeId).format(Date(ms))

fun awalHariIni(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun buatTeksLaporan(judul: String, daftar: List<Transaksi>): String {
    val sb = StringBuilder()
    sb.appendLine("LAPORAN PENJUALAN - $judul")
    sb.appendLine("Kalkulator Kasir by Bagas Orcarius")
    sb.appendLine("Dicetak: ${formatWaktu(System.currentTimeMillis())}")
    sb.appendLine()
    sb.appendLine("Jumlah transaksi : ${daftar.size}")
    sb.appendLine("Barang terjual   : ${daftar.sumOf { it.totalBarang }} item")
    sb.appendLine("Total pendapatan : ${rupiah(daftar.sumOf { it.total })}")
    sb.appendLine()
    daftar.asReversed().forEachIndexed { i, t ->
        sb.appendLine("${i + 1}. ${formatWaktu(t.waktu)}")
        t.items.forEach { x ->
            sb.appendLine("   - ${rupiah(x.harga)} x ${x.qty} = ${rupiah(x.harga * x.qty)}")
        }
        sb.appendLine("   Total ${rupiah(t.total)} | Bayar ${rupiah(t.bayar)} | Kembali ${rupiah(t.kembali)}")
    }
    return sb.toString().trimEnd()
}

private val CardShape = RoundedCornerShape(14.dp)
private val FieldShape = RoundedCornerShape(10.dp)

// Tautan sosial media developer
private const val URL_FACEBOOK = "https://facebook.com/willydevops1"
private const val URL_GITHUB = "https://github.com/BagasExpoit"
private const val URL_INSTAGRAM =
    "https://www.instagram.com/bagas_samuji74?igsh=bmlscWxqN2pmZnJx"

// Path ikon (viewport 24 x 24)
private const val PATH_GITHUB =
    "M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"
private const val PATH_FACEBOOK =
    "M9.101 23.691v-7.98H6.627v-3.667h2.474v-1.58c0-4.085 1.848-5.978 5.858-5.978.401 0 .955.042 1.468.103a8.68 8.68 0 0 1 1.141.195v3.325a8.623 8.623 0 0 0-.653-.036 26.805 26.805 0 0 0-.733-.009c-.707 0-1.259.096-1.675.309a1.686 1.686 0 0 0-.679.622c-.258.42-.374.995-.374 1.752v1.297h3.919l-.386 2.103-.287 1.564h-3.246v8.245C19.396 23.238 24 18.179 24 12.044c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.628 3.874 10.35 9.101 11.647Z"

// ---------------------------------------------------------
//  TEMA
// ---------------------------------------------------------
@Composable
fun KasirTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFF80CBC4),
            onPrimary = Color(0xFF00201D),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF262626),
            onBackground = Color(0xFFEDEDED),
            onSurface = Color(0xFFEDEDED)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF00695C),
            onPrimary = Color.White,
            background = Color(0xFFF3F6F5),
            surface = Color.White,
            surfaceVariant = Color(0xFFE3EEEB),
            onBackground = Color(0xFF111111),
            onSurface = Color(0xFF111111)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

// ---------------------------------------------------------
//  ROOT
// ---------------------------------------------------------
@Composable
fun KasirApp() {
    val systemDark = isSystemInDarkTheme()
    var dark by rememberSaveable { mutableStateOf(systemDark) }

    // Warna ikon status bar & navigation bar mengikuti tema aplikasi
    val activity = LocalContext.current as ComponentActivity
    DisposableEffect(dark) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = if (dark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
        )
        onDispose { }
    }

    // Batasi skala font sistem agar tampilan tetap pas
    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density,
            fontScale = minOf(baseDensity.fontScale, 1.1f)
        )
    ) {
        KasirTheme(dark) {
            val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

            Scaffold(
                modifier = Modifier.imePadding(),
                topBar = { KasirTopBar(dark) { dark = it } },
                bottomBar = { if (!imeVisible) Footer() },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
            ) { padding ->
                KasirScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
fun KasirTopBar(dark: Boolean, onToggle: (Boolean) -> Unit) {
    val barColor = if (dark) Color(0xFF1B2927) else Color(0xFF00695C)
    val textColor = if (dark) MaterialTheme.colorScheme.primary else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(barColor)
            .statusBarsPadding()
            .height(48.dp)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "Kalkulator Kasir",
            modifier = Modifier.align(Alignment.CenterStart),
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (dark) "Gelap" else "Terang",
                color = textColor,
                fontSize = 12.sp
            )
            Spacer(Modifier.width(4.dp))
            Switch(
                checked = dark,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.75f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF1B2927),
                    checkedTrackColor = Color(0xFF80CBC4),
                    checkedBorderColor = Color(0xFF80CBC4),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0x33FFFFFF),
                    uncheckedBorderColor = Color(0xB3FFFFFF)
                )
            )
        }
    }
}

// ---------------------------------------------------------
//  LAYAR UTAMA
// ---------------------------------------------------------
@Composable
fun KasirScreen(modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kasir_prefs", Context.MODE_PRIVATE) }
    val laporan = remember {
        mutableStateListOf<Transaksi>().apply {
            addAll(parseLaporan(prefs.getString("laporan", "[]") ?: "[]"))
        }
    }
    val items = remember { mutableStateListOf<CartItem>() }
    var nextId by remember { mutableLongStateOf(1L) }

    var hargaText by rememberSaveable { mutableStateOf("") }
    var qtyText by rememberSaveable { mutableStateOf("1") }
    var uangText by rememberSaveable { mutableStateOf("") }

    val totalBarang = items.sumOf { it.qty }
    val totalHarga = items.sumOf { it.harga * it.qty }
    val uang = uangText.toLongOrNull() ?: 0L
    val kembalian = uang - totalHarga

    fun tambahBarang() {
        val h = hargaText.toLongOrNull() ?: 0L
        val q = qtyText.toIntOrNull() ?: 0
        if (h > 0 && q > 0) {
            items.add(CartItem(nextId++, h, q))
            hargaText = ""
            qtyText = "1"
        }
        focusManager.clearFocus()
    }

    fun simpanLaporan() {
        prefs.edit().putString("laporan", laporan.toJson()).apply()
    }

    fun selesaikanTransaksi() {
        if (totalHarga <= 0L || uang < totalHarga) return
        val now = System.currentTimeMillis()
        laporan.add(0, Transaksi(now, now, uang, items.toList()))
        simpanLaporan()
        items.clear()
        hargaText = ""
        qtyText = "1"
        uangText = ""
        focusManager.clearFocus()
        Toast.makeText(context, "Transaksi disimpan ke laporan", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // ---------- INPUT BARANG ----------
        SectionCard("Input Barang") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactField(
                    label = "Harga Barang",
                    value = hargaText,
                    onValueChange = { v -> hargaText = v.filter { it.isDigit() }.take(12) },
                    modifier = Modifier.weight(1f),
                    prefix = "Rp",
                    placeholder = "0"
                )
                CompactField(
                    label = "Jumlah",
                    value = qtyText,
                    onValueChange = { v -> qtyText = v.filter { it.isDigit() }.take(5) },
                    modifier = Modifier.width(84.dp),
                    placeholder = "1",
                    imeAction = ImeAction.Done,
                    onDone = { tambahBarang() }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { tambahBarang() },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = FieldShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) { Text("Tambah", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }

                OutlinedButton(
                    onClick = {
                        items.clear()
                        hargaText = ""
                        qtyText = "1"
                        uangText = ""
                        focusManager.clearFocus()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = FieldShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) { Text("Reset", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }
        }

        // ---------- DAFTAR BELANJA ----------
        if (items.isNotEmpty()) {
            SectionCard("Daftar Belanja") {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${index + 1}. ${rupiah(item.harga)} × ${item.qty}",
                                fontSize = 13.sp
                            )
                            Text(
                                "Subtotal ${rupiah(item.harga * item.qty)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { items.remove(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (index != items.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }

        // ---------- TOTAL ----------
        SectionCard("Total") {
            ResultRow("Total Barang", "$totalBarang item")
            Spacer(Modifier.height(6.dp))
            ResultRow("Total Belanja", rupiah(totalHarga), big = true)
        }

        // ---------- PEMBAYARAN ----------
        SectionCard("Pembayaran") {
            CompactField(
                label = "Uang Pembeli",
                value = uangText,
                onValueChange = { v -> uangText = v.filter { it.isDigit() }.take(12) },
                modifier = Modifier.fillMaxWidth(),
                prefix = "Rp",
                placeholder = "0",
                imeAction = ImeAction.Done,
                onDone = { focusManager.clearFocus() }
            )
            Spacer(Modifier.height(10.dp))

            when {
                uangText.isEmpty() || totalHarga == 0L ->
                    ResultRow("Kembalian", "-", big = true)

                kembalian >= 0 ->
                    ResultRow("Kembalian", rupiah(kembalian), big = true, highlight = true)

                else ->
                    ResultRow("Uang Kurang", rupiah(-kembalian), big = true, error = true)
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { selesaikanTransaksi() },
                enabled = totalHarga > 0L && uang >= totalHarga,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = FieldShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("Selesaikan Transaksi", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // ---------- LAPORAN ----------
        LaporanCard(
            laporan = laporan,
            onHapus = { t ->
                laporan.remove(t)
                simpanLaporan()
            },
            onHapusSemua = {
                laporan.clear()
                simpanLaporan()
            }
        )

        // ---------- DEVELOPER ----------
        DeveloperCard()
    }
}

// ---------------------------------------------------------
//  KOMPONEN
// ---------------------------------------------------------
@Composable
fun LaporanCard(
    laporan: List<Transaksi>,
    onHapus: (Transaksi) -> Unit,
    onHapusSemua: () -> Unit
) {
    val context = LocalContext.current
    var hariIni by rememberSaveable { mutableStateOf(true) }
    var konfirmasi by remember { mutableStateOf(false) }

    val tampil = if (hariIni) laporan.filter { it.waktu >= awalHariIni() } else laporan.toList()
    val totalPendapatan = tampil.sumOf { it.total }
    val totalTerjual = tampil.sumOf { it.totalBarang }
    val onSurface = MaterialTheme.colorScheme.onSurface

    SectionCard("Laporan Penjualan") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = hariIni,
                onClick = { hariIni = true },
                label = { Text("Hari Ini", fontSize = 12.sp) }
            )
            FilterChip(
                selected = !hariIni,
                onClick = { hariIni = false },
                label = { Text("Semua", fontSize = 12.sp) }
            )
        }
        Spacer(Modifier.height(10.dp))
        ResultRow("Jumlah Transaksi", "${tampil.size}")
        Spacer(Modifier.height(4.dp))
        ResultRow("Barang Terjual", "$totalTerjual item")
        Spacer(Modifier.height(6.dp))
        ResultRow("Total Pendapatan", rupiah(totalPendapatan), big = true, highlight = true)
        Spacer(Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        if (tampil.isEmpty()) {
            Text(
                "Belum ada transaksi.",
                fontSize = 12.sp,
                color = onSurface.copy(alpha = 0.6f)
            )
        } else {
            tampil.take(10).forEachIndexed { index, t ->
                val nomor = laporan.size - laporan.indexOf(t)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "#$nomor · ${formatWaktu(t.waktu)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${t.totalBarang} item · ${rupiah(t.total)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Bayar ${rupiah(t.bayar)} · Kembali ${rupiah(t.kembali)}",
                            fontSize = 11.sp,
                            color = onSurface.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(
                        onClick = { onHapus(t) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus transaksi",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (index != minOf(tampil.size, 10) - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            if (tampil.size > 10) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Menampilkan 10 terbaru dari ${tampil.size} transaksi",
                    fontSize = 11.sp,
                    color = onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val judul = if (hariIni) "HARI INI" else "SEMUA"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, buatTeksLaporan(judul, tampil))
                        }
                        context.startActivity(Intent.createChooser(intent, "Bagikan laporan"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = FieldShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) { Text("Bagikan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }

                OutlinedButton(
                    onClick = { konfirmasi = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = FieldShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        "Hapus Semua",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE53935)
                    )
                }
            }
        }
    }

    if (konfirmasi) {
        AlertDialog(
            onDismissRequest = { konfirmasi = false },
            title = { Text("Hapus semua laporan?") },
            text = { Text("Seluruh riwayat transaksi akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    onHapusSemua()
                    konfirmasi = false
                }) { Text("Hapus", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { konfirmasi = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun CompactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    placeholder: String = "",
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = if (focused) primary else onSurface.copy(alpha = 0.30f)

    Column(modifier) {
        Text(label, fontSize = 12.sp, color = onSurface.copy(alpha = 0.70f))
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .border(if (focused) 2.dp else 1.dp, borderColor, FieldShape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (prefix != null) {
                Text(prefix, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primary)
                Spacer(Modifier.width(6.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = onSurface),
                cursorBrush = SolidColor(primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                fontSize = 16.sp,
                                color = onSurface.copy(alpha = 0.35f)
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}

@Composable
fun ResultRow(
    label: String,
    value: String,
    big: Boolean = false,
    error: Boolean = false,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = if (big) 14.sp else 13.sp)
        Text(
            value,
            fontSize = if (big) 20.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                error -> Color(0xFFE53935)
                highlight -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
fun DeveloperCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Developer", fontSize = 11.sp)
            Text("Bagas Orcarius", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SocialButton("Facebook", URL_FACEBOOK) {
                    FacebookIcon(Modifier.size(30.dp))
                }
                SocialButton("GitHub", URL_GITHUB) {
                    GithubIcon(Modifier.size(30.dp), MaterialTheme.colorScheme.onSurface)
                }
                SocialButton("Instagram", URL_INSTAGRAM) {
                    InstagramIcon(Modifier.size(30.dp))
                }
            }
        }
    }
}

// Tombol bulat berisi ikon saja (tanpa teks / tautan)
@Composable
fun SocialButton(description: String, url: String, icon: @Composable () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable { uriHandler.openUri(url) }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { icon() }
}

@Composable
fun FacebookIcon(modifier: Modifier = Modifier) {
    val path = remember { PathParser().parsePathString(PATH_FACEBOOK).toPath() }
    Canvas(modifier) {
        val s = size.minDimension / 24f
        // Latar putih agar huruf "f" tampak putih
        drawCircle(Color.White, radius = 12f * s, center = Offset(12f * s, 12.04f * s))
        scale(s, pivot = Offset.Zero) { drawPath(path, Color(0xFF1877F2)) }
    }
}

@Composable
fun GithubIcon(modifier: Modifier = Modifier, color: Color) {
    val path = remember { PathParser().parsePathString(PATH_GITHUB).toPath() }
    Canvas(modifier) {
        val s = size.minDimension / 24f
        scale(s, pivot = Offset.Zero) { drawPath(path, color) }
    }
}

@Composable
fun InstagramIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.minDimension
        val stroke = Stroke(width = w * 0.07f)

        // Latar gradasi khas Instagram
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFEDA77),
                    Color(0xFFF58529),
                    Color(0xFFDD2A7B),
                    Color(0xFF8134AF),
                    Color(0xFF515BD4)
                ),
                start = Offset(0f, w),
                end = Offset(w, 0f)
            ),
            cornerRadius = CornerRadius(w * 0.28f)
        )
        // Bingkai kamera
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.21f, w * 0.21f),
            size = Size(w * 0.58f, w * 0.58f),
            cornerRadius = CornerRadius(w * 0.17f),
            style = stroke
        )
        // Lensa
        drawCircle(
            color = Color.White,
            radius = w * 0.14f,
            center = Offset(w * 0.5f, w * 0.5f),
            style = stroke
        )
        // Lampu kilat
        drawCircle(
            color = Color.White,
            radius = w * 0.04f,
            center = Offset(w * 0.68f, w * 0.32f)
        )
    }
}

@Composable
fun Footer() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            "Dibuat dengan Kotlin by Bagas Orcarius",
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            fontSize = 11.sp
        )
    }
}
