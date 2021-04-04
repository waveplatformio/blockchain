package io.waveplatform.rpc.server.context

import io.waveplatform.rpc.NoCallContextException
import io.waveplatform.rpc.server.IncomingMethodCall

object CallContextHolder {

    private val contexts = mutableMapOf<String, CallContext>()

    fun getCallContext(): CallContext
    {
        val name = getContextNameForThread()
        return contexts[name] ?: throw NoCallContextException(name)
    }

    fun storeCallContext(methodCall: IncomingMethodCall)
    {
        val name = getContextNameForThread()
        contexts[name] = CallContext(methodCall)
    }

    private fun getContextNameForThread(): String {
        return Thread.currentThread().name
    }

}