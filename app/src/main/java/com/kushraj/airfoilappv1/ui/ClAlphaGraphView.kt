package com.kushraj.airfoilappv1.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import com.kushraj.airfoilappv1.geometry.Naca4Params
import com.kushraj.airfoilappv1.math.ThinAirfoilTheory
import kotlin.math.PI

class ClAlphaGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private fun clFromAlpha(alphaDeg: Double, alphaL0Deg: Double): Double {
        val alphaRad = alphaDeg * PI / 180.0
        val alphaL0Rad = alphaL0Deg * PI / 180.0
        return 2.0 * PI * (alphaRad - alphaL0Rad)
    }

    //region set variables
    private var alphaL0Deg = 0.0
    private var currentAlphaDeg = 0.0
    private var currentCl = 0.0
    private var hasPoint = false
    //endregion

    fun setGraphData(alphaL0Deg: Double, currentAlphaDeg: Double, currentCl: Double) {
        this.alphaL0Deg = alphaL0Deg
        this.currentAlphaDeg = currentAlphaDeg
        this.currentCl = currentCl
        this.hasPoint = true
        invalidate()
    }

    fun clearGraph() {
        alphaL0Deg = 0.0
        currentAlphaDeg = 0.0
        currentCl = 0.0
        hasPoint = false
        invalidate()
    }

    private var overlays: List<CamberLineView.OverlaySpec> = emptyList()
    fun setOverlays(list: List<CamberLineView.OverlaySpec>) {
        overlays = list.toList()
        invalidate()
    }

    //region Paint colours

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val pointOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 30f
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 30f
    }

    private val legendBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 220
        style = Paint.Style.FILL
    }

    private val legendBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val legendSwatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    //endregion

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun drawLegend(canvas: Canvas) {
        if (overlays.isEmpty()) return

        //region set legend region box
        val pad = 20f
        val boxPad = 16f
        val lineH = 40f
        val swatch = 24f
        val gap = 12f

        val lines = overlays.map { "${it.code}  ${it.alphaDeg}°" }
        val maxTextW = lines.maxOf { legendTextPaint.measureText(it) }
        val boxW = boxPad * 2 + swatch + gap + maxTextW
        val boxH = boxPad * 2 + lineH * lines.size

        val right = width - pad
        val top = pad
        val left = right - boxW
        val bottom = top + boxH
        //endregion

        canvas.drawRoundRect(left, top, right, bottom, 16f, 16f, legendBoxPaint)
        canvas.drawRoundRect(left, top, right, bottom, 16f, 16f, legendBorderPaint)

        overlays.forEachIndexed { i, ov ->
            val y = top + boxPad + (i + 0.75f) * lineH
            val sx = left + boxPad
            val sy = y - swatch * 0.75f

            legendSwatchPaint.color = ov.color
            canvas.drawRoundRect(sx, sy, sx + swatch, sy + swatch, 6f, 6f, legendSwatchPaint)
            canvas.drawText("${ov.code}  ${ov.alphaDeg}°", sx + swatch + gap, y, legendTextPaint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //region set plot box region
        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0f || h <= 0f) return

        val left = w * 0.14f
        val right = w * 0.94f
        val top = h * 0.10f
        val bottom = h * 0.88f

        val plotWidth = right - left
        val plotHeight = bottom - top
        //endregion

        // X range in degrees
        val alphaMin = -10.0
        val alphaMax = 10.0

        // Y range in Cl
        val clMin = -1.5
        val clMax = 1.5

        fun mapX(alphaDeg: Double): Float {
            return (left + ((alphaDeg - alphaMin) / (alphaMax - alphaMin)) * plotWidth).toFloat()
        }

        fun mapY(cl: Double): Float {
            return (bottom - ((cl - clMin) / (clMax - clMin)) * plotHeight).toFloat()
        }

        // Axis positions for Cl=0 and alpha=0
        val xAxisY = mapY(0.0)
        val yAxisX = mapX(0.0)

        // Draw axes
        canvas.drawLine(left, xAxisY, right, xAxisY, axisPaint)
        canvas.drawLine(yAxisX, top, yAxisX, bottom, axisPaint)

        val alphaL0X = mapX(alphaL0Deg)

        canvas.drawLine(
            alphaL0X,
            xAxisY - 12f,
            alphaL0X,
            xAxisY + 12f,
            tickPaint
        )

        canvas.drawText(
            "Lift slope: 2π per rad",
            left + 10f,
            top + 30f,
            textPaint
        )


        // Draw x ticks and labels
        for (a in -10..10 step 5) {
            val x = mapX(a.toDouble())
            canvas.drawLine(x, xAxisY - 10f, x, xAxisY + 10f, tickPaint)
            canvas.drawText("$a", x - 12f, xAxisY + 38f, textPaint)
        }

        // Draw y ticks and labels
        val yTicks = listOf(-1.0, -0.5, 0.5, 1.0)
        for (cl in yTicks) {
            val y = mapY(cl)
            canvas.drawLine(yAxisX - 10f, y, yAxisX + 10f, y, tickPaint)
            canvas.drawText(cl.toString(), yAxisX + 16f, y + 10f, textPaint)
        }

        // Axis labels
        canvas.drawText("α (deg)", right - 70f, xAxisY - 16f, textPaint)
        canvas.drawText("Cl", yAxisX + 14f, top + 30f, textPaint)

        // Build thin airfoil graph line: Cl = 2π(α - αL0), alpha in radians
        val samples = 200


        // Draw overlays first
        for (ov in overlays) {
            val params = Naca4Params.Companion.parse(ov.code) ?: continue
            val result = ThinAirfoilTheory.solveNaca4(params, 0.0)
            val alphaL0DegOverlay = result.alphaL0 * 180.0 / PI
            overlayPaint.color = ov.color

            val overlayPath = Path()
            for (i in 0..samples) {
                val alphaDeg = alphaMin + i.toDouble() * (alphaMax - alphaMin) / samples
                val cl = clFromAlpha(alphaDeg, alphaL0DegOverlay)

                val x = mapX(alphaDeg)
                val y = mapY(cl)

                if (i == 0) overlayPath.moveTo(x, y) else overlayPath.lineTo(x, y) }
            overlayPaint.strokeWidth =
                if (ov == overlays.last()) 6f else 3f
            canvas.drawPath(overlayPath, overlayPaint)
        }
        if (hasPoint) {
            val px = mapX(currentAlphaDeg)
            val py = mapY(currentCl)

            canvas.drawCircle(px, py, 12f, pointPaint)
            canvas.drawCircle(px, py, 12f, pointOutlinePaint)
        }

        drawLegend(canvas)
    }
}