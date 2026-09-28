package com.ahora.app

import android.app.Application
import com.ahora.app.di.AppContainer
import com.ahora.app.notifications.NotificationHelper

class AhoraApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
    }
}
