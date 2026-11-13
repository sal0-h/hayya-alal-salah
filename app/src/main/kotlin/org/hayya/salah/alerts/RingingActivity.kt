// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.alerts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import org.hayya.core.*
import org.hayya.salah.*
import org.hayya.salah.R

class RingingActivity : ComponentActivity() {
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        org.hayya.salah.focus.PrayerFocusService.onOwnWindowFocus(hasFocus)
    }

    override fun attachBaseContext(base: android.content.Context) { super.attachBaseContext(DeliveryLocale.context(base)) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else @Suppress("DEPRECATION") window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            val active by AlarmPlayback.current.collectAsState()
            val appearance = remember { DeliveryLocale.appearance(this@RingingActivity) }
            HayyaTheme(appearance) {
                Surface(Modifier.fillMaxSize()) {
                    Scaffold(bottomBar = {
                        if (active?.isFajr == true) OutlinedButton(onClick = { AlarmPlayback.stop(this@RingingActivity); finish() },
                            Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp).heightIn(min = 56.dp)) { Text(stringResource(R.string.emergency_stop)) }
                    }) { contentPadding ->
                    Column(Modifier.padding(contentPadding).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(stringResource(R.string.fajr_alarm), style = MaterialTheme.typography.headlineLarge)
                        Text(stringResource(R.string.dismiss_not_record))
                        if (active?.isFajr != true) {
                            Text(stringResource(R.string.alarm_finished))
                            Button(onClick = { finish() }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.close)) }
                        } else {
                            val entry = active!!
                            val questions = remember(entry.token) { arithmeticQuestions(entry.difficulty) }
                            var progress by rememberSaveable(entry.token) { mutableIntStateOf(0) }
                            var answer by rememberSaveable(entry.token) { mutableStateOf("") }
                            var incorrect by remember(entry.token) { mutableStateOf(false) }
                            if (progress < questions.size) {
                                val q = questions[progress]
                                Text(stringResource(R.string.question_progress, numberLabel(progress + 1), numberLabel(questions.size)))
                                Text("${numberLabel(q.left)} ${if (q.multiply) "×" else "+"} ${numberLabel(q.right)} = ?", style = MaterialTheme.typography.headlineMedium)
                                OutlinedTextField(answer, { answer = it; incorrect = false }, label = { Text(stringResource(R.string.answer)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = incorrect, modifier = Modifier.fillMaxWidth())
                                Button(onClick = {
                                    if (normalizeNumber(answer).toIntOrNull() == q.answer) { progress++; answer = ""; if (progress == questions.size) { AlarmPlayback.stop(this@RingingActivity); finish() } }
                                    else incorrect = true
                                }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.check_answer)) }
                            } else Button(onClick = { AlarmPlayback.stop(this@RingingActivity); finish() }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.stop_alarm)) }
                        }
                    }
                    }
                }
            }
        }
    }
}
