package com.webianks.bluechat

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.*
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.popup.view.*
/** Overlay service for receive message when MainActivity onStop*/
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
    private lateinit var mView: View
    private lateinit var mWindowManager: WindowManager
    private var mParams: WindowManager.LayoutParams? = null
    private val mHandler = Handler()

    override fun onCreate() {
        super.onCreate()

        val intentFilter = IntentFilter(BluetoothChatService.INTENT_FILTER_OVERLAY).apply {
            addAction(BluetoothChatService.ACTION_OVERLAY_RECEIVE_MESSAGE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter)

        inflate = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        mView = inflate.inflate(R.layout.popup, null).apply {
            setOnClickListener {
                it.tv_content.visibility = View.VISIBLE
                val intent = Intent(this@OverlayService, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                hide()
                startActivity(intent)
                mHandler.removeMessages(0)
            }
        }
        mParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT)
    }
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothChatService.ACTION_OVERLAY_RECEIVE_MESSAGE -> {
                    val message = intent.getStringExtra(BluetoothChatService.ARG_RECEIVE_MESSAGE)
                    mView.tv_content.text = message
                    show()
                }
            }
        }
    }
    /** This is showing overlay view when 5sec later overlay will be disappear*/
    fun show() {
        mWindowManager.addView(mView, mParams)
        mHandler.postDelayed({
            mWindowManager.removeView(mView)
        }, 5000)
    }
    /** This is disappear overlay*/
    fun hide() {
        mWindowManager.removeView(mView)
    }
}