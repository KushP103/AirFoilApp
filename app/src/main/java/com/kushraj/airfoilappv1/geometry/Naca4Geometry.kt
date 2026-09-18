package com.kushraj.airfoilappv1.geometry

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Naca4Geometry {

    data class AirfoilSurfaces(
        val upper: List<Pair<Double, Double>>,
        val lower: List<Pair<Double, Double>>
    )

    /** Mean camber line yc(x) and slope dyc/dx at chordwise x in [0,1]. */
    fun camberAndSlope(x: Double, m: Double, p: Double): Pair<Double, Double> {
        if (m == 0.0 || p == 0.0) return 0.0 to 0.0

        return if (x < p) {
            val yc = (m / (p * p)) * (2.0 * p * x - x * x)
            val dy = (2.0 * m / (p * p)) * (p - x)
            yc to dy
        } else {
            val denom = (1.0 - p) * (1.0 - p)
            val yc = (m / denom) * ((1.0 - 2.0 * p) + 2.0 * p * x - x * x)
            val dy = (2.0 * m / denom) * (p - x)
            yc to dy
        }
    }

    /** NACA 4-digit thickness distribution y_t(x). */
    fun thicknessYt(x: Double, t: Double): Double {
        val term =
            0.2969 * sqrt(x) -
                    0.1260 * x -
                    0.3516 * x * x +
                    0.2843 * x * x * x -
                    0.1015 * x * x * x * x
        return 5.0 * t * term
    }

    /** Camber line points (x, yc) for drawing. */
    fun camberLinePoints(m: Double, p: Double, steps: Int = 300): List<Pair<Double, Double>> {
        return (0..steps).map { i ->
            val x = i.toDouble() / steps
            val (yc, _) = camberAndSlope(x, m, p)
            x to yc
        }
    }

    fun camberLinePoints(params: Naca4Params, steps: Int = 300): List<Pair<Double, Double>> =
        camberLinePoints(params.m, params.p, steps)

    /** Upper and lower surface points (x, y) in chord coordinates. */
    fun surfaces(m: Double, p: Double, t: Double, steps: Int = 300): AirfoilSurfaces {
        val upper = ArrayList<Pair<Double, Double>>(steps + 1)
        val lower = ArrayList<Pair<Double, Double>>(steps + 1)

        for (i in 0..steps) {
            val x = i.toDouble() / steps
            val (yc, dy) = camberAndSlope(x, m, p)
            val theta = atan(dy)
            val yt = thicknessYt(x, t)

            val xu = x - yt * sin(theta)
            val yu = yc + yt * cos(theta)

            val xl = x + yt * sin(theta)
            val yl = yc - yt * cos(theta)

            upper.add(xu to yu)
            lower.add(xl to yl)
        }

        return AirfoilSurfaces(upper, lower)
    }

    fun surfaces(params: Naca4Params, steps: Int = 300): AirfoilSurfaces =
        surfaces(params.m, params.p, params.t, steps)
}