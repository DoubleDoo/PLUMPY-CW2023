package com.kkozhakin.ryvok1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class DeviceConnectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_connection)
    }
    fun newConnectionOnClick(view: View) {
        startActivity(Intent(this, SelectDeviceActivity::class.java))
    }
}