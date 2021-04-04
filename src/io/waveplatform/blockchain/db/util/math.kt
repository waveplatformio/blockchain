package io.waveplatform.blockchain.db.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode


fun convertDecimal(amount : Double) : BigDecimal {
    return BigDecimal(amount).setPrecision(8)
}

fun BigDecimal.setPrecision(newPrecision: Int) = BigDecimal(toPlainString(), MathContext(newPrecision, RoundingMode.HALF_UP))
