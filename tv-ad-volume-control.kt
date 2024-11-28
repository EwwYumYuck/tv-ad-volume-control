// MainActivity.kt
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.media.AudioManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private val audioAnalyzer = AudioAnalyzer()
    private var monitoringJob: Job? = null
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        if (hasPermissions()) {
            initializeAudioMonitoring()
        } else {
            requestPermissions()
        }
    }
    
    private fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeAudioMonitoring()
            } else {
                Toast.makeText(this, "Permissions required for ad detection", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun initializeAudioMonitoring() {
        audioAnalyzer.initialize()
        startAudioMonitoring()
    }
    
    private fun startAudioMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = coroutineScope.launch {
            while(isActive) {
                checkAndAdjustVolume()
                delay(1000) // Check every second
            }
        }
    }
    
    private suspend fun checkAndAdjustVolume() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val sensitivityThreshold = prefs.getInt("ad_sensitivity", 70)
        val isEnabled = prefs.getBoolean("enable_ad_control", true)
        
        if (isEnabled) {
            val isAdvertisement = audioAnalyzer.detectAdvertisement(sensitivityThreshold)
            if (isAdvertisement) {
                reduceVolume(sensitivityThreshold)
            }
        }
    }
    
    private fun reduceVolume(percentage: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val reducedVolume = (currentVolume * (percentage / 100f)).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, reducedVolume, 0)
    }
    
    override fun onDestroy() {
        coroutineScope.cancel()
        audioAnalyzer.release()
        super.onDestroy()
    }
}

// SettingsFragment.kt
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        // Configure sensitivity slider
        val sensitivityPreference = findPreference<SeekBarPreference>("ad_sensitivity")
        sensitivityPreference?.apply {
            min = 10
            max = 90
            value = 70
        }
    }
}

// AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.tvadcontrol">
    
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
    
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">
        
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
        
        <activity android:name=".SettingsFragment"/>
    </application>
</manifest>

// preferences.xml (in res/xml/)
<?xml version="1.0" encoding="utf-8"?>
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">
    <SeekBarPreference
        android:key="ad_sensitivity"
        android:title="Ad Detection Sensitivity"
        android:summary="Adjust the sensitivity of ad volume reduction"
        android:defaultValue="70"
        android:max="90"
        android:min="10"/>
    
    <SwitchPreference
        android:key="enable_ad_control"
        android:title="Enable Ad Volume Control"
        android:defaultValue="true"/>
</PreferenceScreen>
