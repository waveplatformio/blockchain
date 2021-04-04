package io.waveplatform.rpc.server.service

data class CreatePoolRequest(
    val pool: String,
    val amount:Double,
    val privKey:String,
    val pubKey:String
)