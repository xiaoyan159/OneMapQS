package com.navinfo.collect.library.map

import android.location.Location


class NILocation(provider: String) : Location(provider) {
    var radius = 0f
}