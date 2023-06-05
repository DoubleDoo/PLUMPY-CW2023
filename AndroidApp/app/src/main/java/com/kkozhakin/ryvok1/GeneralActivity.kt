package com.kkozhakin.ryvok1

import android.app.DatePickerDialog
import android.content.*
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.kkozhakin.ryvok1.databinding.ActivityGeneralBinding
import java.io.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.timer


class GeneralActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityGeneralBinding

    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mConnected = false
    private var mBluetoothLeService: BluetoothLeService? = null
    private var textViewState: TextView? = null
    private var exportingLoad: ProgressBar? = null
    private var exportFuture: CompletableFuture<Void>? = null
    private var batteryUpdateFlag: Boolean = false

    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService!!.connect(mDeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
        selected_date = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
        updateCharts()
        invalidateBDate()
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED == action) {
                mConnected = true
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) {
                mConnected = false
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                notifyChars()
                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBluetoothLeService!!.supportedGattServices)
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE == action) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
            }
        }
    }

    private fun displayData(data: String?) {
        if (data != null) {
            textViewState!!.text = data
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)
        
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)

        binding = ActivityGeneralBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarGeneral.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_general)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_settings, R.id.nav_about_device
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        if (intent.getBooleanExtra(DEVICE_PAIRED, false)) {
            navController.navigate(R.id.nav_home)
        } else{
            navController.navigate(R.id.nav_settings)
        }
        if (!batteryUpdateFlag) {
            timer("BatteryUpdate", true, 0, 30000) { updateBattery() }
            batteryUpdateFlag = true
        }
        loadPersonalData()
        val loadActivityMLThread = Thread {
            for (line in load_data("activity_ml_data")){
                val d = line?.split(",")
                activityResult[d!![1]] = d[0].toInt()
            }
        }
        val loadSleepMLThread = Thread {
            for (line in load_data("sleep_ml_data")){
                val d = line?.split(",")
                sleepResult[d!![1]] = d[0].toInt()
            }
        }
        loadActivityMLThread.start()
        loadSleepMLThread.start()

        exportingLoad = findViewById(R.id.export_loading)
        generalActivity = this
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.general, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_general)
        // Find the view pager that will allow the user to swipe between fragments
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mBluetoothLeService != null) {
            val result = mBluetoothLeService!!.connect(mDeviceAddress)
            Log.d(TAG, "Connect request result=$result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBluetoothLeService = null
    }

    companion object {
        private val TAG = GeneralActivity::class.java.simpleName
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
        const val DEVICE_PAIRED = "DEVICE_IS_PAIRED"
        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun notifyChars() {
        var uuid: String?
        val gattServices = mBluetoothLeService!!.supportedGattServices ?: return

        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val gattCharacteristics = gattService.characteristics

            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                uuid = gattCharacteristic.uuid.toString()
                if (uuid == BluetoothLeService.DATA_UUID_STRING) {
                    mBluetoothLeService!!.setCharacteristicNotification(
                        gattCharacteristic, true
                    )
                }
            }
        }
    }

    fun exportOnClick(menuItem: MenuItem) {
        if (exportFuture == null) {
            exportingLoad!!.visibility = VISIBLE
            exportFuture = CompletableFuture.runAsync {
                Log.e("e", "thread start")
                val src = load_data("data")
                var writer: BufferedWriter? = null
                try {
                    val downloadDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (downloadDir.canWrite()) {
                        val dst = FileOutputStream(File(downloadDir, "data_export.csv"))
                        writer = BufferedWriter(OutputStreamWriter(dst))
                        writer.write("pocket_id,v1,v2,v3,v4,v5,v6,v7,time")
                        writer.newLine()
                        for (line in src) {
                            writer.write(line.toString())
                            writer.newLine()
                        }
                        writer.close()
                        dst.close()
                    } else {
                        Toast.makeText(this, "WRITE permission required!", Toast.LENGTH_SHORT)
                            .show()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    writer?.close()
                }
            }

            exportFuture!!.thenApply {
                runOnUiThread {
                    Log.e("e", "thread stop")
                    exportingLoad!!.visibility = INVISIBLE
                    exportFuture = null
                }
            }
        }

    }

    fun clearOnClick(menuItem: MenuItem) {
        clear_data("data")
        clear_data("activity_ml_data")
        clear_data("sleep_ml_data")
    }

    private fun load_data(filename: String): ArrayList<String?> {
        val input: FileInputStream?
        var reader: BufferedReader? = null
        val content: ArrayList<String?> = arrayListOf()
        try {
            // Устанавливаем имя открываемого файла хранилища
            input = openFileInput(filename)
            //FileInputStream -> InputStreamReader ->BufferedReader
            reader = BufferedReader(InputStreamReader(input))
            var line: String?
            // Считываем каждую строку данных и добавляем ее к объекту StringBuilder до конца
            while (reader.readLine().also { line = it } != null) {
                content.add(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            reader?.close()
        }
        return content
    }

    private fun clear_data(filename: String){
        val out: FileOutputStream?
        var writer: BufferedWriter? = null
        try {
            out = openFileOutput(filename, MODE_PRIVATE)
            writer = BufferedWriter(OutputStreamWriter(out))
            writer.write("")
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            writer?.close()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun resetOnClick(view: View) {
        mBluetoothLeService?.unbondDevice()
        mBluetoothLeService?.disconnect()
        clear_data("data")
        clear_data("activity_ml_data")
        clear_data("sleep_ml_data")

        val preferences: SharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.clear()
        editor.apply()

        mBluetoothLeService!!.writeCharacteristic(mBluetoothLeService!!.getCharacteristicByUuid(BluetoothLeService.RESET_UUID), byteArrayOf(1))

        val intent = Intent(this, DeviceConnectionActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun savePersonalDataOnClick(view: View){
        val preferences: SharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE)

        val editor: SharedPreferences.Editor = preferences.edit()

        editor.putString("name", findViewById<EditText>(R.id.userName).text.toString())
        editor.putString("surname", findViewById<EditText>(R.id.userSurname).text.toString())
        editor.putString("age", findViewById<EditText>(R.id.userAge).text.toString())
        editor.putString("height", findViewById<EditText>(R.id.userHeight).text.toString())
        editor.putString("weight", findViewById<EditText>(R.id.userWeight).text.toString())

        editor.apply()

        val navController = findNavController(R.id.nav_host_fragment_content_general)
        navController.navigate(R.id.nav_home)
    }

    private fun loadPersonalData(){
        val preferences: SharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE)

        personalData = mapOf(
            "userName" to preferences.getString("name", ""),
            "userSurname" to preferences.getString("surname", ""),
            "userAge" to preferences.getString("age", ""),
            "userHeight" to preferences.getString("height", ""),
            "userWeight" to preferences.getString("weight", "")
        )
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun updateBattery() {
        mBluetoothLeService?.readCharacteristic(mBluetoothLeService!!.getCharacteristicByUuid(BluetoothLeService.BATTERY_UUID))
    }

    // Get the selected radio button text using radio button on click listener
    @RequiresApi(Build.VERSION_CODES.O)
    fun radioButtonClick(view: View){
        val rb = view as RadioButton
        when (rb.id) {
            R.id.rbDay -> {
                data_type = "day"
            }
            R.id.rbMonth -> {
                data_type = "month"
            }
            R.id.rbYear -> {
                data_type = "year"
            }
            else -> {}
        }
        updateCharts()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun datePickOnClick(view: View){
        val c = Calendar.getInstance()

        DatePickerDialog(this, dateSetListener,
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun invalidateBDate(){
        val formatter: DateTimeFormatter = when(data_type) {
            "day" -> {
                DateTimeFormatter.ofPattern("dd LLLL yyyy")
            }
            "month" -> {
                DateTimeFormatter.ofPattern("LLLL yyyy")
            }
            "year" -> {
                DateTimeFormatter.ofPattern("yyyy")
            }
            else -> {
                DateTimeFormatter.ofPattern("dd LLLL yyyy")
            }
        }
        findViewById<Button>(R.id.bDateActivity).text = selected_date.format(formatter)
        findViewById<Button>(R.id.bDateSleep).text = selected_date.format(formatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateCharts() {
        invalidateBDate()
        update_activity_bar_chart()
        update_sleep_bar_chart()
    }

    fun updateTexts(runTimeText: String?, walkTimeText: String?, fastTimeText: String?, slowTimeText: String?, sleepTimeText: String?){
        runOnUiThread {
            try {
                if (!runTimeText.isNullOrBlank()) {
                    findViewById<TextView>(R.id.run_time).text = runTimeText
                }
                if (!walkTimeText.isNullOrBlank()) {
                    findViewById<TextView>(R.id.walk_time).text = walkTimeText
                }
                if (!fastTimeText.isNullOrBlank()) {
                    findViewById<TextView>(R.id.fast_time).text = fastTimeText
                }
                if (!slowTimeText.isNullOrBlank()) {
                    findViewById<TextView>(R.id.slow_time).text = slowTimeText
                }
                if (!sleepTimeText.isNullOrBlank()) {
                    findViewById<TextView>(R.id.time).text = sleepTimeText
                }
            }
           catch (_: Exception){}
        }
    }
}