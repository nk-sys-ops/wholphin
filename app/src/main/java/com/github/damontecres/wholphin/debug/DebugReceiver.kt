package com.github.damontecres.wholphin.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.core.DataStore
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.updateExperimentalPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DebugReceiver : BroadcastReceiver() {
    @Inject
    lateinit var dataStore: DataStore<AppPreferences>

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.github.damontecres.wholphin.UPDATE_PREFS") {
            Log.d("DebugReceiver", "Received UPDATE_PREFS intent")
            GlobalScope.launch {
                dataStore.updateData { prefs ->
                    prefs.updateExperimentalPreferences {
                        if (intent.hasExtra("tsDirectPlay")) {
                            val value = intent.getBooleanExtra("tsDirectPlay", false)
                            Log.d("DebugReceiver", "Setting tsDirectPlay = $value")
                            tsDirectPlay = value
                        }
                        if (intent.hasExtra("preferAc3Surround")) {
                            val value = intent.getBooleanExtra("preferAc3Surround", true)
                            Log.d("DebugReceiver", "Setting preferAc3Surround = $value")
                            preferAc3Surround = value
                        }
                    }
                }
            }
        }
    }
}
