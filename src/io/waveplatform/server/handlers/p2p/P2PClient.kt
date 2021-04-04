package io.waveplatform.server.handlers.p2p

import io.waveplatform.blockchain.Block
import io.waveplatform.blockchain.wallet.Transaction
import io.waveplatform.rpc.server.service.FullNodeService
import io.waveplatform.server.handlers.p2p.messages.Packet
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.handshake.ServerHandshake
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.net.URI


class P2PClient(
    uri: URI
)  {

    companion object {
        val log = LoggerFactory.getLogger(FullNodeService::class.java)
    }

    var webSocketClient: WebSocketClient = object : WebSocketClient(uri) {
        override fun onOpen(handshakedata: ServerHandshake?) {

        }
        override fun onMessage(message: String) {
            println("***************onMEssage P2PClient***************")
            println(message)
            println("***************onMEssage end***************")

        }
        override fun onClose(code: Int, reason: String?, remote: Boolean) {

        }
        override fun onError(ex: Exception?) {

        }
    }


    init {
        this.webSocketClient.connect()
    }

    fun syncNode(){
//        try{
            this.webSocketClient.send(
                Packet.sync()
            )
//        }catch (wnE : WebsocketNotConnectedException){
//            wnE.printStackTrace()
//        }
    }

    fun broadcastUnfreeze(transaction: Transaction) {
        this.webSocketClient.send(
            Packet.serve(transaction,MessageTypes.UNFREEZE)
        )
    }

    fun broadcastFreeze(transaction: Transaction) {
        this.webSocketClient.send(
            Packet.serve(transaction,MessageTypes.FREEZE)
        )
    }

    fun broadcastTransaction(transaction: Transaction){
        this.sendTransaction(this.webSocketClient,transaction)
    }

    private fun sendTransaction(conn: WebSocketClient,transaction: Transaction){

        conn.send(
            Packet.serve(transaction)
        )
    }

    fun broadcastBlock(block: Block){
        this.sendBlock(this.webSocketClient,block)
    }

    private fun sendBlock(conn: WebSocket, block: Block){
        println("sendBlockFromClient")
        conn.send(
            Packet.serve(block)
        )
    }

}