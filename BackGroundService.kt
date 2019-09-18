package com.webianks.bluechat

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class BackGroundService : Service() {

    private val myBinder = MyLocalBinder()
    override fun onBind(p0: Intent?): IBinder {
        ruturn myBinder
    }
    inner class MyLocalBinder : Binder()
    {
        fun get
    }
}