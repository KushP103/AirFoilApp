package com.kushraj.airfoilappv1.geometry

data class Naca4Params(val m: Double, val p: Double, val t: Double) {



    companion object {

        val DEFAULT = Naca4Params(0.0, 0.0, 0.12)

        fun parse(code: String): Naca4Params? {

            val s = code.trim()
            if (s.length != 4 || !s.all { it.isDigit() }) return null

            val d1 = s[0].digitToInt()
            val d2 = s[1].digitToInt()

            val m = d1 / 100.0
            val p = d2 / 10.0
            val t = s.substring(2, 4).toInt() / 100.0

            if (m != 0.0 && (p <= 0.0 || p >= 1.0)) return null

            return Naca4Params(m, p, t)
        }

    }

}