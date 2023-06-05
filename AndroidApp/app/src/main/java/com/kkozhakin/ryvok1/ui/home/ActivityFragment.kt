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

class ActivityFragment : Fragment() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_activity, container, false)

        activityBarChart = view.findViewById(R.id.activityBarChart)

        val btn = view.findViewById<Button>(R.id.bDateActivity)
        if (btn.text.isNullOrBlank()) {
            btn.text = selected_date.format(DateTimeFormatter.ofPattern("dd LLLL yyyy"))
        }
        update_activity_bar_chart()

        return view
    }
}