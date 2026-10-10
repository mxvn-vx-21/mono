package com.mono.app

import android.app.Application
import com.mono.app.data.AppContainer

class MonoApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
