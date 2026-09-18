package com.kushraj.airfoilappv1.ui

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.kushraj.airfoilappv1.R.id
import com.kushraj.airfoilappv1.math.ThinAirfoilTheory
import java.math.RoundingMode
import kotlin.math.PI
import android.graphics.Color
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kushraj.airfoilappv1.geometry.Naca4Params
import com.kushraj.airfoilappv1.R
import com.kushraj.airfoilappv1.data.AirfoilRepo
import kotlin.math.abs

@SuppressLint("SetTextI18n")

class MainActivity : AppCompatActivity() {

    //  ACTIVITY_MAIN VARIABLE INITIALISATION  //

    private lateinit var etDigitInput: EditText
    private lateinit var etAngleOfAttack: EditText
    private lateinit var btnEnter: Button
    private lateinit var tvCl: TextView
    private lateinit var tvZeroLiftAngle: TextView
    private lateinit var btnClear: Button
    private lateinit var btnLogResults: Button
    private lateinit var btnSelector: Button
    private lateinit var btnClearLog: Button

    private fun refreshOverlays() {
        val overlayColors = listOf(
            Color.MAGENTA,
            Color.BLUE,
            Color.GREEN,
            Color.CYAN,
            Color.RED
        )

        val overlays = AirfoilRepo.history
            .filter { it.enabled }
            .take(5)
            .mapIndexed { idx, e ->
                CamberLineView.OverlaySpec(
                    code = e.code,
                    m = e.m,
                    p = e.p,
                    t = e.t,
                    color = overlayColors[idx],
                    alphaDeg = e.alphaDeg
                )
            }

        AirfoilRepo.currentOverlays = overlays
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)     //|
        enableEdgeToEdge()                     //| Set up the window/ bridge to .xml file
        setContentView(R.layout.activity_main) //|

        //The values come from the inputs in the edittext windows
        etDigitInput = findViewById(id.etDigitInput)
        etAngleOfAttack = findViewById(id.etAngleOfAttack)
        btnEnter = findViewById(id.btnEnter)
        tvCl = findViewById(id.tvCl)
        tvZeroLiftAngle = findViewById(id.tvZeroLiftAngle)
        btnClear = findViewById(id.btnClear)
        btnLogResults = findViewById(id.btnLogResults)
        btnSelector = findViewById(id.btnSelector)
        btnClearLog = findViewById(id.btnClearLog)

        val viewPager = findViewById<ViewPager2>(id.viewPager)
        val tabLayout = findViewById<TabLayout>(id.tabLayout)

        val pagerAdapter = VisualPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Airfoil" else "Cl vs α"
        }.attach()

        btnClear.setOnClickListener {
            etDigitInput.text.clear()
            etAngleOfAttack.text.clear()
            tvCl.text = ""
            tvZeroLiftAngle.text = ""

            pagerAdapter.airfoilFragment.refreshFromRepo()
            pagerAdapter.graphFragment.refreshFromRepo()
        }

        btnClearLog.setOnClickListener {
            AirfoilRepo.history.clear()
            AirfoilRepo.showCurrent = true
            AirfoilRepo.currentOverlays = emptyList()
            AirfoilRepo.hasCurrentAirfoil = false
            pagerAdapter.airfoilFragment.refreshFromRepo()
            pagerAdapter.graphFragment.refreshFromRepo()
        }

        btnLogResults.setOnClickListener {
            val nacaCode = etDigitInput.text.toString().trim()
            val alphaStr = etAngleOfAttack.text.toString().trim()
            val alpha = alphaStr.toDoubleOrNull()

            val params = Naca4Params.Companion.parse(nacaCode) ?: return@setOnClickListener
            if (alpha == null) return@setOnClickListener

            val alreadyExists = AirfoilRepo.history.any {
                it.code == nacaCode && abs(it.alphaDeg - alpha) < 1e-6
            }

            if (!alreadyExists) {
                AirfoilRepo.history.add(
                    AirfoilRepo.AirfoilEntry(
                        code = nacaCode,
                        m = params.m,
                        p = params.p,
                        t = params.t,
                        alphaDeg = alpha,
                        enabled = false
                    )
                )
            }else {
                Toast.makeText(this, "This airfoil and angle are already logged.", Toast.LENGTH_SHORT).show()
            }
            refreshOverlays()
            pagerAdapter.airfoilFragment.refreshFromRepo()
            pagerAdapter.graphFragment.refreshFromRepo()
        }

        btnSelector.setOnClickListener {

            if (AirfoilRepo.history.isEmpty()) return@setOnClickListener

            val items = AirfoilRepo.history.map { e ->
                val params = Naca4Params.Companion.parse(e.code)

                if (params == null) {
                    "${e.code}   α=${e.alphaDeg}°   invalid"
                } else {
                    val alphaRad = e.alphaDeg * PI / 180.0
                    val result = ThinAirfoilTheory.solveNaca4(params, alphaRad)

                    val clRounded = result.cl
                        .toBigDecimal()
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble()

                    val alphaL0Deg = (result.alphaL0 * 180.0 / PI)
                        .toBigDecimal()
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble()

                    "${e.code}   ${e.alphaDeg}°   Cl ${clRounded}   α0 ${alphaL0Deg}°"
                }
            }.toTypedArray()

            val checked = AirfoilRepo.history.map { it.enabled }.toBooleanArray()

            AlertDialog.Builder(this)
                .setTitle("Select airfoils to overlay")
                .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                    AirfoilRepo.history[which].enabled = isChecked
                }
                .setPositiveButton("Apply") { _, _ ->
                    refreshOverlays()
                    pagerAdapter.airfoilFragment.refreshFromRepo()
                    pagerAdapter.graphFragment.refreshFromRepo()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnEnter.setOnClickListener {
            val nacaCode = etDigitInput.text.toString().trim()
            val alphaStr = etAngleOfAttack.text.toString().trim()
            val alpha = alphaStr.toDoubleOrNull()

            val params = Naca4Params.Companion.parse(nacaCode) ?: run {
                Toast.makeText(this, "NACA code must be 4 digits (e.g. 2412).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (alpha == null) {
                Toast.makeText(this, "Angle of attack must be a number.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val alphaRad = alpha * PI / 180.0
            val r = ThinAirfoilTheory.solveNaca4(params, alphaRad)
            var alphaL0deg = r.alphaL0 * 180.0 / PI
            alphaL0deg = alphaL0deg.toBigDecimal().setScale(4, RoundingMode.HALF_UP).toDouble()
            r.cl = r.cl.toBigDecimal().setScale(4, RoundingMode.HALF_UP).toDouble()

            AirfoilRepo.currentM = params.m
            AirfoilRepo.currentP = params.p
            AirfoilRepo.currentT = params.t
            AirfoilRepo.currentAlphaDeg = alpha
            AirfoilRepo.currentAlphaL0Deg = alphaL0deg
            AirfoilRepo.currentCl = r.cl
            AirfoilRepo.hasCurrentAirfoil = true
            AirfoilRepo.showCurrent = true

            tvCl.text = "Cl: ${r.cl}"
            tvZeroLiftAngle.text = "Alpha L0: ${alphaL0deg}°"

            pagerAdapter.airfoilFragment.refreshFromRepo()
            pagerAdapter.graphFragment.refreshFromRepo()

        }

    }
}



