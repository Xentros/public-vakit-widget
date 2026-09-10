package com.vakit.widget.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class PrayerTimesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: PrayerTimesWidget = PrayerTimesWidget()
}