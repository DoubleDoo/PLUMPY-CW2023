package com.kkozhakin.ryvok1

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

var data_result: java.util.ArrayList<java.util.ArrayList<String>> = arrayListOf()
lateinit var personalData: Map<String, String?>
var battery_percent: Int = 0

var data_type: String = "day"
@RequiresApi(Build.VERSION_CODES.O)
var selected_date: LocalDate = LocalDate.now()
var activityBarChart: BarChart? = null
var sleepBarChart: HorizontalBarChart? = null
@SuppressLint("StaticFieldLeak")
var generalActivity: GeneralActivity? = null

@RequiresApi(Build.VERSION_CODES.S)
fun update_activity_bar_chart(){
    if (activityBarChart != null) {
        val runEntriesList = arrayListOf<BarEntry>()
        val walkEntriesList = arrayListOf<BarEntry>()
        val barDataSets = arrayListOf<BarDataSet>()
        val r = mutableMapOf<Int, Int>()
        val w = mutableMapOf<Int, Int>()

        when(data_type) {
            "day" -> {
                for (i in activityResult){
                    var time: LocalDateTime
                    try {
                        time = LocalDateTime.parse(i.key)
                    }
                    catch (e: Exception){
                        continue
                    }
                    if ((selected_date.year == time.year) and (selected_date.monthValue == time.monthValue) and (selected_date.dayOfMonth == time.dayOfMonth)){
                        if (i.value == 2){
                            r.compute(time.hour) { _, v -> v?.plus(1) ?: 1  }
                        }
                        if (i.value == 1){
                            w.compute(time.hour) { _, v -> v?.plus(1) ?: 1  }
                        }
                    }
                }

                for (i in 0..23){
                    runEntriesList.add(BarEntry(i.toFloat(), r.getOrDefault(i, 0) * 3f / 60))
                }

                barDataSets.add(BarDataSet(runEntriesList, "").apply {
                    valueTextColor = Color.BLACK
                    valueTextSize = 0f
                    color = Color.BLUE
                })

                for (i in 0..23){
                    walkEntriesList.add(BarEntry(i.toFloat(), (r.getOrDefault(i, 0) + w.getOrDefault(i, 0)) * 3f / 60))
                }

                barDataSets.add(BarDataSet(walkEntriesList, "").apply {
                    valueTextColor = Color.BLACK
                    valueTextSize = 0f
                    color = Color.GREEN
                })

                activityBarChart!!.xAxis.labelCount = 24
                activityBarChart!!.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"))
            }
            "month" -> {
                for (i in activityResult){
                    var time: LocalDateTime
                    try {
                        time = LocalDateTime.parse(i.key)
                    }
                    catch (e: Exception){
                        continue
                    }
                    if ((selected_date.year == time.year) and (selected_date.monthValue == time.monthValue)){
                        if (i.value == 2){
                            r.compute(time.dayOfMonth - 1) { _, v -> v?.plus(1) ?: 1  }
                        }
                        if (i.value == 1){
                            w.compute(time.dayOfMonth - 1) { _, v -> v?.plus(1) ?: 1  }
                        }
                    }
                }

                for (i in 0..30){
                    runEntriesList.add(BarEntry(i.toFloat(), r.getOrDefault(i, 0) * 3f / 60))
                }

                barDataSets.add(BarDataSet(runEntriesList, "").apply {
                    valueTextColor = Color.BLACK
                    valueTextSize = 0f
                    color = Color.BLUE
                })

                for (i in 0..30){
                    walkEntriesList.add(BarEntry(i.toFloat(), (r.getOrDefault(i, 0) + w.getOrDefault(i, 0))* 3f / 60))
                }

                barDataSets.add(BarDataSet(walkEntriesList, "").apply {
                    valueTextColor = Color.BLACK
                    valueTextSize = 0f
                    color = Color.GREEN
                })
                activityBarChart!!.xAxis.labelCount = 31
                activityBarChart!!.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31"))
            }
            "year" -> {
                for (i in activityResult){
                    var time: LocalDateTime
                    try {
                        time = LocalDateTime.parse(i.key)
                    }
                    catch (e: Exception){
                        continue
                    }
                    if (selected_date.year == time.year) {
                        if (i.value == 2){
                            r.compute(time.monthValue - 1) { _, v -> v?.plus(1) ?: 1  }
                        }
                        if (i.value == 1){
                            w.compute(time.monthValue - 1) { _, v -> v?.plus(1) ?: 1  }
                        }
                    }
                }

                for (i in 0..11){
                    runEntriesList.add(BarEntry(i.toFloat(), r.getOrDefault(i, 0) * 3f / 60))
                }

                barDataSets.add(BarDataSet(runEntriesList, "").apply {
                    valueTextColor = Color.BLACK
                    valueTextSize = 0f
                    color = Color.BLUE
                })

                for (i in 0..11){
                    walkEntriesList.add(BarEntry(i.toFloat(), (r.getOrDefault(i, 0) + w.getOrDefault(i, 0)) * 3f / 60))
                }

                barDataSets.add(BarDataSet(walkEntriesList, "").apply {
                    valueTextColor = Color.BLACK
                    valueTextSize = 0f
                    color = Color.GREEN
                })
                activityBarChart!!.xAxis.labelCount = 12
                activityBarChart!!.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"))
            }
        }

        val barData = BarData()
        for (ds in barDataSets.reversed()) {
            barData.addDataSet(ds)
        }

        activityBarChart!!.xAxis.position = XAxis.XAxisPosition.BOTTOM
        activityBarChart!!.data = barData
        activityBarChart!!.description.isEnabled = false

        activityBarChart!!.isEnabled = false
        activityBarChart!!.invalidate()
        activityBarChart!!.isEnabled = true

        val runDur = Duration.ofSeconds(r.values.sum() * 3L)
        val walkDur = Duration.ofSeconds(w.values.sum() * 3L)
        generalActivity?.updateTexts("${runDur.toHours()}:${runDur.toMinutesPart()}", "${walkDur.toHours()}:${walkDur.toMinutesPart()}", null, null, null)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
fun update_sleep_bar_chart(){
    if (sleepBarChart != null) {
        val barDataSets = arrayListOf<BarDataSet>()
        val barEntry = BarEntry(0f,0f)
        var last_value = -1
        var fast_time_count = 0
        var slow_time_count = 0

        when(data_type) {
            "day" -> {
                for (i in sleepResult.toSortedMap()){
                    var time: LocalDateTime
                    try {
                        time = LocalDateTime.parse(i.key)
                    }
                    catch (e: Exception){
                        continue
                    }
                    if ((selected_date.year == time.year) and (selected_date.monthValue == time.monthValue) and (selected_date.dayOfMonth == time.dayOfMonth)){
                        if (last_value != i.value) {
                            barDataSets.add(BarDataSet(arrayListOf(barEntry), "").apply {
                                valueTextColor = Color.BLACK
                                valueTextSize = 0f
                                color = if (last_value == 0) {Color.BLUE} else {Color.YELLOW}
                            })
                        }
                        else {
                            barEntry.y += 3f / 60
                        }
                        last_value = i.value
                        if (i.value == 0){
                            fast_time_count += 1
                        }
                        else {
                            slow_time_count += 1
                        }
                    }
                }
            }
            "month" -> {
                for (i in sleepResult.toSortedMap()){
                    var time: LocalDateTime
                    try {
                        time = LocalDateTime.parse(i.key)
                    }
                    catch (e: Exception){
                        continue
                    }
                    if ((selected_date.year == time.year) and (selected_date.monthValue == time.monthValue)){
                        if (last_value != i.value) {
                            barDataSets.add(BarDataSet(arrayListOf(barEntry), "").apply {
                                valueTextColor = Color.BLACK
                                valueTextSize = 0f
                                color = if (last_value == 0) {Color.BLUE} else {Color.YELLOW}
                            })
                        }
                        else {
                            barEntry.y += 3f / 60
                        }
                        last_value = i.value
                        if (i.value == 0){
                            fast_time_count += 1
                        }
                        else {
                            slow_time_count += 1
                        }
                    }
                }
            }
            "year" -> {
                for (i in sleepResult.toSortedMap()){
                    var time: LocalDateTime
                    try {
                        time = LocalDateTime.parse(i.key)
                    }
                    catch (e: Exception){
                        continue
                    }
                    if (selected_date.year == time.year) {
                        if (last_value != i.value) {
                            barDataSets.add(BarDataSet(arrayListOf(barEntry), "").apply {
                                valueTextColor = Color.BLACK
                                valueTextSize = 0f
                                color = if (last_value == 0) {Color.BLUE} else {Color.YELLOW}
                            })
                        }
                        else {
                            barEntry.y += 3f / 60
                        }
                        last_value = i.value
                        if (i.value == 0){
                            fast_time_count += 1
                        }
                        else {
                            slow_time_count += 1
                        }
                    }
                }
            }
        }

        // on below line we are initializing our bar data
        val barData = BarData()
        for (ds in barDataSets.reversed()) {
            barData.addDataSet(ds)
        }

        // on below line we are setting data to our bar chart
        sleepBarChart!!.legend.isEnabled = false
        sleepBarChart!!.data = barData
        sleepBarChart!!.xAxis.isEnabled = false
        sleepBarChart!!.axisRight.isEnabled = false
        sleepBarChart!!.axisLeft.isEnabled = false
        sleepBarChart!!.setDrawValueAboveBar(false)

        // on below line we are enabling description as false
        sleepBarChart!!.description.isEnabled = false
        sleepBarChart!!.setDrawBorders(true)
        sleepBarChart!!.isEnabled = false
        sleepBarChart!!.invalidate()
        sleepBarChart!!.isEnabled = true

        val fastDur = Duration.ofSeconds(fast_time_count * 3L)
        val slowDur = Duration.ofSeconds(slow_time_count * 3L)
        generalActivity?.updateTexts(null, null, "${fastDur.toHours()}:${fastDur.toMinutesPart()}", "${slowDur.toHours()}:${slowDur.toMinutesPart()}", "${(slowDur + fastDur).toHours()}:${(slowDur + fastDur).toMinutesPart()}")
    }
}