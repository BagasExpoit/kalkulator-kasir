package com.kasirku.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Utilitas QRIS (standar EMVCo Merchant-Presented QR): baca, validasi, ubah statis -> dinamis. */
object Qris {

    private data class Tlv(val tag: String, val value: String)

    data class Info(
        val merchant: String,
        val city: String,
        val dynamic: Boolean,
        val lockedAmount: Long? = null   // hadir jika QRIS sudah dinamis & ada tag 54
    )

    // ---- Parsing TLV EMVCo ----

    private fun parse(p: String): List<Tlv>? {
        val out = mutableListOf<Tlv>()
        var i = 0
        while (i < p.length) {
            if (i + 4 > p.length) return null
            val len = p.substring(i + 2, i + 4).toIntOrNull() ?: return null
            if (i + 4 + len > p.length) return null
            out.add(Tlv(p.substring(i, i + 2), p.substring(i + 4, i + 4 + len)))
            i += 4 + len
        }
        return out
    }

    /**
     * Validasi & ekstrak info dari payload QRIS.
     * Mengembalikan null jika bukan payload QRIS yang valid (tidak dimulai "000201" / tanpa CRC).
     */
    fun inspect(payload: String): Info? {
        val p = payload.trim()
        if (!p.startsWith("000201")) return null
        val t = parse(p) ?: return null
        if (t.none { it.tag == "63" }) return null
        fun v(tag: String) = t.firstOrNull { it.tag == tag }?.value.orEmpty()
        val isDynamic = v("01") == "12"
        val lockedAmt = if (isDynamic) v("54").toLongOrNull() else null
        return Info(v("59"), v("60"), isDynamic, lockedAmt)
    }

    /** Validasi CRC dari payload — true jika CRC payload cocok. */
    fun verifyCrc(payload: String): Boolean {
        val p = payload.trim()
        val crcIdx = p.lastIndexOf("6304")
        if (crcIdx < 0 || crcIdx + 8 != p.length) return false
        val body = p.substring(0, crcIdx + 4)
        return crc16(body).equals(p.substring(crcIdx + 4), ignoreCase = true)
    }

    /** CRC16-CCITT (poly 0x1021, init 0xFFFF) sesuai spesifikasi EMVCo. */
    fun crc16(s: String): String {
        var crc = 0xFFFF
        for (b in s.toByteArray(Charsets.US_ASCII)) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return "%04X".format(crc)
    }

    /**
     * Ubah QRIS statis → dinamis:
     *  • Pastikan tag 01 = "12"
     *  • Sisipkan tag 54 (nominal) di posisi yang benar
     *  • Hapus tag 54 lama jika sudah ada (re-lock nominal)
     *  • Hitung ulang CRC (tag 63)
     */
    fun toDynamic(payload: String, amount: Long): String? {
        val list = parse(payload.trim()) ?: return null

        // Hapus tag CRC (63) dan tag nominal lama (54) sebelum diproses ulang
        val out = list.filter { it.tag != "63" && it.tag != "54" }.toMutableList()

        // Pastikan tag "01" ada dan bernilai "12" (dinamis)
        val idx01 = out.indexOfFirst { it.tag == "01" }
        if (idx01 >= 0) {
            out[idx01] = Tlv("01", "12")
        } else {
            // Sisipkan setelah tag "00" jika ada, atau di awal
            val after00 = out.indexOfFirst { it.tag > "01" }.let { if (it < 0) out.size else it }
            out.add(after00, Tlv("01", "12"))
        }

        // Sisipkan tag "54" (nominal) di posisi yang sesuai urutan tag numerik
        val insertAt = out.indexOfFirst { it.tag > "54" }.let { if (it < 0) out.size else it }
        out.add(insertAt, Tlv("54", amount.toString()))

        // Bangun ulang payload + hitung CRC baru
        val body = out.joinToString("") {
            it.tag + it.value.length.toString().padStart(2, '0') + it.value
        } + "6304"
        return body + crc16(body)
    }

    // ---- Gambar ----

    fun flatten(b: Bitmap): Bitmap {
        val o = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
        Canvas(o).also { c -> c.drawColor(Color.WHITE); c.drawBitmap(b, 0f, 0f, null) }
        return o
    }

    private fun scaleDown(b: Bitmap, max: Int): Bitmap {
        val m = maxOf(b.width, b.height)
        if (m <= max) return b
        val f = max.toFloat() / m
        return Bitmap.createScaledBitmap(
            b,
            (b.width * f).toInt().coerceAtLeast(1),
            (b.height * f).toInt().coerceAtLeast(1),
            true
        )
    }

    /** Membaca teks QR dari bitmap, null jika tidak ditemukan. */
    fun decode(src: Bitmap): String? {
        val hints: Map<DecodeHintType, Any> = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )
        for (max in listOf(1600, 1000, 640)) {
            val bmp = scaleDown(src, max)
            val px = IntArray(bmp.width * bmp.height)
            bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            val lum = RGBLuminanceSource(bmp.width, bmp.height, px)
            for (s in listOf<LuminanceSource>(lum, lum.invert())) {
                for (bin in listOf(HybridBinarizer(s), GlobalHistogramBinarizer(s))) {
                    try {
                        return QRCodeReader().decode(BinaryBitmap(bin), hints).text
                    } catch (_: Exception) { /* coba kombinasi berikutnya */ }
                }
            }
        }
        return null
    }

    /** Membuat gambar QR (hitam di atas putih). */
    fun qrBitmap(text: String, size: Int = 720): Bitmap {
        val hints: Map<EncodeHintType, Any> = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val px = IntArray(size * size) { i ->
            if (m.get(i % size, i / size)) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
    }
}
