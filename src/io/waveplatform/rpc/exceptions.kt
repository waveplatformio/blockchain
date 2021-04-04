package io.waveplatform.rpc



class JsonRpcException(override val message: String ) : RuntimeException(message) {
    companion object {
        const val errorMethodNotFound = -32601
        const val errorInvalidParams = -32602
        const val errorInternal = -32603
        const val errorInvalidRequest = -32600
        const val errorServer = -32000
        const val errorClientServerConnection = -32001
    }
}


class NoCallContextException(@Suppress("CanBeParameter") val threadName: String): Exception("No call context for thread $threadName")