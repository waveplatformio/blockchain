package io.waveplatform.rpc.server

import com.fasterxml.jackson.databind.JsonNode
import io.waveplatform.rpc.objects.RpcObject
import java.util.*

data class IncomingMethodCall(val id: Int, val method: String, val params: Array<JsonNode>): RpcObject()
{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IncomingMethodCall

        if (id != other.id) return false
        if (method != other.method) return false
        if (!Arrays.equals(params, other.params)) return false
        if (jsonrpc != other.jsonrpc) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + method.hashCode()
        result = 31 * result + Arrays.hashCode(params)
        result = 31 * result + jsonrpc.hashCode()
        return result
    }


}