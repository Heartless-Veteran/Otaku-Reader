package com.otakureader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.otakureader.ui.AppViewModel
import com.otakureader.ui.navigation.OtakuNavGraph
import com.otakureader.ui.theme.OtakuReaderTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appViewModel: AppViewModel = viewModel()
            val settings by appViewModel.settings.collectAsState()
            val navController = rememberNavController()

            OtakuReaderTheme(
                theme = settings.theme,
                accent = settings.accent,
                dynamicColor = settings.dynamicColor,
            ) {
                OtakuNavGraph(
                    navController = navController,
                    appViewModel = appViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
