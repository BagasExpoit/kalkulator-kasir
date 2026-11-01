package com.kasirku.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

// ---- Model ----

data class Item(val id: Long, val name: String, val price: Long, val qty: Int) {
    val subtotal: Long get() = price * qty
}

data class InvoiceItem(val name: String, val price: Long, val qty: Int) {
    val subtotal: Long get() = price * qty
}

data class Invoice(
    val id: Long,
    val timestamp: Long,
    val storeName: String,
    val items: List<InvoiceItem>,
    val total: Long,
    val paid: Long,
    val change: Long
)

// ---- ViewModel ----

class KasirViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("kasir", Context.MODE_PRIVATE)
    private val qrisFile = File(app.filesDir, "qris.png")

    var storeName by mutableStateOf("Toko Saya"); private set
    var phone by mutableStateOf(""); private set
    var paid by mutableStateOf(""); private set
    var darkTheme by mutableStateOf(false); private set
    var qrisPayload by mutableStateOf(""); private set
    var qrisBitmap by mutableStateOf<Bitmap?>(null); private set
    var dirty by mutableStateOf(false); private set

    val items = mutableStateListOf<Item>()
    val invoices = mutableStateListOf<Invoice>()

    val total: Long get() = items.sumOf { it.subtotal }
    val paidAmount: Long get() = paid.toLongOrNull() ?: 0L
    val change: Long get() = paidAmount - total

    init {
        val sysDark = (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        storeName = prefs.getString("store", "Toko Saya") ?: "Toko Saya"
        phone = prefs.getString("phone", "") ?: ""
        paid = prefs.getString("paid", "") ?: ""
        qrisPayload = prefs.getString("qris", "") ?: ""
        darkTheme = prefs.getBoolean("dark", sysDark)

        runCatching {
            val arr = JSONArray(prefs.getString("items", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(Item(o.getLong("id"), o.getString("name"), o.getLong("price"), o.getInt("qty")))
            }
        }
        if (qrisFile.exists()) qrisBitmap = BitmapFactory.decodeFile(qrisFile.path)
        loadInvoices()
    }

    // ---- Toko ----
    fun updateStoreName(v: String) { storeName = v; dirty = true }
    fun updatePhone(v: String) { phone = v; dirty = true }
    fun updatePaid(v: String) { paid = v; dirty = true }
    fun toggleTheme() { darkTheme = !darkTheme; prefs.edit().putBoolean("dark", darkTheme).apply() }

    // ---- Barang ----
    fun addItem(name: String, price: Long, qty: Int) {
        items.add(Item(System.nanoTime(), name.ifBlank { "Barang" }, price, qty))
        dirty = true
    }

    fun updateItem(item: Item) {
        val i = items.indexOfFirst { it.id == item.id }
        if (i >= 0) { items[i] = item; dirty = true }
    }

    fun removeItem(id: Long) { items.removeAll { it.id == id }; dirty = true }

    /** Simpan invoice lalu mulai transaksi baru. */
    fun newTransaction() {
        if (items.isNotEmpty()) {
            val inv = Invoice(
                id = System.nanoTime(),
                timestamp = System.currentTimeMillis(),
                storeName = storeName,
                items = items.map { InvoiceItem(it.name, it.price, it.qty) },
                total = total,
                paid = paidAmount,
                change = change
            )
            invoices.add(0, inv)   // terbaru di depan
            persistInvoices()
        }
        items.clear()
        paid = ""
        dirty = true
    }

    // ---- QRIS ----
    fun loadQris(uri: Uri, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val r = withContext(Dispatchers.Default) { runCatching { readQris(uri) } }
            r.onSuccess { (bmp, text) ->
                qrisBitmap = bmp
                qrisPayload = text
                dirty = true
                onDone(null)
            }.onFailure { onDone(it.message ?: "Gagal membaca gambar") }
        }
    }

    private fun readQris(uri: Uri): Pair<Bitmap, String> {
        val raw = decodeSampled(getApplication(), uri)
            ?: error("Gambar tidak bisa dibuka. Coba pilih ulang.")
        val bmp = Qris.flatten(raw)
        val text = Qris.decode(bmp)?.trim()
            ?: error("QR tidak terbaca. Pastikan gambar QRIS cukup terang, fokus, dan tidak terpotong.")
        if (Qris.inspect(text) == null)
            error("Ini bukan QRIS yang valid. Pastikan gambar berasal dari aplikasi pembayaran resmi.")
        if (!Qris.verifyCrc(text))
            error("CRC QRIS tidak cocok — data mungkin rusak atau tidak lengkap.")
        return Pair(bmp, text)
    }

    private fun decodeSampled(ctx: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2000) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    fun removeQris() { qrisPayload = ""; qrisBitmap = null; dirty = true }

    /** Payload QRIS dinamis dengan nominal terkunci, null jika belum ada QRIS atau nominal ≤ 0. */
    fun dynamicQris(amount: Long): String? =
        if (qrisPayload.isBlank() || amount <= 0L) null else Qris.toDynamic(qrisPayload, amount)

    // ---- Laporan ----
    private fun loadInvoices() {
        runCatching {
            val arr = JSONArray(prefs.getString("invoices", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val ia = o.getJSONArray("items")
                val invItems = (0 until ia.length()).map { j ->
                    val it = ia.getJSONObject(j)
                    InvoiceItem(it.getString("name"), it.getLong("price"), it.getInt("qty"))
                }
                invoices.add(Invoice(
                    id = o.getLong("id"),
                    timestamp = o.getLong("ts"),
                    storeName = o.getString("store"),
                    items = invItems,
                    total = o.getLong("total"),
                    paid = o.getLong("paid"),
                    change = o.getLong("change")
                ))
            }
        }
    }

    private fun persistInvoices() {
        val arr = JSONArray()
        invoices.take(200).forEach { inv ->
            val ia = JSONArray()
            inv.items.forEach { item ->
                ia.put(JSONObject().put("name", item.name).put("price", item.price).put("qty", item.qty))
            }
            arr.put(
                JSONObject()
                    .put("id", inv.id).put("ts", inv.timestamp).put("store", inv.storeName)
                    .put("items", ia).put("total", inv.total).put("paid", inv.paid).put("change", inv.change)
            )
        }
        prefs.edit().putString("invoices", arr.toString()).apply()
    }

    fun clearInvoices() {
        invoices.clear()
        prefs.edit().putString("invoices", "[]").apply()
    }

    // ---- Simpan ----
    fun save() {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("price", it.price).put("qty", it.qty))
        }
        prefs.edit()
            .putString("store", storeName)
            .putString("phone", phone)
            .putString("paid", paid)
            .putString("items", arr.toString())
            .putString("qris", qrisPayload)
            .putBoolean("dark", darkTheme)
            .apply()
        val bmp = qrisBitmap
        if (bmp != null) {
            FileOutputStream(qrisFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } else {
            qrisFile.delete()
        }
        dirty = false
    }
}
