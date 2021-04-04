package io.waveplatform.rpc.server

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.waveplatform.rpc.JsonRpcException
import io.waveplatform.rpc.objects.MethodError
import io.waveplatform.rpc.server.context.CallContextHolder
import org.slf4j.LoggerFactory

class JsonRpcServer(private val services: Array<Any>) {

    companion object {
        val log = LoggerFactory.getLogger(JsonRpcServer::class.java)

        val mapper = jacksonObjectMapper().configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)

        const val JsonRpcAuthorizationTokenType = "JsonRpcToken"
    }


    private fun callMethod(call: IncomingMethodCall): OutgoingMethodResult
    {
        try {
            val method = MethodIdentifier.createFromFullName(call.method)
            val impl = findAndVerifyImplementation(method)

            if(call.params.size != impl.method.parameterCount)
            {
                return createInvalidParamsError(call.id,
                    "given parameter count (${call.params.size} != required (${impl.method.parameterCount}")
            }

            CallContextHolder.storeCallContext( call)

            log.trace("starting invoke on proxy")
            val result = impl.method.invoke(impl.impl,
                *impl.method.parameterTypes.mapIndexed {
                        index, cls -> mapper.treeToValue(call.params[index], cls)
                }.toTypedArray())
            log.trace("proxy invoke success")

            return OutgoingMethodResult(call.id, result, null)
        }
        catch (throwable: Throwable) {
            return when(throwable) {
                is JsonRpcException -> OutgoingMethodResult(call.id, null, MethodError(1, throwable.message, null))
                is IllegalArgumentException -> createInvalidParamsError(call.id, throwable.message!!)
                is JsonProcessingException -> createInvalidParamsError(call.id, throwable.message!!)
                else -> OutgoingMethodResult(call.id, null, MethodError(
                    JsonRpcException.errorServer, throwable.message
                    ?: "internal server error", null))
            }
        }
    }

    private fun validateTokenAndStripType(authorizationHeader: String?): String? {
        return if(authorizationHeader == null)
        {
            null
        }
        else if(!authorizationHeader.startsWith(JsonRpcAuthorizationTokenType))
        {
            null
        }
        else
        {
            authorizationHeader.substring(JsonRpcAuthorizationTokenType.length+1)
        }
    }

    private fun createInvalidParamsError(id: Int, message: String): OutgoingMethodResult = OutgoingMethodResult(id, null, MethodError(JsonRpcException.errorInvalidParams, message, null))

    private fun findAndVerifyImplementation(method: MethodIdentifier): CallableMethod {
        val impl = services
            .filter { service -> service.javaClass.interfaces.any { it.simpleName == method.intface } &&
                    service.javaClass.methods.any { it.name == method.methodName }}
            .map { service ->
                CallableMethod(
                    service.javaClass.methods.first { it.name == method.methodName },
                    service,
                    service.javaClass.interfaces.first { it.simpleName == method.intface }.kotlin) }
            .firstOrNull()

        if(impl != null)
        {
            log.debug("found implementation for $method: ${impl.impl} - ${impl.method}")
            return impl
        }
        else
        {
            throw JsonRpcException("no implementation found for $method")
        }
    }

    suspend fun handle(call: ApplicationCall)
    {
        val methodCall = call.receive<IncomingMethodCall>()
        val result = callMethod(methodCall)

        val status = if(result.hasError() && isInternalError(result.error!!.code))
        {
            HttpStatusCode.BadRequest
        }
        else if(result.hasError())
        {
            HttpStatusCode.BadRequest
        }
        else
        {
            HttpStatusCode.OK
        }
        call.respond(status, result)
    }

    private fun isInternalError(code: Int) = code < 0
}