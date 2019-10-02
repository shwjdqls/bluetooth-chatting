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

    lateinit var ContentTV: TextView

    private lateinit var inflate: LayoutInflater
    private lateinit var wm: WindowManager
    private lateinit var mview: View
    private lateinit var mWindowManager: WindowManager
    private var mViewAdded = false

    override fun onCreate() {
        super.onCreate()

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(BluetoothChatService.INTENT_FILTER_OVERLAY))
        val listener: View.OnClickListener = View.OnClickListener { }

        var wm: WindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        inflate = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (p1?.action) {
                BluetoothChatService.ACTION_UPDATE_STATUS -> {
                    ContentTV.text = p1.getStringExtra("msg")
                    show()
                }
            }
        }
    }

    fun show() {
        Handler().postDelayed({
            createView(null)
        }, 5000)
        wm.removeView(mview)
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
        wm.addView(mview, mParams)
        initViews(mView)
        return mView
    }

    private fun initViews(mView: View) {
        ContentTV = mView.findViewById(R.id.tv_content)
    }
}