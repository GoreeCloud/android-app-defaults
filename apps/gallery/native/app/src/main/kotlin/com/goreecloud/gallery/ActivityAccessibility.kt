package com.goreecloud.gallery

import android.app.Activity

fun Activity.announceForAccessibility(message: CharSequence) {
    window?.decorView?.announceForAccessibility(message)
}
