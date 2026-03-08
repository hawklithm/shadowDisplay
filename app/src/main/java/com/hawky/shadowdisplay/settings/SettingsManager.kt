package com.hawky.shadowdisplay.settings

import android.content.Context
import android.content.SharedPreferences
import com.hawky.shadowdisplay.models.DisplayMode
import com.hawky.shadowdisplay.models.ScreenSaverSettings
import com.hawky.shadowdisplay.models.TriggerMode

/**
 * 设置管理器
 */
class SettingsManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "screen_saver_settings"

        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_TRIGGER_MODE = "trigger_mode"
        private const val KEY_AUTO_ROTATION = "auto_rotation"
        private const val KEY_AUTO_BRIGHTNESS = "auto_brightness"
        private const val KEY_MANUAL_BRIGHTNESS = "manual_brightness"
        private const val KEY_BURN_IN_PROTECTION = "burn_in_protection"

        private const val KEY_DIGITAL_CLOCK_SHOW_DATE = "digital_clock_show_date"
        private const val KEY_DIGITAL_CLOCK_SHOW_WEEKDAY = "digital_clock_show_weekday"
        private const val KEY_DIGITAL_CLOCK_COLOR = "digital_clock_color"
        private const val KEY_DIGITAL_CLOCK_SIZE = "digital_clock_size"

        private const val KEY_ANALOG_CLOCK_STYLE = "analog_clock_style"
        private const val KEY_ANALOG_CLOCK_SMOOTH_SECONDS = "analog_clock_smooth_seconds"

        private const val KEY_EYES_GAZE_FREQUENCY = "eyes_gaze_frequency"
        private const val KEY_EYES_BLINK_FREQUENCY = "eyes_blink_frequency"
        private const val KEY_EYES_IRIS_COLOR = "eyes_iris_color"

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSettings(): ScreenSaverSettings {
        return ScreenSaverSettings(
            displayMode = DisplayMode.valueOf(
                prefs.getString(KEY_DISPLAY_MODE, DisplayMode.DIGITAL_CLOCK.name)
                    ?: DisplayMode.DIGITAL_CLOCK.name
            ),
            triggerMode = TriggerMode.valueOf(
                prefs.getString(KEY_TRIGGER_MODE, TriggerMode.CHARGING_ONLY.name)
                    ?: TriggerMode.CHARGING_ONLY.name
            ),
            autoRotation = prefs.getBoolean(KEY_AUTO_ROTATION, false),
            autoBrightness = prefs.getBoolean(KEY_AUTO_BRIGHTNESS, true),
            manualBrightness = prefs.getFloat(KEY_MANUAL_BRIGHTNESS, 1.0f),
            burnInProtection = prefs.getBoolean(KEY_BURN_IN_PROTECTION, true),

            digitalClockShowDate = prefs.getBoolean(KEY_DIGITAL_CLOCK_SHOW_DATE, true),
            digitalClockShowWeekday = prefs.getBoolean(KEY_DIGITAL_CLOCK_SHOW_WEEKDAY, true),
            digitalClockColor = prefs.getInt(KEY_DIGITAL_CLOCK_COLOR, 0xFFFFFFFF.toInt()),
            digitalClockSize = prefs.getFloat(KEY_DIGITAL_CLOCK_SIZE, 1.0f),

            analogClockStyle = prefs.getInt(KEY_ANALOG_CLOCK_STYLE, 0),
            analogClockSmoothSeconds = prefs.getBoolean(KEY_ANALOG_CLOCK_SMOOTH_SECONDS, true),

            eyesGazeFrequency = prefs.getInt(KEY_EYES_GAZE_FREQUENCY, 5),
            eyesBlinkFrequency = prefs.getInt(KEY_EYES_BLINK_FREQUENCY, 10),
            eyesIrisColor = prefs.getInt(KEY_EYES_IRIS_COLOR, 0xFF4CAF50.toInt())
        )
    }

    fun saveSettings(settings: ScreenSaverSettings) {
        prefs.edit().apply {
            putString(KEY_DISPLAY_MODE, settings.displayMode.name)
            putString(KEY_TRIGGER_MODE, settings.triggerMode.name)
            putBoolean(KEY_AUTO_ROTATION, settings.autoRotation)
            putBoolean(KEY_AUTO_BRIGHTNESS, settings.autoBrightness)
            putFloat(KEY_MANUAL_BRIGHTNESS, settings.manualBrightness)
            putBoolean(KEY_BURN_IN_PROTECTION, settings.burnInProtection)

            putBoolean(KEY_DIGITAL_CLOCK_SHOW_DATE, settings.digitalClockShowDate)
            putBoolean(KEY_DIGITAL_CLOCK_SHOW_WEEKDAY, settings.digitalClockShowWeekday)
            putInt(KEY_DIGITAL_CLOCK_COLOR, settings.digitalClockColor)
            putFloat(KEY_DIGITAL_CLOCK_SIZE, settings.digitalClockSize)

            putInt(KEY_ANALOG_CLOCK_STYLE, settings.analogClockStyle)
            putBoolean(KEY_ANALOG_CLOCK_SMOOTH_SECONDS, settings.analogClockSmoothSeconds)

            putInt(KEY_EYES_GAZE_FREQUENCY, settings.eyesGazeFrequency)
            putInt(KEY_EYES_BLINK_FREQUENCY, settings.eyesBlinkFrequency)
            putInt(KEY_EYES_IRIS_COLOR, settings.eyesIrisColor)

            apply()
        }
    }

    fun updateDisplayMode(mode: DisplayMode) {
        prefs.edit().putString(KEY_DISPLAY_MODE, mode.name).apply()
    }

    fun updateTriggerMode(mode: TriggerMode) {
        prefs.edit().putString(KEY_TRIGGER_MODE, mode.name).apply()
    }
}
