package com.webianks.bluechat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.Snackbar
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

class popup : AppCompatActivity() {

    private lateinit var OKbt: Button
    private lateinit var Cancelbt : Button
    private lateinit var chatInput : EditText
    private lateinit var sendButton : Button
    private lateinit var recyclerviewChat: RecyclerView
    private var chatAdapter: ChatAdapter? = null
    private val messageList = arrayListOf<com.webianks.bluechat.Message>()

    override fun onCreate(savedInstanceState : Bundle?)
    {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        var layoutParams : WindowManager.LayoutParams
        layoutParams = WindowManager.LayoutParams()
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.7f
        getWindow().setAttributes(layoutParams)
        setContentView(R.layout.popup)

        findViewById<Button>(R.id.btn_ok).setOnClickListener{

            goFragment()
        }
        findViewById<Button>(R.id.btn_cancel).setOnClickListener{
            Exit()
        }
    }

    fun goFragment(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        val mView: View  = LayoutInflater.from(this).inflate(R.layout.chat_fragment, container, false)
        initViews(mView)
        return mView
    }
    fun Exit()
    {
        finish()
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