package com.yeasin.myjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yeasin.myjournal.ui.AppNav
import com.yeasin.myjournal.ui.theme.MyJournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MyJournalTheme { AppNav() }
        }
    }
}
