package com.kkozhakin.ryvok1.ui.home

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.kkozhakin.ryvok1.*
import java.time.format.DateTimeFormatter

class SleepFragment : Fragment() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_sleep, container, false)

        sleepBarChart = view.findViewById(R.id.sleepBarChart)

        val btn = view.findViewById<Button>(R.id.bDateSleep)
        if (btn.text.isNullOrBlank()) {
            btn.text = selected_date.format(DateTimeFormatter.ofPattern("dd LLLL yyyy"))
        }
        update_sleep_bar_chart()

        return view
    }
}