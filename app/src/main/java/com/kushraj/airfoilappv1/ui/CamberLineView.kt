package com.kushraj.airfoilappv1.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.kushraj.airfoilappv1.geometry.Naca4Geometry
import kotlin.math.cos
import kotlin.math.sin

class CamberLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {


    // Converts the x from 0 - 1 into the screen that goes along the axis
    private fun mapToScreen(
        box: PlotBox,
        pts: List<Pair<Double, Double>>,
        scaleFactor: Float = 0.9f
    ): List<Pair<Float, Float>> {
        return pts.map { (x, y) ->
            val px = box.left + (x.toFloat() * box.chordLen)
            val py = box.chordY - (y.toFloat() * box.chordLen * scaleFactor)
            px to py
        }
    }

    //Turns a list of screen points into a drawable line
    private fun buildPathFrom(screenPts: List<Pair<Float, Float>>) {
        path.reset()
        screenPts.forEachIndexed { i, (px, py) ->
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
    }


    // Small helper so we don't recalc the same bounds everywhere - internal plotting region
    private data class PlotBox(
        val left: Float,
        val right: Float,
        val top: Float,
        val bottom: Float,
        val chordY: Float,
        val chordLen: Float
    )

    //Sets up how big the plotting region is based on current view size
    private fun plotBox(): PlotBox? {
        val w = width.toFloat()
        val h = height.toFloat()

        val left = w * 0.18f
        val right = w * 0.94f
        val top = h * 0.12f
        val bottom = h * 0.88f

        val chordY = (top + bottom) * 0.5f
        val chordLen = right - left
        if (chordLen <= 0f) return null

        return PlotBox(left, right, top, bottom, chordY, chordLen)
    }

    // --- Overlay airfoils drawn behind current
    data class OverlaySpec(
        val code: String,
        val m: Double,
        val p: Double,
        val t: Double,
        val color: Int,
        val alphaDeg: Double
    )

    private var alphaDeg = 0.0
    private var alphaRad = 0.0

    fun setAngle(alphaDeg: Double) {
        this.alphaDeg = alphaDeg
        alphaRad = Math.toRadians(alphaDeg)
        invalidate()
    }

    private var showCurrentAirfoil: Boolean = true
    private var overlays: List<OverlaySpec> = emptyList()

    fun setOverlays(list: List<OverlaySpec>) {
        overlays = list.toList()
        invalidate()
    }

    fun setShowCurrent(show: Boolean) {
        showCurrentAirfoil = show
        invalidate()
    }

    // --- Current airfoil params + current camber points
    private var currentCamberPts: List<Pair<Double, Double>>? = null
    private var mVal = 0.0
    private var pVal = 0.0
    private var tVal = 0.12
    private var showAirfoilShape = false

    // --- Paints
    private val xAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = Color.GRAY
    }

    private val yAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = Color.GRAY
    }

    private val camberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.RED
        pathEffect = DashPathEffect(floatArrayOf(16f, 10f), 0f)
    }

    private val airfoilFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#90CAF9")
    }

    private val airfoilStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.BLACK
    }

    private val overlayStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = 180
        pathEffect = DashPathEffect(floatArrayOf(16f, 10f), 0f)
    }

    private val overlayCamberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        alpha = 200
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 34f
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

    // IMPORTANT: reuse this (don't allocate Paint inside onDraw)
    private val legendSwatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val flowArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.parseColor("#1565C0")
    }

    private val flowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        textSize = 32f
    }
    // Paints

    private fun drawFreestreamArrow(
        canvas: Canvas,
        box: PlotBox,
        alphaDeg: Double,
        color: Int,
        yOffset: Float = 0f
    ) {
        val oldColor = flowArrowPaint.color
        val oldTextColor = flowTextPaint.color

        flowArrowPaint.color = color
        flowTextPaint.color = color
        val angleRad = Math.toRadians(-alphaDeg) // fixed-airfoil convention
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        // Start position near top-left of plot area
        val startX = box.left - 120f
        val startY = box.chordY + yOffset
        val len = 90f
        val endX = startX + len * cosA
        val endY = startY + len * sinA

        // Main arrow line
        canvas.drawLine(startX, startY, endX, endY, flowArrowPaint)

        // Arrowhead
        val headLen = 24f
        val headAngle = Math.toRadians(25.0)
        val backAngle1 = angleRad + Math.PI - headAngle
        val backAngle2 = angleRad + Math.PI + headAngle

        val hx1 = endX + headLen * cos(backAngle1).toFloat()
        val hy1 = endY + headLen * sin(backAngle1).toFloat()

        val hx2 = endX + headLen * cos(backAngle2).toFloat()
        val hy2 = endY + headLen * sin(backAngle2).toFloat()

        canvas.drawLine(endX, endY, hx1, hy1, flowArrowPaint)
        canvas.drawLine(endX, endY, hx2, hy2, flowArrowPaint)

        // Label
        canvas.drawText("V∞", startX, startY - 12f, flowTextPaint)

        flowArrowPaint.color = oldColor
        flowTextPaint.color = oldTextColor
    }

    private val path = Path()

    fun setAirfoilParams(m: Double, p: Double, t: Double) {
        mVal = m
        pVal = p
        tVal = t
        showAirfoilShape = true
        // Keep camber points in sync with params
        currentCamberPts = Naca4Geometry.camberLinePoints(m, p)
        invalidate()
    }

    // --- Drawing
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val box = plotBox() ?: return //Gets the plot box

        // Axes
        canvas.drawLine(box.left, box.chordY, box.right, box.chordY, xAxisPaint)
        canvas.drawLine(box.left, box.top, box.left, box.bottom, yAxisPaint)

        // Loops through all enabled overlays
        // For each one, you set the overlay colour, shape, camber line and free stream arrow
        for ((idx, ov) in overlays.withIndex()) {
            overlayStrokePaint.color = ov.color
            overlayCamberPaint.color = ov.color
            drawFreestreamArrow(
                canvas,
                box,
                alphaDeg = ov.alphaDeg,
                color = ov.color,
                yOffset = idx * 42f
            )


            drawAirfoil(canvas, box, ov.m, ov.p, ov.t, fill = false)
            drawCurve(
                canvas,
                box,
                pts = Naca4Geometry.camberLinePoints(ov.m, ov.p),
                paint = overlayCamberPaint,
                alpha = 220
            )


        }

        //Legend is added to show colours corresponding to overlay
        drawLegend(canvas)
    }

    fun clearAll() {
        currentCamberPts = null
        mVal = 0.0
        pVal = 0.0
        tVal = 0.12
        showAirfoilShape = false
        showCurrentAirfoil = true
        overlays = emptyList()
        alphaDeg = 0.0
        alphaRad = 0.0
        invalidate()
    }

    private fun drawLegend(canvas: Canvas) {
        if (overlays.isEmpty()) return


        val pad = 20f
        val boxPad = 16f
        val lineH = 42f
        val swatch = 26f
        val gap = 14f

        val lines = overlays.map { it.code }
        val maxTextW = lines.maxOf { legendTextPaint.measureText(it) }
        val boxW = boxPad * 2 + swatch + gap + maxTextW
        val boxH = boxPad * 2 + lineH * lines.size

        val right = width - pad
        val top = pad
        val left = right - boxW
        val bottom = top + boxH

        canvas.drawRoundRect(left, top, right, bottom, 18f, 18f, legendBoxPaint)
        canvas.drawRoundRect(left, top, right, bottom, 18f, 18f, legendBorderPaint)

        overlays.forEachIndexed { i, ov ->
            val y = top + boxPad + (i + 0.75f) * lineH
            val sx = left + boxPad
            val sy = y - swatch * 0.75f

            legendSwatchPaint.color = ov.color
            canvas.drawRoundRect(sx, sy, sx + swatch, sy + swatch, 6f, 6f, legendSwatchPaint)

            canvas.drawText(ov.code, sx + swatch + gap, y, legendTextPaint)


        }
    }

    private fun drawAirfoil(canvas: Canvas, box: PlotBox, m: Double, p: Double, t: Double, fill: Boolean) {
        val scaleFactor = 0.85f
        //Gets upper and lower surfaces
        val surfaces = Naca4Geometry.surfaces(m, p, t, steps = 400)
        //maps the surfaces to the screen
        val upperScreen = mapToScreen(box, surfaces.upper, scaleFactor)
        val lowerScreen = mapToScreen(box, surfaces.lower, scaleFactor)
        path.reset()
        // Upper forward
        upperScreen.forEachIndexed { i, (px, py) ->
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py) }
        // Lower backward
        for (i in lowerScreen.size - 1 downTo 0) {
            val (px, py) = lowerScreen[i]
            path.lineTo(px, py) }
        //Finishes drawing
        path.close()
        canvas.drawPath(path, airfoilStrokePaint)
    }


    private fun drawCurve(
        canvas: Canvas,
        box: PlotBox,
        pts: List<Pair<Double, Double>>,
        paint: Paint,
        alpha: Int
    ) {
        val oldAlpha = paint.alpha
        paint.alpha = alpha

        val screenPts = mapToScreen(box, pts)
        buildPathFrom(screenPts)

        canvas.drawPath(path, paint)
        paint.alpha = oldAlpha
    }
}