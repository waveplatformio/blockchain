package io.waveplatform.server

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*

import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import io.waveplatform.blockchain.BlockChain
import io.waveplatform.blockchain.Constants
import io.waveplatform.blockchain.crypto.EncryptedData
import io.waveplatform.blockchain.db.*
import io.waveplatform.blockchain.db.services.BlockService
import io.waveplatform.blockchain.db.services.WalletService
import io.waveplatform.blockchain.generatePubKeyFromRandomMnemonics
import io.waveplatform.blockchain.wallet.TransactionPool
import io.waveplatform.server.config.appConfig
import io.waveplatform.server.config.configureDatabase
import io.waveplatform.server.exceptions.*
import io.waveplatform.server.handlers.serverHandler
import kotlinx.coroutines.async
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

import java.util.*
import org.slf4j.event.Level
import java.net.InetSocketAddress
import java.text.DateFormat

fun main(args: Array<String>) {
    val env = applicationEngineEnvironment {
        module {
            main()
        }
        connector {
            host = "127.0.0.1"
            port = 8181
        }
    }
    embeddedServer(Netty, env).start(true)
}


@Suppress("unused")
@kotlin.jvm.JvmOverloads
fun Application.main(testing: Boolean = false) {
    configureDatabase()

//    install(Compression) {
//        gzip {
//            priority = 1.0
//        }
//        deflate {
//            priority = 10.0
//            minimumSize(1024) // condition
//        }
//    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Post)
        method(HttpMethod.Get)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.AccessControlAllowHeaders)
        header(HttpHeaders.ContentType)
        header(HttpHeaders.AccessControlAllowOrigin)
        header("Access-Control-Allow-Origin")
        header("Access-Control-Allow-Headers")
        header("Control-Allow-Origin")

        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
        exposeHeader("account")
        headersOf("Access-Control-Allow-Headers","account")
        allowHeaders {
            true
        }

    }

    install(CallLogging){
        level = Level.INFO
//        filter { call -> call.request.path().startsWith("/") }
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            dateFormat = DateFormat.getDateInstance()
            deactivateDefaultTyping()
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
    }

    install(Sessions) {
        cookie<ExpirableSession>("SESSION", storage = SessionStorageMemory())
        header<WalletRecordDTO>("account", storage = SessionStorageMemory())
    }


    install(StatusPages) {
//        status(HttpStatusCode.NotFound) {
//            call.respond(TextContent("${it.value} ${it.description}", ContentType.Text.Plain.withCharset(Charsets.UTF_8), it))
//        }

        exception<AccountNotFoundException> { cause->
            call.respondError("ACCOUNT_NOT_FOUND",cause.localizedMessage,null)
        }
        exception<InvalidDataException> { cause ->
            call.respondError("INVALID_DATA", cause.localizedMessage,null)
        }
        exception< RequestResponseException> { cause ->
            call.respondError("IO_EXCEPTION",cause.localizedMessage, null)
        }
        exception<MissingParameterException> { cause ->
            call.respondError("MISSING_PARAMETER",cause.localizedMessage,null)
        }
        exception<NoSuchElementException> {
            call.respondError("ELEMENT_NOT_FOUND","Response is Empty",null)
        }
        exception<ContentCompilerException> { cause ->
            call.respondError("TRIGGER_CONTENT",cause.localizedMessage,null)
        }

        exception<InsufficientBalance> { cause ->
            call.respondError("INSUFFICIENT_AMOUNT", cause.localizedMessage,null)
        }
    }

    entryPoint()
}

data class ExpirableSession(val name: String, val expiration: Long)