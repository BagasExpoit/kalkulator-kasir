package com.kasirku.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun QrisScreen(vm: KasirViewModel, notify: (String) -> Unit) {
    val focus = LocalFocusManager.current
    var loading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            loading = true
            vm.loadQris(uri) { err ->
                loading = false
                if (err == null) notify("QRIS terbaca ✓ — tekan Simpan agar tersimpan")
                else notify("⚠ $err")
            }
        }
    }

    var testNominal by rememberSaveable { mutableStateOf("") }
    val info = remember(vm.qrisPayload) { Qris.inspect(vm.qrisPayload) }
    val bmp = vm.qrisBitmap
    val amt = testNominal.toLongOrNull() ?: 0L
    val dynPayload = remember(vm.qrisPayload, amt) { if (amt > 0L) vm.dynamicQris(amt) else null }
    val dynBmp = remember(dynPayload) { dynPayload?.let { Qris.qrBitmap(it) } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---- Upload QRIS ----
        SectionTitle("QRIS Toko")
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (bmp == null) {
                    // Placeholder
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        if (loading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(Modifier.size(36.dp))
                                Text("Membaca QRIS…", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.QrCode2, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                Text("Belum ada QRIS", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("Upload gambar QRIS statis dari bank / e-wallet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Image(
                        bmp.asImageBitmap(), "QRIS",
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )
                }

                Button(
                    onClick = { picker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                ) {
                    Icon(Icons.Rounded.UploadFile, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (bmp == null) "Upload QRIS" else "Ganti QRIS")
                }
                if (bmp != null) {
                    TextButton(
                        onClick = { vm.removeQris() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Hapus QRIS", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // ---- Status validasi ----
        if (info != null) {
            // Chip status
            Surface(
                color = if (info.dynamic) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (info.dynamic) Icons.Rounded.DynamicFeed else Icons.Rounded.CheckCircle,
                        null, Modifier.size(20.dp),
                        tint = if (info.dynamic) MaterialTheme.colorScheme.onSecondaryContainer
                               else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (info.dynamic) "QRIS Dinamis" else "QRIS Statis ✓",
                            fontWeight = FontWeight.SemiBold,
                            color = if (info.dynamic) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        val detail = buildList {
                            if (info.merchant.isNotBlank()) add(info.merchant)
                            if (info.city.isNotBlank()) add(info.city)
                            if (info.dynamic && info.lockedAmount != null) add("Nominal terkunci: ${rp(info.lockedAmount)}")
                        }.joinToString(" • ")
                        if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall,
                            color = if (info.dynamic) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Info QRIS sudah dinamis
            if (info.dynamic) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Warning, null, Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            "QRIS ini sudah dinamis. Disarankan upload QRIS statis " +
                            "dari aplikasi bank / e-wallet agar nominal bisa dikunci per transaksi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // ---- Uji konversi statis → dinamis ----
            SectionTitle("Pratinjau Dinamis")
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Info, null, Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Masukkan nominal untuk melihat pratinjau QRIS dinamis. " +
                            "Di tab Kasir nominal diambil otomatis dari total belanja.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = testNominal,
                        onValueChange = { testNominal = it.filter(Char::isDigit).take(10) },
                        label = { Text("Nominal uji coba") },
                        prefix = { Text("Rp ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (dynBmp != null && amt > 0L) {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                dynBmp.asImageBitmap(), "QRIS dinamis",
                                Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(6.dp)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    rp(amt),
                                    Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    } else if (amt > 0L && dynPayload == null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "Gagal membuat QRIS dinamis. Upload ulang QRIS.",
                                Modifier.fillMaxWidth().padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Text(
                "Catatan: konfirmasi pembayaran tetap harus dicek dari notifikasi/mutasi bank atau e-wallet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
