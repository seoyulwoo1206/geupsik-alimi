package com.geupsik.alimi.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geupsik.alimi.ui.components.UpdateDialog
import com.geupsik.alimi.ui.theme.GeupsikTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeupsikTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModel.Factory)
                val start by vm.startState.collectAsStateWithLifecycle()
                val update by vm.update.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    // 저장된 학교를 읽는 아주 짧은 순간에는 빈 배경만 보여준다
                    when (start) {
                        StartState.Loading -> Unit
                        StartState.NeedsSchool -> AppNavHost(hasSchool = false)
                        StartState.Ready -> AppNavHost(hasSchool = true)
                    }
                }
                update?.let { info ->
                    UpdateDialog(info = info, onDownload = vm::onUpdateOpened, onLater = vm::dismissUpdate)
                }
            }
        }
    }
}
