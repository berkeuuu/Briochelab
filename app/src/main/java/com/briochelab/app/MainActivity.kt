package com.briochelab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.briochelab.app.data.AppDb
import com.briochelab.app.data.Repo
import com.briochelab.app.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = Repo(applicationContext, AppDb.get(applicationContext))
        setContent {
            var ready by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                repo.ensureDefaults()
                ready = true
            }
            if (ready) AppRoot(repo)
        }
    }
}
