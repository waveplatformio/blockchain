package io.waveplatform.server.requests

import java.math.BigDecimal

data class WalletLoginRequest(
    val secret : String,
)

data class WalletResponse(
    val pubKey:String,
    val amount : BigDecimal,
    val stakeDate : Long,
)



