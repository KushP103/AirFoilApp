package com.kushraj.airfoilappv1.math

import com.kushraj.airfoilappv1.geometry.Naca4Params
import kotlin.math.PI
import kotlin.math.cos
import android.util.Log

object ThinAirfoilTheory {

    data class Result(
        val I0: Double,          // ∫ dz/dx dθ
        val Icos: Double,        // ∫ dz/dx cosθ dθ
        val A1: Double,          // (2/π) Icos
        var alphaL0: Double,     // (1/π) I0 - A1/2   (radians)
        var cl: Double           // 2π(α - αL0)
    )

    fun integrateTrapz(a: Double, b: Double, n: Int = 3000, f: (Double) -> Double): Double {
        val h = (b - a) / n
        var sum = 0.0
        for (i in 0..n) {
            val x = a + i * h
            val w = if (i == 0 || i == n) 0.5 else 1.0
            sum += w * f(x)
        }
        return sum * h
    }

    private fun dzdxNaca4(m: Double, p: Double, x: Double): Double {
        if (m == 0.0) return 0.0
        return if (x < p)
            (2.0 * m / (p * p)) * (p - x)
        else
            (2.0 * m / ((1.0 - p) * (1.0 - p))) * (p - x)
    }

    /**
     * Computes thin-airfoil theory using the standard Fourier-coefficient route:
     * I0   = ∫0^π dz/dx dθ
     * Icos = ∫0^π dz/dx cosθ dθ
     * A1   = (2/π) Icos
     * αL0  = (1/π) I0 - A1/2
     * Cl   = 2π(α - αL0)
     */
    fun solveNaca4(params: Naca4Params, alphaRad: Double, n: Int = 100): Result {

        val m = params.m
        val p = params.p

        Log.d("AIRFOIL_DEBUG", "Input m=$m p=$p alpha(rad)=$alphaRad")

        val I0 = integrateTrapz(0.0, PI, n) { theta ->
            val x = 0.5 * (1.0 - cos(theta))
            dzdxNaca4(m, p, x)
        }

        Log.d("AIRFOIL_DEBUG", "I0 integral = $I0")

        val Icos = integrateTrapz(0.0, PI, n) { theta ->
            val x = 0.5 * (1.0 - cos(theta))
            dzdxNaca4(m, p, x) * cos(theta)
        }

        Log.d("AIRFOIL_DEBUG", "Icos integral = $Icos")

        val A1 = (2.0 / PI) * Icos
        Log.d("AIRFOIL_DEBUG", "A1 coefficient = $A1")

        val alphaL0 = (1.0 / PI) * I0 - 0.5 * A1
        Log.d("AIRFOIL_DEBUG", "Alpha L0 (rad) = $alphaL0")

        val cl = 2.0 * PI * (alphaRad - alphaL0)
        Log.d("AIRFOIL_DEBUG", "Cl = $cl")

        return Result(I0, Icos, A1, alphaL0, cl)
    }



    // Convenience overload if you already have alpha in degrees
    fun solveNaca4Deg(params: Naca4Params, alphaDeg: Double, n: Int = 5000): Result {
        val alphaRad = alphaDeg * PI / 180.0
        return solveNaca4(params, alphaRad, n)
    }
}