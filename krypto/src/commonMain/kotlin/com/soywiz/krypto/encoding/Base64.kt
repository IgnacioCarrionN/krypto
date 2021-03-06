package com.soywiz.krypto.encoding

object Base64 {
    private const val ENCODE_TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    private const val ENCODE_TABLE_URLSAFE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_="

    private val DECODER = createDecoder(ENCODE_TABLE)

    private val DECODER_URLSAFE = createDecoder(ENCODE_TABLE_URLSAFE)

    private fun createDecoder(table: String): IntArray = IntArray(0x100){ -1 }.apply {
        for (n in table.indices) {
            this[table[n].toInt()] = n
        }
    }

    operator fun invoke(v: String) = decodeIgnoringSpaces(v)
    operator fun invoke(v: ByteArray) = encode(v)

    fun decode(str: String): ByteArray = decode(str, DECODER)

    fun decodeUrlSafe(str: String): ByteArray = decode(str, DECODER_URLSAFE)

    fun decodeIgnoringSpaces(str: String): ByteArray = decode(str.removeSpaces())

    fun decodeUrlSafeIgnoringSpaces(str: String): ByteArray = decodeUrlSafe(str.removeSpaces())

    private fun String.removeSpaces() = this.replace(" ", "").replace("\n", "").replace("\r", "")

    private fun decode(str: String, decoder: IntArray): ByteArray {
        val src = ByteArray(str.length) { str[it].toByte() }
        val dst = ByteArray(src.size)
        return dst.copyOf(decode(src, dst, decoder))
    }

    private fun decode(src: ByteArray, dst: ByteArray, decoder: IntArray): Int {

        var m = 0

        var n = 0
        while (n < src.size) {
            val d = decoder[src.readU8(n)]
            if (d < 0) {
                n++
                continue // skip character
            }

            val b0 = decoder[src.readU8(n++)]
            val b1 = decoder[src.readU8(n++)]
            val b2 = decoder[src.readU8(n++)]
            val b3 = decoder[src.readU8(n++)]
            dst[m++] = (b0 shl 2 or (b1 shr 4)).toByte()
            if (b2 < 64) {
                dst[m++] = (b1 shl 4 or (b2 shr 2)).toByte()
                if (b3 < 64) {
                    dst[m++] = (b2 shl 6 or b3).toByte()
                }
            }
        }
        return m
    }

    fun encode(src: ByteArray): String {
        return encode(src, ENCODE_TABLE)
    }

    fun encodeUrlSafe(src: ByteArray): String {
        return encode(src, ENCODE_TABLE_URLSAFE)
    }


    @Suppress("UNUSED_CHANGED_VALUE")
    private fun encode(src: ByteArray, table: String): String {

        val out = StringBuilder((src.size * 4) / 3 + 4)
        var ipos = 0
        val extraBytes = src.size % 3
        while (ipos < src.size - 2) {
            val num = src.readU24BE(ipos)
            ipos += 3

            out.append(table[(num ushr 18) and 0x3F])
            out.append(table[(num ushr 12) and 0x3F])
            out.append(table[(num ushr 6) and 0x3F])
            out.append(table[(num ushr 0) and 0x3F])
        }

        if (extraBytes == 1) {
            val num = src.readU8(ipos++)
            out.append(table[num ushr 2])
            out.append(table[(num shl 4) and 0x3F])
            out.append('=')
            out.append('=')
        } else if (extraBytes == 2) {
            val tmp = (src.readU8(ipos++) shl 8) or src.readU8(ipos++)
            out.append(table[tmp ushr 10])
            out.append(table[(tmp ushr 4) and 0x3F])
            out.append(table[(tmp shl 2) and 0x3F])
            out.append('=')
        }

        return out.toString()
    }

    private fun ByteArray.readU8(index: Int): Int = this[index].toInt() and 0xFF
    private fun ByteArray.readU24BE(index: Int): Int =
        (readU8(index + 0) shl 16) or (readU8(index + 1) shl 8) or (readU8(index + 2) shl 0)
}

fun String.fromBase64IgnoreSpaces(): ByteArray = Base64.decodeIgnoringSpaces(this)
fun String.fromBase64UrlSafeIgnoreSpaces(): ByteArray = Base64.decodeUrlSafeIgnoringSpaces(this)
fun String.fromBase64(ignoreSpaces: Boolean = false): ByteArray = if (ignoreSpaces) Base64.decodeIgnoringSpaces(this) else Base64.decode(this)
fun String.fromBase64UrlSafe(ignoreSpaces: Boolean = false): ByteArray = if(ignoreSpaces) Base64.decodeUrlSafeIgnoringSpaces(this) else Base64.decodeUrlSafe(this)
fun ByteArray.toBase64(): String = Base64.encode(this)
fun ByteArray.toBase64UrlSafe(): String = Base64.encodeUrlSafe(this)
val ByteArray.base64: String get() = Base64.encode(this)
val ByteArray.base64UrlSafe: String get() = Base64.encodeUrlSafe(this)
