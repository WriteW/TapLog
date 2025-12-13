package com.roroi.taplog.daily.subScreen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.roroi.taplog.daily.viewmodel.DailyViewModel

@Composable
fun PortalEditor(viewModel: DailyViewModel) {
    Scaffold(
        topBar = {
            PortalTB(viewModel)
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .background(Color.Green)
            .fillMaxSize()) {
            Text("Portal's adding and editing...WIP...", textAlign = TextAlign.Center, modifier = Modifier.fillMaxSize().wrapContentSize(
                Alignment.Center))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalTB(viewModel: DailyViewModel) {
    CenterAlignedTopAppBar(
        title = {
            Text("Portal")
        },
        navigationIcon = {
            IconButton(onClick = {
                viewModel.navigatePop()
                Log.d("snake is cute", ".23")
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
    )
}