package com.kushraj.airfoilappv1.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kushraj.airfoilappv1.R
import com.kushraj.airfoilappv1.data.AirfoilRepo

class AirfoilFragment : Fragment() {

    private var camberLineView: CamberLineView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.airfoil_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        camberLineView = view.findViewById(R.id.camberLineView)
        refreshFromRepo()
    }

    override fun onResume() {
        super.onResume()
        refreshFromRepo()
    }

    fun refreshFromRepo() {
        val lineView = camberLineView ?: return

        if (!AirfoilRepo.hasCurrentAirfoil) {
            lineView.clearAll()
            return
        }

        lineView.setAirfoilParams(
            AirfoilRepo.currentM,
            AirfoilRepo.currentP,
            AirfoilRepo.currentT
        )

        lineView.setAngle(AirfoilRepo.currentAlphaDeg)
        lineView.setOverlays(AirfoilRepo.currentOverlays)
        lineView.setShowCurrent(AirfoilRepo.showCurrent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        camberLineView = null
    }
}