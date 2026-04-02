package com.noahlee.alarmapp

data class Alarm(
    val id: Long,
    val title: String,
    val triggerAtMillis: Long,
)
