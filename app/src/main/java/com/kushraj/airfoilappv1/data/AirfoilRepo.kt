package com.kushraj.airfoilappv1.data

import com.kushraj.airfoilappv1.ui.CamberLineView

object AirfoilRepo {
    data class AirfoilEntry(
        val code: String,
        val m: Double,
        val p: Double,
        val t: Double,
        var alphaDeg: Double,
        var enabled: Boolean = true,
    )

    var currentOverlays: List<CamberLineView.OverlaySpec> = emptyList()
    val history = mutableListOf<AirfoilEntry>()
    var showCurrent: Boolean = true

    var currentM: Double = 0.0
    var currentP: Double = 0.0
    var currentT: Double = 0.12
    var currentAlphaDeg: Double = 0.0
    var currentAlphaL0Deg: Double = 0.0
    var currentCl: Double = 0.0
    var hasCurrentAirfoil: Boolean = false
}