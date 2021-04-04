package io.waveplatform.server

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.waveplatform.server.exceptions.MissingParameterException
import io.waveplatform.server.exceptions.RequestResponseException

suspend inline fun <reified T: Any> ApplicationCall.receiveJson() : T {
    try {
        val request = this.receive<T>()
        return request
    }
    catch (parex : MissingKotlinParameterException){
        throw MissingParameterException(parex.originalMessage)
    }
    catch (cause : Throwable){
        throw RequestResponseException("Validation Error Parameter Unknown Or Missing")
    }
}



suspend inline fun ApplicationCall.respondError(action : String, message : String, statusCode : HttpStatusCode?) {
    val status = when(statusCode){
        null -> HttpStatusCode.BadRequest
        else -> statusCode
    }
    this.respond(
        status,
        mapOf(
            "action" to action,
            "message" to message
        )
    )

}