package com.empresa.payments.domain.model

import java.math.BigDecimal

data class CreatePaymentRequest(
    val amount: BigDecimal,
    val currency: String,
    val method: String,
    val details: String? = null
)