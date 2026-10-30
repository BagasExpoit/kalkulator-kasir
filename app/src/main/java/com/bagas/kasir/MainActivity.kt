package com.bagas.kasir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
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

private val CardShape = RoundedCornerShape(14.dp)
private val FieldShape = RoundedCornerShape(10.dp)

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
        }

        // ---------- DEVELOPER ----------
        DeveloperCard()
    }
}

// ---------------------------------------------------------
//  KOMPONEN
// ---------------------------------------------------------
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
    val uriHandler = LocalUriHandler.current
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
            Spacer(Modifier.height(6.dp))
            FilledTonalButton(
                onClick = { uriHandler.openUri("https://facebook.com/willydevops1") },
                modifier = Modifier.height(34.dp),
                shape = FieldShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
            ) {
                Text("Facebook: willydevops1", fontSize = 12.sp)
            }
        }
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
