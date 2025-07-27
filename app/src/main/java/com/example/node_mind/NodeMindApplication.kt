package com.example.node_mind

import android.app.Application
import com.example.node_mind.di.AppContainer

class NodeMindApplication : Application() {
    
    lateinit var container: AppContainer
        private set
    
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
