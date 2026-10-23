package com.findly.core.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToLong

/** Money stored in minor units (cents) to avoid floating point errors. */
@Serializable
data class Money(
    val amountMinor: Long,
    /** ISO 4217 code, e.g. "EUR". */
    val currency: String,
) : Comparable<Money> {

    val amount: BigDecimal get() = BigDecimal.valueOf(amountMinor, 2)

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return Money(amountMinor + other.amountMinor, currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return Money(amountMinor - other.amountMinor, currency)
    }

    override fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return amountMinor.compareTo(other.amountMinor)
    }

    companion object {
        fun of(amount: Double, currency: String = "EUR") = Money((amount * 100).roundToLong(), currency)
        fun of(amount: BigDecimal, currency: String = "EUR") =
            Money(amount.setScale(2, RoundingMode.HALF_UP).unscaledValue().toLong(), currency)
    }
}
