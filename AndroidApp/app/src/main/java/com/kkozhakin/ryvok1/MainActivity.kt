package com.kkozhakin.ryvok1

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED


class MainActivity : AppCompatActivity() {

    private var handler: Handler = Handler()
    private var REQUEST_PERMISSION = 1

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun getPairDevise(bluetoothAdapter: BluetoothAdapter) {
        val pairedDevices = bluetoothAdapter.bondedDevices
        var f = false
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
//                if (device.uuids != null) {
//                    if (ParcelUuid_SERVICE in device.uuids) {
                    if (device.name == "RYVOK-1") {
                        f = true
                        val intent = Intent(
                            this,
                            GeneralActivity::class.java
                        )
                        intent.putExtra(
                            GeneralActivity.EXTRAS_DEVICE_NAME,
                            device.name
                        )
                        intent.putExtra(
                            GeneralActivity.EXTRAS_DEVICE_ADDRESS,
                            device.address
                        )
                        intent.putExtra(
                            GeneralActivity.DEVICE_PAIRED,
                            true
                        )
                        startActivity(intent)
                        finish()
                    }
//                }
            }
        }

        if (!f) {
            val intent = Intent(this@MainActivity, DeviceConnectionActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        activityModelPath = assetFilePath(this, "activity_lstm.ptl")
        sleepModelPath = assetFilePath(this, "sleep_lstm.ptl")

        Log.i("NumberGenerated", "Function has generated zero.")
        checkPermisions()
    }
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<String>, grantResults: IntArray) {
        Log.i("TEST", "start")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var boolRes = true
        for (res in grantResults) {
            Log.i("TEST", res.toString())
            if (res != PERMISSION_GRANTED ) {
                boolRes = false
                break
            }
            }
        Log.i("TEST", "______________________-")
        if(boolRes) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val mBluetoothAdapter = bluetoothManager.adapter

            handler.postDelayed({
                getPairDevise(mBluetoothAdapter)
            }, 2000)
        }
    }

    //Todo Дописать запрос на вулючение блютуза, GPS
    //Todo Ожиддание принятия всех разрешений прежде чем переходить в новый активити
    //Todo При отказе подключения, при поаторном запросе дожжна быть врозможность выбрать
    //Todo WRITE_EXTERNAL_STORAGE
    private fun checkPermisions():Boolean {
//        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//        requestBluetooth.launch(enableBtIntent)
        val lostPermissions: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { //NEW android
//            Toast.makeText(this, "NEW", Toast.LENGTH_SHORT).show()
            lostPermissions=requestPermissions(arrayOf(
//                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
        else { //OLD android
//            Toast.makeText(this, "OLD", Toast.LENGTH_SHORT).show()
            lostPermissions=requestPermissions(arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
        return lostPermissions
    }

//    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//        if (result.resultCode == RESULT_OK) {
//            Toast.makeText(this, "Request BLE ON", Toast.LENGTH_SHORT).show()
//        }else{
//            Toast.makeText(this, "Request BLE ON FAILED", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun requestPermissions(permissions: Array<String>):Boolean {
        val lostPermissions = ArrayList<String>()
        for (perm in permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_DENIED) {
                lostPermissions.add(perm)
            }
        }
        if(lostPermissions.isNotEmpty()){
            ActivityCompat.requestPermissions(this, lostPermissions.toTypedArray(), REQUEST_PERMISSION)
            for (perm in lostPermissions) {
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_DENIED) {
                    lostPermissions.remove(perm)
                }
            }
            return lostPermissions.isEmpty()
        } else{
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val mBluetoothAdapter = bluetoothManager.adapter

            handler.postDelayed({
                getPairDevise(mBluetoothAdapter)
            }, 2000)
            return true
        }
    }
}