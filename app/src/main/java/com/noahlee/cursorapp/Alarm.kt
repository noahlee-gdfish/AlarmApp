package com.noahlee.cursorapp

data class Alarm(
    val id: Long,
    val title: String,
    val triggerAtMillis: Long,
)
