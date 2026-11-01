package com.kasirku.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

fun rp(v: Long): String =
    "Rp" + NumberFormat.getIntegerInstance(Locale("id", "ID")).format(v)

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
fun KasirScreen(vm: KasirViewModel, notify: (String) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Item?>(null) }
    var showQr by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { StoreHeader(vm) }

        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    SectionTitle("Daftar Belanja")
                    if (vm.items.isNotEmpty()) {
                        Text(
                            "${vm.items.size} item • ${vm.items.sumOf { it.qty }} pcs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilledTonalButton(onClick = { adding = true }) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tambah")
                }
            }
        }

        if (vm.items.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.ShoppingCart, null, Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Text("Belum ada barang",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tekan \"Tambah\" untuk menambahkan barang.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                }
            }
        }

        items(vm.items, key = { it.id }) { item ->
            ItemRow(
                item = item,
                onEdit = { editing = item },
                onDelete = { vm.removeItem(item.id) },
                onQty = { d -> vm.updateItem(item.copy(qty = (item.qty + d).coerceAtLeast(1))) }
            )
        }

        item { PaymentCard(vm) }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        if (vm.items.isNotEmpty()) {
                            vm.newTransaction()
                            notify("Transaksi disimpan ke Laporan ✓")
                        } else {
                            vm.newTransaction()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Transaksi Baru")
                }
                Button(
                    onClick = {
                        when {
                            vm.total <= 0L -> notify("Tambahkan barang dulu")
                            vm.qrisPayload.isBlank() -> notify("Upload QRIS dulu di tab QRIS")
                            else -> {
                                vm.updatePaid(vm.total.toString())   // auto-isi nominal
                                showQr = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.QrCode2, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bayar QRIS")
                }
            }
        }
    }

    if (adding) {
        ItemDialog(null, onDismiss = { adding = false }) { n, p, q ->
            vm.addItem(n, p, q); adding = false
        }
    }
    editing?.let { e ->
        ItemDialog(e, onDismiss = { editing = null }) { n, p, q ->
            vm.updateItem(e.copy(name = n, price = p, qty = q)); editing = null
        }
    }
    if (showQr) QrisPayDialog(vm, onDone = { showQr = false }, onFinish = {
        showQr = false
        vm.newTransaction()
        notify("Pembayaran selesai • Transaksi disimpan ke Laporan ✓")
    })
}

// ---- Store header ----

@Composable
private fun StoreHeader(vm: KasirViewModel) {
    val brush = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(brush).padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Storefront, null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text(
                    vm.storeName.ifBlank { "Nama toko belum diisi" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Color.White
                )
            }
            if (vm.phone.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Phone, null, Modifier.size(15.dp),
                        tint = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.width(6.dp))
                    Text(vm.phone, style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

// ---- Item row ----

@Composable
private fun ItemRow(item: Item, onEdit: () -> Unit, onDelete: () -> Unit, onQty: (Int) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text("${rp(item.price)} / satuan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(rp(item.subtotal), fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { onQty(-1) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Remove, "Kurangi", Modifier.size(18.dp))
                }
                Text("${item.qty}", Modifier.widthIn(min = 44.dp),
                    textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                FilledTonalIconButton(onClick = { onQty(1) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Add, "Tambah", Modifier.size(18.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ---- Payment card ----

@Composable
private fun PaymentCard(vm: KasirViewModel) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    rp(vm.total),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider()
            OutlinedTextField(
                value = vm.paid,
                onValueChange = { vm.updatePaid(it.filter(Char::isDigit).take(12)) },
                label = { Text("Uang pembeli") },
                prefix = { Text("Rp ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { if (vm.paidAmount > 0L) Text(rp(vm.paidAmount)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { vm.updatePaid(vm.total.toString()) }, label = { Text("Uang pas") })
                listOf(20_000L, 50_000L, 100_000L).forEach { n ->
                    AssistChip(onClick = { vm.updatePaid(n.toString()) }, label = { Text(rp(n)) })
                }
            }
            val change = vm.change
            val ok = change >= 0L
            Surface(
                color = if (ok) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (ok) "Kembalian" else "Kurang", fontWeight = FontWeight.SemiBold)
                    Text(
                        rp(abs(change)),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

// ---- Dialogs ----

@Composable
private fun ItemDialog(item: Item?, onDismiss: () -> Unit, onConfirm: (String, Long, Int) -> Unit) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var price by remember { mutableStateOf(item?.price?.toString() ?: "") }
    var qty by remember { mutableStateOf(item?.qty?.toString() ?: "1") }
    val p = price.toLongOrNull() ?: 0L
    val q = qty.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Tambah Barang" else "Edit Barang") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Nama barang") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = price, onValueChange = { price = it.filter(Char::isDigit).take(12) },
                    label = { Text("Harga satuan") }, prefix = { Text("Rp ") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = qty, onValueChange = { qty = it.filter(Char::isDigit).take(4) },
                    label = { Text("Jumlah") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), p, q) },
                enabled = name.isNotBlank() && p > 0L && q > 0
            ) { Text(if (item == null) "Tambah" else "Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun QrisPayDialog(vm: KasirViewModel, onDone: () -> Unit, onFinish: () -> Unit) {
    val total = vm.total
    val payload = remember(vm.qrisPayload, total) { vm.dynamicQris(total) }
    val bmp = remember(payload) { payload?.let { Qris.qrBitmap(it) } }
    val info = remember(vm.qrisPayload) { Qris.inspect(vm.qrisPayload) }

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Scan untuk membayar", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (bmp != null) {
                    Image(
                        bmp.asImageBitmap(), "QRIS dinamis",
                        Modifier.size(260.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color.White).padding(6.dp)
                    )
                } else {
                    Text("Gagal membuat QRIS dinamis. Upload ulang QRIS di tab QRIS.",
                        color = MaterialTheme.colorScheme.error)
                }
                Text(
                    rp(total),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                val merchant = info?.merchant.orEmpty()
                if (merchant.isNotBlank()) Text(merchant, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Nominal sudah terkunci. Pastikan pembayaran masuk sebelum menyerahkan barang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(onClick = onFinish) { Text("Selesai & Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDone) { Text("Kembali") }
        }
    )
}
