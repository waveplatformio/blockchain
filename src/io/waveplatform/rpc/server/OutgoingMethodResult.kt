package io.waveplatform.rpc.server

import io.waveplatform.rpc.objects.MethodError
import io.waveplatform.rpc.objects.RpcObject

data class OutgoingMethodResult(val id: Int, val result: Any?, val error: MethodError?): RpcObject()
{
    fun hasError(): Boolean
    {
        return error != null
    }
}