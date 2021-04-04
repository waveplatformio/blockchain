package io.waveplatform.rpc.server


import java.lang.reflect.Method
import kotlin.reflect.KClass

data class CallableMethod(val method: Method, val impl: Any, val intface: KClass<*>)