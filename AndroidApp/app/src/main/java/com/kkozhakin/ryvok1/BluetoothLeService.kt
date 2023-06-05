package com.kkozhakin.ryvok1

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.io.*
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
class BluetoothLeService : Service() {
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mConnectionState = STATE_DISCONNECTED

    private var data_dq: ArrayDeque<ByteArray> = ArrayDeque(0)

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                mConnectionState = STATE_CONNECTED
                broadcastUpdate(intentAction)
                Log.i(TAG, "Connected to GATT server.")
                // Attempts to discover services after successful connection.
                Log.i(
                    TAG, "Attempting to start service discovery:" +
                            mBluetoothGatt!!.discoverServices()
                )
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                mConnectionState = STATE_DISCONNECTED
                Log.i(TAG, "Disconnected from GATT server.")
                broadcastUpdate(intentAction)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(
                    TAG,
                    "onServicesDiscovered received: $status"
                )
            }
        }

        @Deprecated("Deprecated in Java")
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        @Deprecated("Deprecated in Java")
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun broadcastUpdate(
        action: String,
        characteristic: BluetoothGattCharacteristic
    ) {
        val intent = Intent(action)

        when (characteristic.uuid) {
            DATA_UUID -> {
                data_dq.add(characteristic.value)

                while (!data_dq.isEmpty()) {
                    val data = transform_data(
                        data_dq.removeFirst(),
                        LocalDateTime.now()
                            .minusNanos((data_dq.count() * 80).milliseconds.inWholeNanoseconds)
                    )
                    save_data(data.joinToString(","), "data")
                    data_result.add(data)
                    if (data_result.size == 64 * 39) {
                        val activity_result = resolveData(data_result, "activity")
                        if (!activity_result.isNullOrEmpty()) {
                            for (a in activity_result) {
                                activityResult[a["time"] as String] = a["result"] as Int
                                Log.i("res", a.toString())
                                save_data(a.values.joinToString(","), "activity_ml_data")
                            }
                            update_activity_bar_chart()
                        }
                        val sleep_result = resolveData(data_result, "sleep")
                        if (!sleep_result.isNullOrEmpty()) {
                            for (s in sleep_result) {
                                sleepResult[s["time"] as String] = s["result"] as Int
                                Log.i("res", s.toString())
                                save_data(s.values.joinToString(","), "sleep_ml_data")
                            }
                            update_sleep_bar_chart()
                        }
                        data_result.clear()
                    }
                }
            }
            BATTERY_UUID -> {
                battery_percent = characteristic.value[0].toInt()
            }
        }

        sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close()
        return super.onUnbind(intent)
    }

    private val mBinder: IBinder = LocalBinder()

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }
        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(
     * android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            return if (mBluetoothGatt!!.connect()) {
                mConnectionState = STATE_CONNECTING
                true
            } else {
                false
            }
        }
        val device = mBluetoothAdapter!!.getRemoteDevice(address)

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(
     * android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.disconnect()
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun close() {
        if (mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(
     * android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        try {
            mBluetoothGatt!!.readCharacteristic(characteristic)
        }
        catch (_: Exception) {}
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic?, value: ByteArray) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }

        try {
            mBluetoothGatt!!.writeCharacteristic(characteristic!!, value, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        }
        catch (e: Exception){
            Log.e("Error", e.stackTraceToString())
        }
    }

    fun getCharacteristicByUuid(uuid: UUID): BluetoothGattCharacteristic? {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return null
        }
        val gattServices = supportedGattServices ?: return null

        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val gattCharacteristics = gattService.characteristics

            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                if (uuid == gattCharacteristic.uuid) {
                    return gattCharacteristic
                }
            }
        }
        return null
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)

        if (DATA_UUID == characteristic.uuid) {
//            val descriptor = characteristic.getDescriptor(
//                characteristic.uuid
//            )
            val descriptor = characteristic.descriptors[0]
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

            mBluetoothGatt!!.writeDescriptor(descriptor)
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    fun unbondDevice() {
        for (device in mBluetoothAdapter!!.bondedDevices){
            if (device.address == mBluetoothDeviceAddress){
                val method = device.javaClass.getMethod("removeBond")
                method.invoke(device)
            }
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    val supportedGattServices: List<BluetoothGattService>?
        get() = if (mBluetoothGatt == null) null else mBluetoothGatt!!.services

    companion object {
        private val TAG = BluetoothLeService::class.java.simpleName
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2
        const val ACTION_GATT_CONNECTED = "android-er.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "android-er.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "android-er.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "android-er.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "android-er.EXTRA_DATA"
        const val DATA_UUID_STRING = "0000ff01-8e22-4541-9d4c-21edae82ed19"
        val DATA_UUID = UUID.fromString(DATA_UUID_STRING)!!

        private const val BATTERY_UUID_STRING = "0000fe01-8e22-4541-9d4c-21edae82ed19"
        val BATTERY_UUID = UUID.fromString(BATTERY_UUID_STRING)!!

        private const val RESET_UUID_STRING = "0000fd02-8e22-4541-9d4c-21edae82ed19"
        val RESET_UUID = UUID.fromString(RESET_UUID_STRING)!!
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun transform_data(buffer: ByteArray, time: LocalDateTime): ArrayList<String> {
        var i = 0

        val pocket_id = ((buffer[i++].toInt() and 0xff shl 24) or
                (buffer[i++].toInt() and 0xff shl 16) or (buffer[i++].toInt() and 0xff shl 8) or
                (buffer[i++].toInt() and 0xff)).toString()


        val v1 = ByteBuffer.wrap(byteArrayOf(buffer[i++], buffer[i++])).short.toInt().toString()
        val v2 = ByteBuffer.wrap(byteArrayOf(buffer[i++], buffer[i++])).short.toInt().toString()
        val v3 = ByteBuffer.wrap(byteArrayOf(buffer[i++], buffer[i++])).short.toInt().toString()
        val v4 = ByteBuffer.wrap(byteArrayOf(buffer[i++], buffer[i++])).short.toInt().toString()
        val v5 = ByteBuffer.wrap(byteArrayOf(buffer[i++], buffer[i++])).short.toInt().toString()
        val v6 = ByteBuffer.wrap(byteArrayOf(buffer[i++], buffer[i++])).short.toInt().toString()
        val v7 = ByteBuffer.wrap(byteArrayOf(buffer[i++], buffer[i])).short.toInt().toString()

        return arrayListOf(pocket_id, v1, v2, v3, v4, v5, v6, v7, time.toString())
    }

    private fun save_data(data: String, filename: String) {
        val out: FileOutputStream?
        var writer: BufferedWriter? = null
        try {
            // Устанавливаем имя файла и способ хранения
            out = openFileOutput(filename, Context.MODE_APPEND)
            // Создаем объект OutputStreamWriter и передаем его конструктору BufferedWriter
            writer = BufferedWriter(OutputStreamWriter(out))
            // Записываем данные в файл
            writer.write(data)
            writer.newLine()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            writer?.close()
        }
    }
}