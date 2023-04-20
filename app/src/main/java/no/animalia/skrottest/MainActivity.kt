package no.animalia.skrottest

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.animalia.skrottest.ui.theme.MyApplicationTheme
import java.util.UUID

/**
 * Skal finne
 * -> Service 0003cdd0-0000-1000-8000-00805f9b0131
 * --> Characteristic 0003cdd1-0000-1000-8000-00805f9b0131
 */
class MainActivity : ComponentActivity() {
    private val gattServiceUUID = UUID.fromString("0003cdd0-0000-1000-8000-00805f9b0131")
    private val gattCharacteristicUUID = UUID.fromString("0003cdd1-0000-1000-8000-00805f9b0131")

    private var scanning = false
    private val handler = Handler()

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 15000

    private val scanFilter = listOf<ScanFilter>(
        //ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("0003cdd0-0000-1000-8000-00805f9b0131")).build(),
        //ScanFilter.Builder().setDeviceAddress("C6:29:75:DB:24:C2").build(),
        ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString("0003cdd0-0000-1000-8000-00805f9b0131"))).build(),
    )

    var foundDevice: BluetoothDevice? = null
    var bluetoothGatt: BluetoothGatt? = null

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.w(javaClass.name, "SCAN CALLBACK ${result.device.address}, ${result.device.name}")

            if (foundDevice == null) {
                foundDevice = result.device
                connect(foundDevice!!)
            }
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                Log.w(javaClass.name, "CONNECTED")
                gatt!!.discoverServices()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(javaClass.name, "DISCONNECTED")

            }
        }
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.w(javaClass.name, "GATT success ")
                    val services:List<BluetoothGattService>? = gatt!!.services
                    services!!.forEach { service:BluetoothGattService ->
                        Log.w(javaClass.name, "GATT Service ${service.uuid}")
                        if (service.uuid == gattServiceUUID) {
                            Log.w(javaClass.name, "Fant  -> Service ${service.uuid}")
                            service.characteristics.forEach {gattCharacteristic:BluetoothGattCharacteristic ->
                                Log.w(javaClass.name, "Fant  --> Characteristic ${gattCharacteristic.uuid}")
                                if (gattCharacteristic.uuid == gattCharacteristicUUID) {
                                    Log.w(javaClass.name, "Venter --> ${gattCharacteristic.uuid}")
                                    gatt.setCharacteristicNotification(gattCharacteristic, true)

                                    // Vet ikke hvorfor dette er nødvendig :(
                                    val descriptor = gattCharacteristic.descriptors.first()
                                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                    gatt.writeDescriptor(descriptor)
                                }
                            }
                        } else {
                            Log.w(javaClass.name, "Ignorerer Service ${service.uuid}")

                        }
                    }

                }
                else -> {
                    Log.w(javaClass.name, "GATT service (else) ${status}")
                }
            }
        }

        fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            Log.e(javaClass.name, "onCharacteristicChanged")
            Log.e(javaClass.name, "bytes: ${value.size}")
            Log.e(javaClass.name, "UTF-8: ${String(value, Charsets.UTF_8)}")
            val decimals = value.joinToString {"${it.dec()}"}
            Log.e(javaClass.name, "decimals $decimals")
            Log.e(javaClass.name, "hex ${value.toHexString()}")

            /**
             * Skrottkrok RFID
             * bytes: 18
             * UTF-8: :ID64=041A4CA761
             * decimals 57, 72, 67, 53, 51, 60, 47, 51, 48, 64, 51, 66, 64, 54, 53, 48, 12, 9
             * hex 3a464458422d533d3537383237333837333732303231370d0a
             *
             * Øremerke:
             * bytes: 25
             * UTF-8: :FDXB-S=578273873720217
             * decimals 57, 69, 67, 87, 65, 44, 82, 60, 52, 54, 55, 49, 54, 50, 55, 54, 50, 54, 49, 47, 49, 48, 54, 12, 9
             * hex 3a494436343d303431413443413736310d0a
             */

        }

    }


    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        Log.e(javaClass.name, "CONNECTING")
        bluetoothGatt = device.connectGatt(this@MainActivity, false, bluetoothGattCallback)
    }

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [
        "android.permission.BLUETOOTH",
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.BLUETOOTH_CONNECT"])
    private val startScan = {
        Log.w(javaClass.name, "START SCAN")

        if (!scanning) { // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner!!.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            bluetoothLeScanner!!.startScan(scanFilter,
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setNumOfMatches(1)
                    .build(),
                leScanCallback
            )

            //bluetoothLeScanner!!.startScan(leScanCallback)
        } else {
            scanning = false
            bluetoothLeScanner!!.stopScan(leScanCallback)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(javaClass.name, "onCreate")

        ActivityCompat.requestPermissions(
            this@MainActivity as Activity,
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION), 1
        )
        bluetoothManager = this@MainActivity.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager!!.getAdapter() as BluetoothAdapter
        bluetoothLeScanner = bluetoothAdapter!!.getBluetoothLeScanner() as BluetoothLeScanner

        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row {
                        Button(onClick = startScan) {
                            Text("Koble til og scan...")
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}