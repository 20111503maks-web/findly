package com.findly.app.ui.common

import androidx.annotation.StringRes
import com.findly.app.R
import com.findly.core.ai.AiIntent
import com.findly.core.model.Availability
import com.findly.core.model.Category
import com.findly.core.model.Money
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** "€389" / "389 €" depending on locale; drops ",00" for round prices. */
fun formatMoney(money: Money, locale: Locale = Locale.getDefault()): String {
    val format = NumberFormat.getCurrencyInstance(locale)
    runCatching { format.currency = Currency.getInstance(money.currency) }
    val round = money.amountMinor % 100 == 0L
    format.minimumFractionDigits = if (round) 0 else 2
    format.maximumFractionDigits = if (round) 0 else 2
    return format.format(money.amount)
}

fun formatRating(rating: Double): String = String.format(Locale.getDefault(), "%.1f", rating)

@StringRes
fun Category.labelRes(): Int = when (this) {
    Category.SMARTPHONES -> R.string.category_smartphones
    Category.AUDIO -> R.string.category_audio
    Category.COMPUTER_ACCESSORIES -> R.string.category_computer
    Category.SHOES -> R.string.category_shoes
    Category.CLOTHING -> R.string.category_clothing
    Category.BAGS -> R.string.category_bags
    Category.HOME -> R.string.category_home
    Category.OTHER -> R.string.category_other
}

@StringRes
fun AiIntent.labelRes(): Int = when (this) {
    AiIntent.MINIMALIST -> R.string.intent_minimalist
    AiIntent.NO_BIG_LOGOS -> R.string.intent_no_big_logos
    AiIntent.FOR_SCHOOL -> R.string.intent_for_school
    AiIntent.PREMIUM_DESIGN -> R.string.intent_premium
    AiIntent.SIMILAR_TO -> R.string.intent_similar
}

@StringRes
fun Availability.labelRes(): Int? = when (this) {
    Availability.IN_STOCK -> R.string.availability_in_stock
    Availability.LOW_STOCK -> R.string.availability_low_stock
    Availability.OUT_OF_STOCK -> R.string.availability_out_of_stock
    Availability.PREORDER -> R.string.availability_preorder
    Availability.UNKNOWN -> null
}

@StringRes
fun colorLabelRes(color: String): Int? = when (color) {
    "white" -> R.string.color_white
    "black" -> R.string.color_black
    "grey" -> R.string.color_grey
    "blue" -> R.string.color_blue
    "red" -> R.string.color_red
    "green" -> R.string.color_green
    "beige" -> R.string.color_beige
    "brown" -> R.string.color_brown
    "pink" -> R.string.color_pink
    "yellow" -> R.string.color_yellow
    "purple" -> R.string.color_purple
    "silver" -> R.string.color_silver
    else -> null
}

/** Swatch colour (ARGB) for colour filter dots. */
fun colorSwatch(color: String): Long = when (color) {
    "white" -> 0xFFFFFFFF
    "black" -> 0xFF111111
    "grey" -> 0xFF8E8E93
    "blue" -> 0xFF2F5BEA
    "red" -> 0xFFE0352B
    "green" -> 0xFF2E9B5A
    "beige" -> 0xFFE3D3B5
    "brown" -> 0xFF7A4B2A
    "pink" -> 0xFFF29BB8
    "yellow" -> 0xFFF4CC3A
    "purple" -> 0xFF7B4FC4
    "silver" -> 0xFFC9CCD1
    else -> 0xFFBDBDBD
}
