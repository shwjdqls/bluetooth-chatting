package com.webianks.bluechat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.Editable
import android.util.Log
import android.widget.*
import kotlinx.android.synthetic.main.popup.*

class ChatFragment : Fragment(), View.OnClickListener {

    private lateinit var chatInput: EditText
    private lateinit var sendButton: ImageButton
    private var communicationListener: CommunicationListener? = null
    private var chatAdapter: ChatAdapter? = null
    private lateinit var recyclerviewChat: RecyclerView
    private val messageList = arrayListOf<Message>()

    private var OverlayService : popup? = null

    private var message : String = ""
    private var date : String = ""
    private var time : String = ""
    private var intent : Intent? = null
    private val PERMISSION_REQUSET_OVERLAY: Int = 134
    private var alreadyAskedForPermission : Boolean = false


    private val mReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(Context: Context, intent : Intent) {

            message = intent.getStringExtra("messasge").toString()
            date = intent.getStringExtra("date").toString()
            time = intent.getStringExtra("time").toString()

            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }


    companion object {
        fun newInstance(): ChatFragment {
            val myFragment = ChatFragment()
            val args = Bundle()
            myFragment.arguments = args
            return myFragment
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermission()
        requestOverlayPermission()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !Settings.canDrawOverlays(this)) {
            var intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packagename"))
            startActivityForResult(intent, PERMISSION_REQUSET_OVERLAY)
        }

    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val mView: View  = LayoutInflater.from(activity).inflate(R.layout.chat_fragment, container, false)
        initViews(mView)
        return mView

    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop()
    {

        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !Settings.canDrawOverlays(this)) {
            var intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, PERMISSION_REQUSET_OVERLAY)
        } else {
            getActivity().startService(Intent(this, popup::class.java))
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PERMISSION_REQUSET_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                //
            } else {
            }
        }
    }

    private fun initViews(mView: View) {

        chatInput = mView.findViewById(R.id.chatInput)
        sendButton = mView.findViewById(R.id.sendButton)
        recyclerviewChat = mView.findViewById(R.id.chatRecyclerView)

        sendButton.isClickable = false
        sendButton.isEnabled = false

        val llm = LinearLayoutManager(activity)
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

        sendButton.setOnClickListener(this)

        chatAdapter = ChatAdapter(messageList.reversed(),activity)
        recyclerviewChat.adapter = chatAdapter

    }

    override fun onClick(p0: View?) {

        if (chatInput.text.isNotEmpty()){
            communicationListener?.onCommunication(chatInput.text.toString())
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            //intent.putExtra("chatoverlay",chatInput.text.toString())
            chatInput.setText("")
            Log.e("For Overlay Data", "OVERRAY DATA")
        }
    }


    fun setCommunicationListener(communicationListener: CommunicationListener){
       this.communicationListener = communicationListener
   }

    interface CommunicationListener{
        fun onCommunication(message: String)
    }

    fun communicate(message: Message){
        messageList.add(message)
        if(activity != null) {
            chatAdapter = ChatAdapter(messageList.reversed(), activity)
            recyclerviewChat.adapter = chatAdapter
            recyclerviewChat.scrollToPosition(0)

        }
    }

    private fun checkPermission() {
        if (alreadyAskedForPermission)
            return
        else
            alreadyAskedForPermission = true
    }

    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packagename"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        getActivity().stopService(Intent(this, popup::class.java))
    }

    private fun Context.bindService(serviceIntent: Intent) {
    }

}