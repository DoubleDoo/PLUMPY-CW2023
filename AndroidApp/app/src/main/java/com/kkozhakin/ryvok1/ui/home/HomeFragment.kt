package com.kkozhakin.ryvok1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.kkozhakin.ryvok1.R
import com.kkozhakin.ryvok1.SimpleFragmentPagerAdapter
import com.kkozhakin.ryvok1.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Find the view pager that will allow the user to swipe between fragments
        val viewPager = root.findViewById<ViewPager>(R.id.viewpager)
        // Create an adapter that knows which fragment should be shown on each page
        val adapter = SimpleFragmentPagerAdapter(childFragmentManager)
        // Set the adapter onto the view pager
        viewPager.adapter = adapter

        viewPager.currentItem = 0

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}