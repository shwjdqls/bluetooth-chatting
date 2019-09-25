package com.webianks.bluechat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import android.os.*
import android.os.Message
import android.support.design.widget.Snackbar
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

class popup : Service() {
    inner class LocalBinder : Binder() {
        val service: popup = this@popup
    }

    private val mBinder = LocalBinder()

    override fun onBind(intent: Intent?) = mBinder

    private lateinit var OKbt: Button
    private lateinit var Cancelbt : Button
    private lateinit var chatInput : EditText
    private lateinit var sendButton : Button
    private lateinit var recyclerviewChat: RecyclerView
    private var chatAdapter: ChatAdapter? = null
    private val messageList = arrayListOf<com.webianks.bluechat.Message>()
    var wm :WindowManager? = null
    var mview : View? = null

    private var handler : Handler? = null

    var LastTime : Long?  = null
    var CurrentTime : Long? = null
    val  second = 50 * 1000

    override fun onCreate()
    {
        super.onCreate()
        var inflate : LayoutInflater

        LastTime = System.currentTimeMillis()

        inflate =(LayoutInflater) getSystemService (Context.LAYOUT_INFLATER_SERVICE)
        mview = inflate.inflate(R.layout.popup, null)
        var mParams : WindowManager.LayoutParams  = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE)
        wm.addView(mview, mParams)

        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(object : BroadcastReceiver()
        {
            override fun onReceive(context: Context, intent : Intent) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                val ShowMsg = intent.getStringExtra("chat")
            }
            },)

        handler.postDelayed(time(),5000)
    }

    private fun time : Runnable
    {
        onDestroy()
    }


    override fun onDestroy() {
        super.onDestroy()
        CurrentTime = System.currentTimeMillis()
        if((LastTime.plus(second)).compareTo(CurrentTime) < 0)
    }//when overlay run 5seconds, destroy overlay

    fun goFragment(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        val mView: View  = LayoutInflater.from(this).inflate(R.layout.chat_fragment, container, false)
        initViews(mView)
        return mView
    }

    private fun initViews(mView: View) {

        chatInput = mView.findViewById(R.id.chatInput)
        sendButton = mView.findViewById(R.id.sendButton)
        recyclerviewChat = mView.findViewById(R.id.chatRecyclerView)

        sendButton.isClickable = false
        sendButton.isEnabled = false

        val llm = LinearLayoutManager(this)
        llm.reverseLayout = true
        recyclerviewChat.layoutManager = llm

        chatInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {

                if (s.isNotEmpty()) {
                    sendButton.isClickable = true
                    sendButton.isEnabled = true
                }else {
                    sendButton.isClickable = false
                    sendButton.isEnabled = false
                }
            }
        })
        chatAdapter = ChatAdapter(messageList.reversed(),this)
        recyclerviewChat.adapter = chatAdapter

    }



}