package com.webianks.bluechat

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.*
import android.os.Message
import android.support.constraint.solver.widgets.ConstraintWidgetContainer
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.*
import android.widget.*
import kotlinx.android.synthetic.main.popup.view.*

class OverlayService : Service() {
    inner class LocalBinder : Binder() {
        val service: OverlayService = this@OverlayService
    }

    private val mBinder = LocalBinder()

    override fun onBind(intent: Intent?) = mBinder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY

    }

    private lateinit var inflate: LayoutInflater
    private lateinit var mview: View
    private lateinit var mWindowManager: WindowManager
    private var mViewAdded = false
    private var mParams: WindowManager.LayoutParams? = null
    private val mHandler = Handler()

    override fun onCreate() {
        super.onCreate()

        Log.i("test", "Overlayservice Start")
        val intentFilter = IntentFilter(BluetoothChatService.INTENT_FILTER_OVERLAY).apply {
            addAction(BluetoothChatService.ACTION_OVERLAY_RECEIVE_MESSAGE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter)

        inflate = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mview = inflate.inflate(R.layout.popup, null)
        mview.setOnClickListener {
            mview.tv_content.visibility = View.VISIBLE
            val intent = Intent(this, MainActivity::class.java)
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            hide()
            startActivity(intent)
            mHandler.removeMessages(0)
        }

        mParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT)

    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothChatService.ACTION_OVERLAY_RECEIVE_MESSAGE -> {
                    Log.i("test", "Overlay Receive Message")
                    val message = intent.getStringExtra(BluetoothChatService.ARG_RECEIVE_MESSAGE)
                    mview.tv_content.text = message
                    show()
                }
            }
        }
    }

fun show() {
    mWindowManager.addView(mview, mParams)

    mHandler.postDelayed({
        mWindowManager.removeView(mview)
    }, 5000)
    mViewAdded = true
    Log.e("test", "show overlay")
}

fun hide() {
    mWindowManager.removeView(mview)
    mViewAdded = false
}

}