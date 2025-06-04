package com.devshiv.holesale

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager

class Utils {

    companion object {

        const val TAG : String = "MyTag"

        fun Window.transparentStatusBar() {
            setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            if (Build.VERSION.SDK_INT >= 21) {
                setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
                statusBarColor = Color.TRANSPARENT
            }
        }

        fun Window.transparentStatusAndNavigation() {
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            setWindowFlag(
                (WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION), false
            )
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }

        fun Window.setWindowFlag(bits: Int, on: Boolean) {
            val winParams = attributes
            if (on) {
                winParams.flags = winParams.flags or bits
            } else {
                winParams.flags = winParams.flags and bits.inv()
            }
            attributes = winParams
        }

        fun changeStatusBarColor(window: Window, root: View, color: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                    val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
                    view.setBackgroundColor(color)
                    root.fitsSystemWindows = false
                    view.setPadding(0, statusBarInsets.top, 0, 0)
                    insets
                }
            } else {
                window.statusBarColor = color
            }
        }

        fun changeNavigationBarColor(window: Window, root: View, color: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                    val navBarInsets = insets.getInsets(WindowInsets.Type.navigationBars())
                    view.setBackgroundColor(color)
                    root.fitsSystemWindows = false
                    view.setPadding(0, 0, 0, navBarInsets.bottom)
                    insets
                }
            } else {
                window.navigationBarColor = color
            }
        }

        fun changeSystemBarsColor(
            window: Window,
            root: View,
            statusBarColor: Int,
            navBarColor: Int
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                    val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
                    val navBarInsets = insets.getInsets(WindowInsets.Type.navigationBars())

                    view.setBackgroundColor(statusBarColor)
                    root.fitsSystemWindows = false
                    view.setPadding(0, statusBarInsets.top, 0, navBarInsets.bottom)
                    insets
                }
            } else {
                window.statusBarColor = statusBarColor
                window.navigationBarColor = navBarColor
            }
        }

    }
}