package com.cacaoforecast.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cacaoforecast.ai.ui.navigation.CacaoNavGraph
import com.cacaoforecast.ai.ui.theme.CacaoForecastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CacaoForecastApp).container
        setContent {
            CacaoForecastTheme {
                CacaoNavGraph(appContainer = container)
            }
        }
    }
}
