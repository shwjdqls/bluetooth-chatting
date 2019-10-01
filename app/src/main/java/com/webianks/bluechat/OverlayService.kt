package com.webianks.bluechat

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.*
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.*
import android.widget.*

class OverlayService : Service() {
    inner class LocalBinder : Binder() {
        val service: OverlayService = this@OverlayService
    }

    private val mBinder = LocalBinder()

    override fun onBind(intent: Intent?) = mBinder

    lateinit var ContentTV: TextView

    private val inflate: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var wm: WindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private lateinit var mview: View

    private val intentFileter = IntentFilter("com.webianks.bluechat.SEND_BROAD_CAST")
    private var chatAdapter: ChatAdapter? = null
    private val messageList = arrayListOf<com.webianks.bluechat.Message>()
    private val PERMISSION_REQUSET_OVERLAY: Int = 123;
    private var alreadyAskedForPermission = false

    private val WINDOW_OUT: Int = 101
    private val Window_IN: Int = 102
    private val Msgsave: String = ""
    private lateinit var chatFragment: ChatFragment

    private var mWindowManager : WindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var mViewAdded = false

    private lateinit var mPackageManager: PackageManager

    companion object {
        val ACTION_START = "start"
        val ACTION_STOP = "stop"
    }


    override fun onCreate() {
        super.onCreate()

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(BluetoothChatService.INTENT_FILTER_OVERLAY))
        val listener: View.OnClickListener = View.OnClickListener { }

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        mview.setOnClickListener {

        }

        // inflate = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val mParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT)

        wm.addView(mview, mParams)
        //val mView: View = LayoutInflater.from(this).inflate(R.layout.OverlayService, container, false)
        //initViews(mView)
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
            createView(inflate, null, null)
        }, 5000)
        wm.removeView(mview)
        mViewAdded = true
       Log.e("test", "show overlay")
    }

    fun hide()
    {
        mWindowManager.removeView(mview)
        mViewAdded = false
    }

    private fun createView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val mParams  = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT)

        wm.addView(mview, mParams)
        val mView: View = LayoutInflater.from(this).inflate(R.layout.popup, container, false)
        initViews(mView)
        return mView
    }

    private fun initViews(mView: View) {
        ContentTV = mView.findViewById(R.id.tv_content)
    }
}