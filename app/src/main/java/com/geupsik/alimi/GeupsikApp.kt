package com.geupsik.alimi

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras

class GeupsikApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.mealNotifier.createChannel()
    }
}

/** ViewModel 팩토리 안에서 AppContainer를 꺼낼 때 쓴다. */
val CreationExtras.appContainer: AppContainer
    get() = (this[APPLICATION_KEY] as GeupsikApp).container
