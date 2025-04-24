package com.marat.app.ui

data class Command(
    val title: String,
    val description: String,
    val iconRes: Int,
    val bits: List<Int>
)