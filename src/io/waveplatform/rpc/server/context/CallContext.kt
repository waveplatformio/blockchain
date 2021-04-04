package io.waveplatform.rpc.server.context

import io.waveplatform.rpc.server.IncomingMethodCall

data class CallContext(val methodCall: IncomingMethodCall)