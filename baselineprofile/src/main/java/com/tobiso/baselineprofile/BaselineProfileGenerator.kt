package com.tobiso.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the baseline profile: cold start, then a few scrolls on whatever the home screen shows.
 * Run with `./gradlew :app:generateBaselineProfile` while a device is connected.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = "com.tobiso.tobisoapp") {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()

        // Scroll the first scrollable container a couple of times to warm up list rendering.
        device.findObject(By.scrollable(true))?.let {
            it.setGestureMargin(device.displayWidth / 5)
            it.scroll(Direction.DOWN, 1.0f)
            device.waitForIdle()
            it.scroll(Direction.UP, 1.0f)
            device.waitForIdle()
        }
    }
}
