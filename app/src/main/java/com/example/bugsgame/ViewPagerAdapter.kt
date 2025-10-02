package com.example.bugsgame

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RegistrationFragment()
            1 -> GameFragment()
            2 -> RulesFragment()
            3 -> AuthorsFragment()
            4 -> SettingsFragment()
            else -> RegistrationFragment()
        }
    }
}