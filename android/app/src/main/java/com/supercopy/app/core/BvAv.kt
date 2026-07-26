package com.supercopy.app.core

/** B站 BV ⇄ AV 视频号互转（经典 58 进制 XOR 算法），移植自网页版。 */
object BvAv {
    private const val TABLE = "fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF"
    private val S = intArrayOf(11, 10, 3, 8, 4, 6) // BV 字符串中编码字符的位置
    private const val XOR = 177451812
    private const val ADD = 8728348608L

    private fun pow58(i: Int): Long {
        var r = 1L
        repeat(i) { r *= 58 }
        return r
    }

    fun bvToAv(bv: String): String? {
        var sum = 0L
        for (i in 0 until 6) {
            val idx = TABLE.indexOf(bv[S[i]])
            if (idx == -1) return null
            sum += idx * pow58(i)
        }
        // JS 位运算是 32 位截断，用 toInt() 复刻同样语义
        val av = (sum - ADD).toInt() xor XOR
        if (av <= 0) return null
        return "av$av"
    }

    fun avToBv(av: String): String? {
        val num = av.replace(Regex("av", RegexOption.IGNORE_CASE), "").toIntOrNull() ?: return null
        if (num <= 0) return null
        var n = (num xor XOR).toLong() + ADD
        val result = "BV1  4 1 7  ".toCharArray()
        for (i in 0 until 6) {
            result[S[i]] = TABLE[((n / pow58(i)) % 58).toInt()]
        }
        return String(result)
    }

    /** 文本级双向转换：BV→AV 后，刚生成的 AV 不再被反向转换 */
    fun convert(text: String): String {
        val convertedFromBv = mutableSetOf<String>()
        var result = Regex("BV1[a-zA-Z0-9]{9}", RegexOption.IGNORE_CASE).replace(text) { m ->
            if (m.value.length != 12) return@replace m.value
            val av = bvToAv(m.value)
            if (av != null) convertedFromBv.add(av.lowercase())
            av ?: m.value
        }
        result = Regex("\\bav(\\d+)\\b", RegexOption.IGNORE_CASE).replace(result) { m ->
            if (m.value.lowercase() in convertedFromBv) return@replace m.value
            avToBv(m.value) ?: m.value
        }
        return result
    }
}
