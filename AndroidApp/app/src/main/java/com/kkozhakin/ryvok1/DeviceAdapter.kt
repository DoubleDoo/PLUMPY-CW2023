package com.kkozhakin.ryvok1

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.RequiresPermission

class DeviceAdapter(context: Context, devices: ArrayList<BluetoothDevice>) :
    ArrayAdapter<BluetoothDevice>(context, android.R.layout.simple_list_item_2, devices) {

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val device: BluetoothDevice? = getItem(position)
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_2, null)
        }
        (convertView!!.findViewById<View>(android.R.id.text2) as TextView).text = device!!.address
        (convertView.findViewById<View>(android.R.id.text1) as TextView).text = if (device.name != null) {
            device.name
        } else {
            "N/A"
        }
        return convertView
    }
}