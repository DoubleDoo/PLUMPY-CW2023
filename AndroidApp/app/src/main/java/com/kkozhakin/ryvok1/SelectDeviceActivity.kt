package com.kkozhakin.ryvok1

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity


open class SelectDeviceActivity : AppCompatActivity() {


    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 5000

    private var mBluetoothAdapter: BluetoothAdapter? = null

    open var mBluetoothLeScanner: BluetoothLeScanner? = null

    private var mScanning = false

    private val RQS_ENABLE_BLUETOOTH = 1

    lateinit var listViewLE: ListView

    open var listBluetoothDevice: ArrayList<BluetoothDevice> = ArrayList()
    open var adapterLeScanResult: ListAdapter? = null
    open var indeterminateBar: ProgressBar? = null

    private var mHandler: Handler? = null

    @RequiresPermission(allOf = [ "android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT"])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_device)

        listViewLE = findViewById<View>(R.id.listOfDevices) as ListView
        indeterminateBar = findViewById<View>(R.id.indeterminateBar) as ProgressBar
        mHandler = Handler()

        // Регистрация широковещательного приемника
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(broadcastReceiver, filter)

        // Check if BLE is supported on the device.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(
                this,
                "BLUETOOTH_LE not supported in this device!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }

        getBluetoothAdapterAndLeScanner()

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(
                this,
                "bluetoothManager.getAdapter()==null",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        adapterLeScanResult = DeviceAdapter(
            this, listBluetoothDevice
        )

        listViewLE.adapter = adapterLeScanResult
        listViewLE.onItemClickListener = scanResultOnItemClickListener

        scanLeDevice(true)
    }

    @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"])
    private var scanResultOnItemClickListener: OnItemClickListener? =
        OnItemClickListener { parent, _, position, _ ->
            val device = parent.getItemAtPosition(position) as BluetoothDevice
            val msg = """
                ${device.address}
                ${device.bluetoothClass}
                ${getBTDevieType(device)}
                """.trimIndent()
            AlertDialog.Builder(this)
                .setTitle(device.name)
                .setMessage(msg)
                .setPositiveButton("OK"
                ) { _, _ -> }
                .setNeutralButton("CONNECT") { _, _ ->
                    device.createBond()
                }
                .show()
        }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresPermission(allOf = ["android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"])
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                when (device?.bondState) {
                    BluetoothDevice.BOND_BONDING -> {
                        // Соединение устанавливается
                    }
                    BluetoothDevice.BOND_BONDED -> {
                        val intent = Intent(
                            this@SelectDeviceActivity,
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
                            false
                        )
                        if (mScanning) {
                            mBluetoothLeScanner!!.stopScan(scanCallback)
                            mScanning = false
                        }
                        startActivity(intent)
                    }
                    BluetoothDevice.BOND_NONE -> {
                        // Соединение не установлено
                    }
                }
            }
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun getBTDevieType(d: BluetoothDevice): String {
        val type: String = when (d.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "DEVICE_TYPE_CLASSIC"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "DEVICE_TYPE_DUAL"
            BluetoothDevice.DEVICE_TYPE_LE -> "DEVICE_TYPE_LE"
            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "DEVICE_TYPE_UNKNOWN"
            else -> "unknown..."
        }
        return type
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    override fun onResume() {
        super.onResume()
        if (mBluetoothAdapter != null && !mBluetoothAdapter!!.isEnabled) {
            if (!mBluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, RQS_ENABLE_BLUETOOTH)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RQS_ENABLE_BLUETOOTH && resultCode == RESULT_CANCELED) {
            finish()
            return
        }
        getBluetoothAdapterAndLeScanner()

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(
                this,
                "bluetoothManager.getAdapter()==null",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getBluetoothAdapterAndLeScanner() {
        // Get BluetoothAdapter and BluetoothLeScanner.
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter != null) {
            mBluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner
            mScanning = false
        }
    }

    /*
    to call startScan (ScanCallback callback),
    Requires BLUETOOTH_ADMIN permission.
    Must hold ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission to get results.
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_SCAN")
    private fun scanLeDevice(enable: Boolean) {
        if (mBluetoothLeScanner != null && enable) {
            indeterminateBar!!.visibility = VISIBLE
            listViewLE.isEnabled = false
            listBluetoothDevice.clear()
            listViewLE.invalidateViews()

            // Stops scanning after a pre-defined scan period.
            mHandler!!.postDelayed({
                mBluetoothLeScanner!!.stopScan(scanCallback)
                listViewLE.invalidateViews()
                indeterminateBar!!.visibility = INVISIBLE
                listViewLE.isEnabled = true
                mScanning = false
            }, SCAN_PERIOD)
//            mBluetoothLeScanner!!.startScan(scanCallback)

            //scan specified devices only with ScanFilter
            val scanFilter = ScanFilter.Builder()
//                .setServiceUuid(BluetoothLeService.ParcelUuid_SERVICE)
                .setDeviceName("RYVOK")
                .build()
            val scanFilters: MutableList<ScanFilter> = ArrayList()
            scanFilters.add(scanFilter)

            val scanSettings = ScanSettings.Builder().build()

            mBluetoothLeScanner!!.startScan(scanFilters, scanSettings, scanCallback)

            mScanning = true
        } else {
            mBluetoothLeScanner!!.stopScan(scanCallback)
            mScanning = false
        }
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            addBluetoothDevice(result.device)
        }

        override fun onBatchScanResults(results: List<ScanResult?>) {
            super.onBatchScanResults(results)
            for (result in results) {
                addBluetoothDevice(result!!.device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(
                this@SelectDeviceActivity,
                "onScanFailed: $errorCode",
                Toast.LENGTH_LONG
            ).show()
        }

        private fun addBluetoothDevice(device: BluetoothDevice) {
            if (!listBluetoothDevice.contains(device)) {
                listBluetoothDevice.add(device)
                listViewLE.invalidateViews()
            }
        }
    }
}