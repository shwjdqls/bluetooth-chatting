package com.webianks.bluechat

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.*
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

    private lateinit var inflate: LayoutInflater
    private lateinit var mview: View
    private lateinit var mWindowManager: WindowManager
    private var mViewAdded = false

    override fun onCreate() {
        super.onCreate()

        val intentFilter = IntentFilter(BluetoothChatService.INTENT_FILTER_OVERLAY).apply {
            addAction(BluetoothChatService.ACTION_RECEIVE_MESSAGE)
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter)
        val listener: View.OnClickListener = View.OnClickListener { }
        inflate = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mview = inflate.inflate(R.layout.popup, null, false)
        mview.setOnClickListener {

            mview.tv_content.visibility = View.INVISIBLE

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        // inflate = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val mParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT)

        //wm.addView(mview, mParams)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothChatService.ACTION_OVERLAY_RECEIVE_MESSAGE -> {
                    Log.i("test","Overlay Receive Message")
                    val message = intent.getStringExtra(BluetoothChatService.ARG_MESSAGE)
                    mview.tv_content.text = message
                }
            }
        }
    }

    fun show() {
        Handler().postDelayed({
            createView(null)
        }, 5000)
        mWindowManager.removeView(mview)
        mViewAdded = true
        Log.e("test", "show overlay")
    }

    fun hide() {
        mWindowManager.removeView(mview)
        mViewAdded = false
    }

    private fun createView(container: ViewGroup?): View? {

        val mParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT)

        val layoutinflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val mView: View = layoutinflater.inflate(R.layout.popup, container, false)
        mWindowManager.addView(mview, mParams)
        initViews(mView)
        return mView
    }

    private fun initViews(mView: View) {
//        ContentTV = mView.findViewById(R.id.tv_content)
    }
}