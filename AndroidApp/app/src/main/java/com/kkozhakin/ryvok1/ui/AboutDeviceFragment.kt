package com.kkozhakin.ryvok1.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kkozhakin.ryvok1.R
import com.kkozhakin.ryvok1.battery_percent
import com.kkozhakin.ryvok1.databinding.FragmentAboutDeviceBinding
import kotlin.concurrent.timer

class AboutDeviceFragment : Fragment() {

    private var _binding: FragmentAboutDeviceBinding? = null
    private var batteryUpdateViewFlag: Boolean = false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAboutDeviceBinding.inflate(inflater, container, false)

        if (!batteryUpdateViewFlag) {
            timer("BatteryUpdateView", true, 0, 15000) {
                if (_binding != null) {
                    updateBaterryView()
                }
            }
            batteryUpdateViewFlag = true
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateBaterryView() {
        val root: View = binding.root

        root.findViewById<TextView>(R.id.battery).text = if (battery_percent >= 100) {
            "100%"
        }
        else if (battery_percent <= 0) {
            "0%"
        }
        else {
            "$battery_percent%"
        }
    }
}