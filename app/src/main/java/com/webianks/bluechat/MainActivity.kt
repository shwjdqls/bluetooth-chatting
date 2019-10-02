package com.webianks.bluechat

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), DevicesRecyclerViewAdapter.ItemClickListener,
        ChatFragment.CommunicationListener {

    companion object {
        private const val REQUEST_ENABLE_BT = 123
        private const val PERMISSION_REQUEST_LOCATION = 123
        private const val PERMISSION_REQUEST_OVERLAY = 134
        private const val ACITION_RESEND_STATE = "status"
        private const val INTENT_FILTER_BLUETOOTH = "bluetooth"
        private const val ARG_STATUS = "status"
    }

    private val mDeviceList = arrayListOf<DeviceData>()
    private var mBtAdapter: BluetoothAdapter? = null

    private lateinit var devicesAdapter: DevicesRecyclerViewAdapter
    private lateinit var mConnectedDeviceName: String
    private lateinit var chatFragment: ChatFragment

    private var mChatService: BluetoothChatService? = null
    private var mOverlayService: OverlayService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBtAdapter == null) {
            AlertDialog.Builder(this)
                    .setTitle(getString(R.string.not_compatible))
                    .setMessage(getString(R.string.no_support))
                    .setPositiveButton("Exit") { _, _ -> exitProcess(0) }
                    .show()
        }

        toolbarTitle.typeface = Typeface.createFromAsset(assets, "fonts/product_sans.ttf")

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerViewPaired.layoutManager = LinearLayoutManager(this)

        recyclerView.isNestedScrollingEnabled = false
        recyclerViewPaired.isNestedScrollingEnabled = false

        devicesAdapter = DevicesRecyclerViewAdapter(context = this, mDeviceList = mDeviceList)
        recyclerView.adapter = devicesAdapter
        devicesAdapter.setItemClickListener(this)

        if (mBtAdapter?.isEnabled == false) {
            status.text = getString(R.string.bluetooth_not_enabled)
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT)
        } else {
            status.text = getString(R.string.not_connected)
        }

        mBtAdapter?.bondedDevices?.let {
            val mPairedDeviceList = arrayListOf<DeviceData>()

            // If there are paired devices, add each one to the ArrayAdapter
            if (it.size > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (device in it) {
                    mPairedDeviceList.add(DeviceData(device.name, device.address))
                }

                DevicesRecyclerViewAdapter(context = this, mDeviceList = mPairedDeviceList).apply {
                    recyclerViewPaired.adapter = this
                    setItemClickListener(this@MainActivity)
                }

                headerLabelPaired.visibility = View.VISIBLE
            }
        }

        searchDevices()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            var intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, PERMISSION_REQUEST_OVERLAY)
        }

        search_devices.setOnClickListener {
            searchDevices()
        }

        make_visible.setOnClickListener {
            makeVisible()
        }
    }

    override fun onStart() {
        super.onStart()

        bindService(Intent(this, BluetoothChatService::class.java), bluetoothServiceConnection, Context.BIND_AUTO_CREATE)
        bindService(Intent(this, OverlayService::class.java), overlayServiceConnection, Context.BIND_AUTO_CREATE)

        registerReceiver(mReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))

        LocalBroadcastManager.getInstance(this).registerReceiver(mStateReceiver, IntentFilter(BluetoothChatService.INTENT_FILTER_MAIN))
    }

    override fun onResume() {
        super.onResume()
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        mChatService?.let {
            if (it.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                it.start()
            }
        }

        if (isFinishing) {
            return
        }
        supportFragmentManager.beginTransaction().let {
            chatFragment = ChatFragment.newInstance().apply {
                setCommunicationListener(this@MainActivity)
                it.replace(R.id.mainScreen, this, "ChatFragment")
                it.addToBackStack("ChatFragment")
                it.commit()
            }
        }
    }

    private val mStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                BluetoothChatService.ACTION_RECEIVE_MESSAGE -> {
                    val message = intent.getStringExtra(BluetoothChatService.ARG_MESSAGE)
                    val milliSecondsTime = System.currentTimeMillis()
                    chatFragment.communicate(Message(message, milliSecondsTime, Constants.MESSAGE_TYPE_RECEIVED))
                }
                BluetoothChatService.ACTION_UPDATE_STATUS -> {
                    when (intent.getIntExtra(BluetoothChatService.ARG_STATUS, -1)) {
                        BluetoothChatService.STATE_NONE -> {
                            status.text = getString(R.string.not_connected)
                            connectionDot.setImageDrawable(getDrawable(R.drawable.ic_circle_red))
                        }
                        BluetoothChatService.STATE_LISTEN -> {
                            status.text = getString(R.string.connecting)
                            connectionDot.setImageDrawable(getDrawable(R.drawable.ic_circle_connecting))
                        }
                        BluetoothChatService.STATE_CONNECTING -> {
                            status.text = getString(R.string.connecting)
                            connectionDot.setImageDrawable(getDrawable(R.drawable.ic_circle_connecting))
                        }
                        BluetoothChatService.STATE_CONNECTED -> {
                            status.text = "${getString(R.string.connected_to)} $mConnectedDeviceName"
                            connectionDot.setImageDrawable(getDrawable(R.drawable.ic_circle_connected))
                            Snackbar.make(findViewById(R.id.mainScreen), "Connected to " + mConnectedDeviceName, Snackbar.LENGTH_SHORT).show()
                        }
                        else -> {
                            // 예외 처리
                        }
                    }
                }
            }
        }
    }

    private fun reSendStatus() {
        Intent(INTENT_FILTER_BLUETOOTH).apply {
            action = ACITION_RESEND_STATE
            putExtra(ARG_STATUS,)

                    }
    }

    private fun makeVisible() {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        startActivity(discoverableIntent)

    }

    private fun searchDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder(this)
                        .setTitle(getString(R.string.need_loc_access))
                        .setMessage(getString(R.string.please_grant_loc_access))
                        .setPositiveButton(android.R.string.ok, null)
                        .setOnDismissListener {
                            // the dialog will be opened so we have to save that
                            requestPermissions(arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                            ), PERMISSION_REQUEST_LOCATION)
                        }.show()
            } else {
                startDiscovery()
            }
        } else {
            startDiscovery()
        }
    }

    private fun startDiscovery() {
        progressBar.visibility = View.VISIBLE
        headerLabel.text = getString(R.string.searching)
        mDeviceList.clear()

        // If we're already discovering, stop it
        if (mBtAdapter?.isDiscovering == true)
            mBtAdapter?.cancelDiscovery()

        // Request discover from BluetoothAdapter
        mBtAdapter?.startDiscovery()
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                mDeviceList.add(DeviceData(device.name, device.address))

                val setList = HashSet<DeviceData>(mDeviceList)
                mDeviceList.clear()
                mDeviceList.addAll(setList)

                devicesAdapter.notifyDataSetChanged()
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                progressBar.visibility = View.INVISIBLE
                headerLabel.text = getString(R.string.found)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        progressBar.visibility = View.INVISIBLE

        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            //Bluetooth is now connected.
            status.text = getString(R.string.not_connected)

            // Get a set of currently paired devices
            val pairedDevices = mBtAdapter?.bondedDevices
            val mPairedDeviceList = arrayListOf<DeviceData>()

            mPairedDeviceList.clear()

            // If there are paired devices, add each one to the ArrayAdapter
            if (pairedDevices?.size ?: 0 > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (device in pairedDevices!!) {
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                    mPairedDeviceList.add(DeviceData(deviceName, deviceHardwareAddress))
                }

                val devicesAdapter = DevicesRecyclerViewAdapter(context = this, mDeviceList = mPairedDeviceList)
                recyclerViewPaired.adapter = devicesAdapter
                devicesAdapter.setItemClickListener(this)
                headerLabelPaired.visibility = View.VISIBLE

            }
            if (requestCode == PERMISSION_REQUEST_OVERLAY) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    //
                } else {
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {

            PERMISSION_REQUEST_LOCATION -> {
                // the request returned a result so the dialog is closed
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    //Log.d(TAG, "Coarse and fine location permissions granted")
                    startDiscovery()
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle(getString(R.string.fun_limted))
                        builder.setMessage(getString(R.string.since_perm_not_granted))
                        builder.setPositiveButton(android.R.string.ok, null)
                        builder.show()
                    }
                }
            }
        }
    }

    override fun itemClicked(deviceData: DeviceData) {
        connectDevice(deviceData)
    }

    private fun connectDevice(deviceData: DeviceData) {

        // Cancel discovery because it's costly and we're about to connect
        mBtAdapter?.cancelDiscovery()
        val deviceAddress = deviceData.deviceHardwareAddress

        val device = mBtAdapter?.getRemoteDevice(deviceAddress)

        status.text = getString(R.string.connecting)
        connectionDot.setImageDrawable(getDrawable(R.drawable.ic_circle_connecting))

        // Attempt to connect to the device
        mChatService?.connect(device, true)

    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStateReceiver)
    }

    private fun sendMessage(message: String) {
        // Check that we're actually connected before trying anything
        if (mChatService?.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return
        }

        // Check that there's actually something to send
        if (message.isNotEmpty()) {
            // Get the message bytes and tell the BluetoothChatService to write
            val send = message.toByteArray()
            mChatService?.write(send)

            // Reset out string buffer to zero and clear the edit text field
            //mOutStringBuffer.setLength(0)
            //mOutEditText.setText(mOutStringBuffer)
        }
    }

    override fun onCommunication(message: String) {
        sendMessage(message)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0)
            super.onBackPressed()
        else
            supportFragmentManager.popBackStack()
    }

    private val bluetoothServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {}

        override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothChatService.LocalBinder
            mChatService = binder.service
        }
    }

    private val overlayServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {}

        override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
            val binder = service as OverlayService.LocalBinder
            mOverlayService = binder.service
        }
    }
}
