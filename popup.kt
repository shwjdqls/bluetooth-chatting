package com.webianks.bluechat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.app.ProgressDialog.show
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.net.Uri
import android.os.*
import android.os.Message
import android.provider.Settings
import android.support.annotation.NonNull
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import kotlinx.android.synthetic.main.chat_fragment.*
import java.util.zip.Inflater

class popup : Service() {
    inner class LocalBinder : Binder() {
        val service: popup = this@popup
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

        val listener: View.OnClickListener = View.OnClickListener { }

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

                val ShowMsg = intent.getStringExtra("msg")
                ContentTV.setText(ShowMsg)
            }
        }, intentFileter)

        // inflate = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val mParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT)

        wm.addView(mview, mParams)
        //val mView: View = LayoutInflater.from(this).inflate(R.layout.popup, container, false)
        //initViews(mView)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

   fun show() {
        var handler: Handler = Handler()
        handler.postDelayed(Runnable()
        {
            onCreateView(inflate, null, null)
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

    fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

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

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {

            PERMISSION_REQUSET_OVERLAY ->

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                }
        }
    }
}