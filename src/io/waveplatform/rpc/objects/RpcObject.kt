package io.waveplatform.rpc.objects

data class MethodError(val code: Int, val message: String, val data: Any?)

abstract class RpcObject {
    companion object {
        const val jsonRpcVersion = "2.0"
    }

    @Suppress("MemberVisibilityCanBePrivate")
    val jsonrpc = jsonRpcVersion
}