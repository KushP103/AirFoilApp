package com.kushraj.airfoilappv1.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class VisualPagerAdapter(activity: AppCompatActivity) :
    FragmentStateAdapter(activity) {

    val airfoilFragment = AirfoilFragment()
    val graphFragment = GraphFragment()

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> airfoilFragment
            else -> graphFragment
        }
    }
}