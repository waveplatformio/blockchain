package io.waveplatform.server.requests

data class CreateStakeRequest(
    val pool : String,
    val amount : Double
)

data class TransactionRequest(
    val to : String,
    val amount : Double,
    val type : String
)

data class FreezeRequest(
    val amount : Double,
    val type : String
)

data class CalculateFee(
    val amount: Double
)

data class TransactionRequestSignable(
    val pubKey : String,
    val privKey : String,
    val to : String,
    val amount : Double,
    val type : String
)


