package com.example.core

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor

/**
 * Pure Kotlin QR Code (Byte mode, ECC Level L, Versions 1..6) encoder.
 * Produces real, camera-scannable QR matrices with zero external dependencies.
 */
object QrMatrixGenerator {
    private val dataCapacityL = intArrayOf(0, 19, 34, 55, 80, 108, 136)
    private val eccCodewordsL = intArrayOf(0, 7, 10, 15, 20, 26, 18) // v6 has 2 blocks x 18 ECC

    private val expTable = IntArray(512)
    private val logTable = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            expTable[i] = x
            logTable[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) x = x xor 0x11d
        }
        for (i in 255 until 512) {
            expTable[i] = expTable[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return expTable[logTable[a] + logTable[b]]
    }

    private fun rsGeneratorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            val root = expTable[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], root)
            }
            poly = next
        }
        return poly
    }

    private fun computeEcc(data: IntArray, eccLen: Int): IntArray {
        val gen = rsGeneratorPoly(eccLen)
        val res = IntArray(data.size + eccLen)
        data.copyInto(res)
        for (i in data.indices) {
            val factor = res[i]
            if (factor != 0) {
                for (j in gen.indices) {
                    res[i + j] = res[i + j] xor gfMul(gen[j], factor)
                }
            }
        }
        return res.copyOfRange(data.size, res.size)
    }

    fun encode(text: String): Array<BooleanArray> {
        val bytes = text.toByteArray(Charsets.UTF_8).let {
            if (it.size > 130) it.copyOf(130) else it
        }
        var version = 1
        while (version <= 6 && bytes.size + 2 > dataCapacityL[version]) {
            version++
        }
        if (version > 6) version = 6

        val size = 17 + 4 * version
        val totalDataCodewords = dataCapacityL[version]

        // Build bitstream: mode (0100), char count (8 bits), data bytes, terminator, pad bytes
        val bits = ArrayList<Boolean>(totalDataCodewords * 8)
        fun pushBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add(((value ushr i) and 1) == 1)
            }
        }
        pushBits(0b0100, 4) // Byte mode
        pushBits(bytes.size, 8)
        for (b in bytes) pushBits(b.toInt() and 0xFF, 8)
        val maxBits = totalDataCodewords * 8
        val termLen = minOf(4, maxBits - bits.size)
        pushBits(0, termLen)
        while (bits.size % 8 != 0) bits.add(false)

        val dataCodewords = IntArray(totalDataCodewords)
        var idx = 0
        for (i in 0 until bits.size step 8) {
            var byteVal = 0
            for (j in 0 until 8) {
                byteVal = (byteVal shl 1) or (if (bits[i + j]) 1 else 0)
            }
            dataCodewords[idx++] = byteVal
        }
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (idx < totalDataCodewords) {
            dataCodewords[idx++] = padBytes[padIdx % 2]
            padIdx++
        }

        // ECC & interleaving (v1..v5 are 1 block; v6-L has 2 blocks of 68 data + 18 ECC)
        val allBytes: IntArray = if (version <= 5) {
            val ecc = computeEcc(dataCodewords, eccCodewordsL[version])
            dataCodewords + ecc
        } else {
            val b1 = dataCodewords.copyOfRange(0, 68)
            val b2 = dataCodewords.copyOfRange(68, 136)
            val e1 = computeEcc(b1, 18)
            val e2 = computeEcc(b2, 18)
            val out = IntArray(136 + 36)
            var p = 0
            for (i in 0 until 68) {
                out[p++] = b1[i]
                out[p++] = b2[i]
            }
            for (i in 0 until 18) {
                out[p++] = e1[i]
                out[p++] = e2[i]
            }
            out
        }

        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setFunc(r: Int, c: Int, dark: Boolean) {
            if (r in 0 until size && c in 0 until size) {
                modules[r][c] = dark
                isFunction[r][c] = true
            }
        }

        fun drawFinder(row: Int, col: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val r = row + dr
                    val c = col + dc
                    if (r !in 0 until size || c !in 0 until size) continue
                    val dist = maxOf(kotlin.math.abs(dr - 3), kotlin.math.abs(dc - 3))
                    setFunc(r, c, dist != 2 && dist != 4)
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // Timing patterns
        for (i in 8 until size - 8) {
            setFunc(6, i, i % 2 == 0)
            setFunc(i, 6, i % 2 == 0)
        }

        // Alignment pattern for v2..v6
        if (version >= 2) {
            val pos = when (version) {
                2 -> 18
                3 -> 22
                4 -> 26
                5 -> 30
                else -> 34
            }
            for (dr in -2..2) {
                for (dc in -2..2) {
                    val dist = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
                    setFunc(pos + dr, pos + dc, dist != 1)
                }
            }
        }

        // Reserve format info + dark module
        setFunc(size - 8, 8, true)
        for (i in 0..8) {
            if (i != 6) {
                setFunc(8, i, false)
                setFunc(i, 8, false)
            }
        }
        for (i in 0 until 8) setFunc(8, size - 1 - i, false)
        for (i in 0 until 7) setFunc(size - 1 - i, 8, false)

        // Place data bits (mask 0: (r + c) % 2 == 0)
        val dataBits = BooleanArray(allBytes.size * 8)
        for (i in allBytes.indices) {
            for (b in 0 until 8) {
                dataBits[i * 8 + b] = ((allBytes[i] ushr (7 - b)) and 1) == 1
            }
        }

        var bitIdx = 0
        var upward = true
        var col = size - 1
        while (col >= 1) {
            if (col == 6) col--
            for (step in 0 until size) {
                val r = if (upward) size - 1 - step else step
                for (dc in 0..1) {
                    val c = col - dc
                    if (!isFunction[r][c]) {
                        val bit = if (bitIdx < dataBits.size) dataBits[bitIdx++] else false
                        val masked = bit xor (((r + c) and 1) == 0)
                        modules[r][c] = masked
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // Write Format Info for ECC-L (01) + Mask 0 (000) -> 15-bit BCH: 0x77C4 (before XOR 0x5412 = 111011111000100)
        // Format string for ECC L, mask 0: 111011111000100
        val fmtBits = 0b111011111000100
        for (i in 0..5) {
            val bit = ((fmtBits ushr (14 - i)) and 1) == 1
            modules[8][i] = bit
            modules[size - 1 - i][8] = bit
        }
        modules[8][7] = ((fmtBits ushr 8) and 1) == 1
        modules[size - 7][8] = ((fmtBits ushr 8) and 1) == 1
        modules[8][8] = ((fmtBits ushr 7) and 1) == 1
        modules[8][size - 8] = ((fmtBits ushr 7) and 1) == 1
        modules[7][8] = ((fmtBits ushr 6) and 1) == 1
        modules[8][size - 7] = ((fmtBits ushr 6) and 1) == 1
        for (i in 0..5) {
            val bit = ((fmtBits ushr (5 - i)) and 1) == 1
            modules[5 - i][8] = bit
            modules[8][size - 6 + i] = bit
        }

        return modules
    }

    fun toBitmap(text: String, pixelSize: Int = 512): Bitmap {
        val matrix = encode(text)
        val n = matrix.size
        val quiet = 4
        val totalModules = n + quiet * 2
        val scale = maxOf(4, pixelSize / totalModules)
        val bmpSize = totalModules * scale
        val bitmap = Bitmap.createBitmap(bmpSize, bmpSize, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(AndroidColor.WHITE)
        for (r in 0 until n) {
            for (c in 0 until n) {
                if (matrix[r][c]) {
                    val x0 = (c + quiet) * scale
                    val y0 = (r + quiet) * scale
                    for (dy in 0 until scale) {
                        for (dx in 0 until scale) {
                            bitmap.setPixel(x0 + dx, y0 + dy, AndroidColor.BLACK)
                        }
                    }
                }
            }
        }
        return bitmap
    }
}
