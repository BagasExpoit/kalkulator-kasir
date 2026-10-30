package com.bagas.kasir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun KasirTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFF80CBC4),
            onPrimary = Color(0xFF00201D),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF2A2A2A),
            onBackground = Color(0xFFEDEDED),
            onSurface = Color(0xFFEDEDED)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF00695C),
            onPrimary = Color.White,
            background = Color(0xFFF6F8F7),
            surface = Color.White,
            surfaceVariant = Color(0xFFE3EEEB),
            onBackground = Color(0xFF111111),
            onSurface = Color(0xFF111111)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirApp() {
    val systemDark = isSystemInDarkTheme()
    var dark by rememberSaveable { mutableStateOf(systemDark) }

    KasirTheme(dark) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Kalkulator Kasir", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        Text(
                            if (dark) "Gelap" else "Terang",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Switch(checked = dark, onCheckedChange = { dark = it })
                        Spacer(Modifier.width(12.dp))
                    }
                )
            },
            bottomBar = { Footer() },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            KasirScreen(Modifier.padding(padding))
        }
    }
}

@Composable
fun KasirScreen(modifier: Modifier = Modifier) {
    val items = remember { mutableStateListOf<CartItem>() }
    var nextId by remember { mutableLongStateOf(1L) }

    var hargaText by rememberSaveable { mutableStateOf("") }
    var qtyText by rememberSaveable { mutableStateOf("1") }
    var uangText by rememberSaveable { mutableStateOf("") }

    val totalBarang = items.sumOf { it.qty }
    val totalHarga = items.sumOf { it.harga * it.qty }
    val uang = uangText.toLongOrNull() ?: 0L
    val kembalian = uang - totalHarga

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ---------- INPUT BARANG ----------
        SectionCard("Input Barang") {
            OutlinedTextField(
                value = hargaText,
                onValueChange = { v -> hargaText = v.filter { it.isDigit() }.take(12) },
                label = { Text("Harga Barang (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = qtyText,
                onValueChange = { v -> qtyText = v.filter { it.isDigit() }.take(5) },
                label = { Text("Jumlah Barang") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val h = hargaText.toLongOrNull() ?: 0L
                        val q = qtyText.toIntOrNull() ?: 0
                        if (h > 0 && q > 0) {
                            items.add(CartItem(nextId++, h, q))
                            hargaText = ""
                            qtyText = "1"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Tambah") }

                OutlinedButton(
                    onClick = {
                        items.clear()
                        hargaText = ""
                        qtyText = "1"
                        uangText = ""
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Reset") }
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
                            Text("${index + 1}. ${rupiah(item.harga)} × ${item.qty}")
                            Text(
                                "Subtotal ${rupiah(item.harga * item.qty)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { items.remove(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus")
                        }
                    }
                    if (index != items.lastIndex) HorizontalDivider()
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
            OutlinedTextField(
                value = uangText,
                onValueChange = { v -> uangText = v.filter { it.isDigit() }.take(12) },
                label = { Text("Uang Pembeli (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            when {
                uangText.isEmpty() || totalHarga == 0L ->
                    ResultRow("Kembalian", "-", big = true)

                kembalian >= 0 ->
                    ResultRow("Kembalian", rupiah(kembalian), big = true)

                else -> {
                    ResultRow("Uang Kurang", rupiah(-kembalian), big = true, error = true)
                }
            }
        }

        // ---------- DEVELOPER ----------
        DeveloperCard()
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun ResultRow(label: String, value: String, big: Boolean = false, error: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = if (big) 16.sp else 14.sp)
        Text(
            value,
            fontSize = if (big) 22.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (error) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DeveloperCard() {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Developer", fontSize = 12.sp)
            Text("Bagas Orcarius", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Kontak Sosial Media", fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = { uriHandler.openUri("https://facebook.com/willydevops1") }
            ) {
                Text("Facebook: willydevops1")
            }
        }
    }
}

@Composable
fun Footer() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 3.dp) {
        Text(
            "Dibuat dengan Kotlin by Bagas Orcarius",
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
    }
}
