package ink.duo3.tuned.data.preferences

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.tunedSettingsDataStore by preferencesDataStore(name = "settings")
