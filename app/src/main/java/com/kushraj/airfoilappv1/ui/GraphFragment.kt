package com.kushraj.airfoilappv1.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kushraj.airfoilappv1.R
import com.kushraj.airfoilappv1.data.AirfoilRepo

class GraphFragment : Fragment() {

    private var graphView: ClAlphaGraphView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.graph_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        graphView = view.findViewById(R.id.graphView)
        refreshFromRepo()
    }

    override fun onResume() {
        super.onResume()
        refreshFromRepo()
    }

    fun refreshFromRepo() {
        val gv = graphView ?: return

        if (!AirfoilRepo.hasCurrentAirfoil) {
            gv.clearGraph()
            gv.setOverlays(emptyList())
            return
        }

        gv.setGraphData(
            AirfoilRepo.currentAlphaL0Deg,
            AirfoilRepo.currentAlphaDeg,
            AirfoilRepo.currentCl
        )

        gv.setOverlays(AirfoilRepo.currentOverlays)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        graphView = null
    }
}