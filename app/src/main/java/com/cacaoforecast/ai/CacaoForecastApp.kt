package com.cacaoforecast.ai

import android.app.Application
import com.cacaoforecast.ai.di.AppContainer

class CacaoForecastApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
