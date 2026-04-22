package com.empresa.payments.infrastructure.web.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class PaymentRequest(
    @field:DecimalMin(value = "0.01", message = "Valor mínimo: R$ 0,01")
    val amount: BigDecimal,

    @field:NotBlank(message = "Moeda é obrigatória")
    @field:Size(min = 3, max = 3, message = "Código de moeda deve ter 3 letras (ex: BRL)")
    val currency: String,

    @field:NotBlank(message = "Método de pagamento é obrigatório")
    val method: String,
    val details: String? = null
)
