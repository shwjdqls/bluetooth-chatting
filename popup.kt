package com.webianks.bluechat

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.popup.*
import kotlin.coroutines.coroutineContext
import kotlin.math.acos

class popup : AppCompatActivity(){

    override  fun onCreate(savedInstanceState : Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup)

        popupbutton.setOnClickListener()
        {

            val intent = Intent(this, ChatFragment::class.java)
            startActivity(intent)
        }
    }

}