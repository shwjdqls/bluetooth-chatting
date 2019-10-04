package com.webianks.bluechat

import android.app.ActivityManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.util.Log
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.content.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import android.os.Bundle
import android.os.Handler
import java.util.*
import android.os.Binder
import android.support.v4.content.LocalBroadcastManager
import android.system.Os.read
import android.view.animation.AccelerateInterpolator
import android.content.ComponentName
import android.app.ActivityManager.RunningTaskInfo
import android.content.Context.ACTIVITY_SERVICE


class BluetoothChatService : Service() {

    inner class LocalBinder : Binder() {
        val service: BluetoothChatService = this@BluetoothChatService
    }

    private val mBinder = LocalBinder()

    override fun onBind(intent: Intent?) = mBinder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY

    }

    // Member fields
    private var mAdapter: BluetoothAdapter? = null
    private var mHandler: Handler? = null
    private var mSecureAcceptThread: AcceptThread? = null
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null

    private var mState: Int = 0
    private var mNewState: Int = 0

    private val TAG: String = javaClass.simpleName

    // Unique UUID for this application
    private val MY_UUID_SECURE = UUID.fromString("29621b37-e817-485a-a258-52da5261421a")
    private val MY_UUID_INSECURE = UUID.fromString("d620cd2b-e0a4-435b-b02e-40324d57195b")


    // Name for the SDP record when creating server socket
    private val NAME_SECURE = "BluetoothChatSecure"
    private val NAME_INSECURE = "BluetoothChatInsecure"

    private val localBroadcastManager = LocalBroadcastManager.getInstance(this)

    private var test: Boolean = true

    fun isActivityRunning(activity: String): Boolean {
        val activityManager: ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val infos: List<ActivityManager.RunningTaskInfo> = activityManager.getRunningTasks(1)
        val runningTask: ActivityManager.RunningTaskInfo = infos[0]
        val componentName: ComponentName = runningTask.topActivity


        return activity.equals(componentName.className)/*
        for (i in 1..infos.size step 1) {
            return activity.equals(componentName.className)
        }
        return false*/
    }

    fun sendState() {
        Log.i("test", "sendstate_blueservice + " + mState.toString())

        val intent = Intent(INTENT_FILTER_MAIN)
        intent.action = ACTION_UPDATE_STATUS
        intent.putExtra(ARG_STATUS, mState)
        localBroadcastManager.sendBroadcast(intent)

        /*Intent(INTENT_FILTER_MAIN).apply {
            action = ACTION_UPDATE_STATUS
            putExtra(ARG_STATUS, mState)
            Log.i("test", mState.toString())
            localBroadcastManager.sendBroadcast(this)
        }*/
    }

    init {
        mAdapter = BluetoothAdapter.getDefaultAdapter()
        mState = STATE_NONE
        sendState()
        mNewState = mState
    }

    /*fun setHandler(handler: Handler) {
        mHandler = handler
    }
*/

    @Synchronized
    fun getState(): Int {
        return mState
    }

    override fun onCreate() {
        super.onCreate()
        mState = STATE_NONE
        sendState()
        Log.i("test", "sendStateStart")
    }

    @Synchronized
    fun start() {
        Log.d(TAG, "start")

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = AcceptThread(true)
            mSecureAcceptThread?.start()
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = AcceptThread(false)
            mInsecureAcceptThread?.start()
        }
        // Update UI title
        //updateUserInterfaceTitle()
    }


    /**
     * Start the ConnectThread to initiate a connection to a remote device.

     * @param device The BluetoothDevice to connect
     * *
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    @Synchronized
    fun connect(device: BluetoothDevice?, secure: Boolean) {

        Log.d(TAG, "connect to: " + device)

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread?.cancel()
                mConnectThread = null
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(device, secure)
        mConnectThread?.start()

        // Update UI title
        //updateUserInterfaceTitle()
    }


    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection

     * @param socket The BluetoothSocket on which the connection was made
     * *
     * @param device The BluetoothDevice that has been connected
     */
    @Synchronized
    fun connected(socket: BluetoothSocket?, device: BluetoothDevice?, socketType: String) {
        Log.d(TAG, "connected, Socket Type:" + socketType)

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread?.cancel()
            mSecureAcceptThread = null
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread?.cancel()
            mInsecureAcceptThread = null
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket, socketType)
        mConnectedThread?.start()

        // Send the name of the connected device back to the UI Activity
        val msg = Constants.MESSAGE_DEVICE_NAME
        //Check to fix
        // Update UI title
        //updateUserInterfaceTitle()
    }

    /**
     * Stop all threads
     */
    @Synchronized
    fun stop() {
        Log.d(TAG, "stop")

        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread?.cancel()
            mSecureAcceptThread = null
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread?.cancel()
            mInsecureAcceptThread = null
        }

        mState = STATE_NONE
        sendState()
        // Update UI title
        //updateUserInterfaceTitle()
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner

     * @param out The bytes to write
     * *
     * @see ConnectedThread.write
     */
    fun write(out: ByteArray) {
        // Create temporary object
        var r: ConnectedThread? = null
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }
        // Perform the write unsynchronized
        r?.write(out)
    }


    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        // Send a failure message back to the Activity
        val msg = mHandler?.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Unable to connect device")
        msg?.data = bundle
        mHandler?.sendMessage(msg)

        mState = STATE_NONE
        sendState()

        // Update UI title
        //updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        this@BluetoothChatService.start()
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        // Send a failure message back to the Activity
        val msg = mHandler?.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Device connection was lost")
        msg?.data = bundle
        mHandler?.sendMessage(msg)

        mState = STATE_NONE
        sendState()

        // Update UI title
        // updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        this@BluetoothChatService.start()
    }


    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private inner class AcceptThread(secure: Boolean) : Thread() {
        // The local server socket
        private val mmServerSocket: BluetoothServerSocket?
        private val mSocketType: String

        init {
            var tmp: BluetoothServerSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter?.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE)
                } else {
                    tmp = mAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e)
            }

            mmServerSocket = tmp
            mState = STATE_LISTEN
            sendState()
        }

        override fun run() {

            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this)
            name = "AcceptThread" + mSocketType

            var socket: BluetoothSocket?

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e)
                    break
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized(this@BluetoothChatService) {
                        when (mState) {
                            STATE_LISTEN, STATE_CONNECTING ->
                                // Situation normal. Start the connected thread.
                                connected(socket, socket?.remoteDevice,
                                        mSocketType)
                            STATE_NONE, STATE_CONNECTED ->
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket?.close()
                                } catch (e: IOException) {
                                    Log.e(TAG, "Could not close unwanted socket", e)
                                }

                            else -> {
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType)

        }

        fun cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this)
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e)
            }

        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private inner class ConnectThread(private val mmDevice: BluetoothDevice?, secure: Boolean) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mSocketType: String

        init {
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = mmDevice?.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE)
                } else {
                    tmp = mmDevice?.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e)
            }

            mmSocket = tmp
            mState = STATE_CONNECTING
            sendState()
        }

        override fun run() {

            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType)
            name = "ConnectThread" + mSocketType

            // Always cancel discovery because it will slow down a connection
            mAdapter?.cancelDiscovery()

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket?.connect()

            } catch (e: IOException) {
                // Close the socket
                try {
                    mmSocket?.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2)
                }

                connectionFailed()
                return
            }

            // Reset the ConnectThread because we're done
            synchronized(this@BluetoothChatService) {
                mConnectThread = null
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType)
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect $mSocketType socket failed", e)
            }

        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket?, socketType: String) : Thread() {

        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            Log.d(TAG, "create ConnectedThread: " + socketType)
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mmSocket?.inputStream
                tmpOut = mmSocket?.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
            mState = STATE_CONNECTED
            sendState()
            localBroadcastManager.sendBroadcast(Intent(INTENT_FILTER_MAIN))
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(1024)

            // Keep listening to the InputStream while connected
            if (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    read(buffer.toString(Charsets.UTF_8))

                    // Send the obtained bytes to the UI Activity
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                }

            }
        }

        fun read(data: String) {
            if (isActivityRunning(ChatFragment::class.java.name)) {
                Intent(INTENT_FILTER_OVERLAY).apply {
                    action = ACTION_OVERLAY_RECEIVE_MESSAGE
                    putExtra(ARG_MESSAGE, data)
                    localBroadcastManager.sendBroadcast(this)
                    Log.i("test", "sendMessageOverlay" + data)
                }
            } else {
                Intent(INTENT_FILTER_MAIN).apply {
                    action = ACTION_RECEIVE_MESSAGE
                    putExtra(ARG_MESSAGE, data)
                    localBroadcastManager.sendBroadcast(this)
                    Log.i("test", "sendMessageMain" + data)
                }
            }
        }

        /**
         * Write to the connected OutStream.

         * @param buffer The bytes to write
         */
        fun write(buffer: ByteArray) {
            try {
                mmOutStream?.write(buffer)
                Intent(INTENT_FILTER_MAIN).apply {
                    action = ACTION_SEND_MESSAGE
                    putExtra(ARG_MESSAGE, buffer.toString(Charsets.UTF_8))
                    localBroadcastManager.sendBroadcast(this)
                    Log.i("test", "WriteMessage " + buffer.toString(Charsets.UTF_8))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }

        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }

        }
    }

    companion object {
        const val STATE_NONE = 0       // we're doing nothing
        const val STATE_LISTEN = 1     // now listening for incoming connections
        const val STATE_CONNECTING = 2 // now initiating an outgoing connection
        const val STATE_CONNECTED = 3

        const val INTENT_FILTER_MAIN = "main"
        const val INTENT_FILTER_OVERLAY = "overlay"

        const val ACTION_SEND_MESSAGE = "message"
        const val ACTION_RECEIVE_MESSAGE = "message"
        const val ACTION_OVERLAY_RECEIVE_MESSAGE = "message"
        const val ACTION_UPDATE_STATUS = "status"

        const val ARG_MESSAGE = "message"
        const val ARG_STATUS = "status"
    }
}

