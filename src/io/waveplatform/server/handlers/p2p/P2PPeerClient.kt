package io.waveplatform.server.handlers.p2p

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.waveplatform.blockchain.*
import io.waveplatform.blockchain.crypto.Convert
import io.waveplatform.blockchain.db.WalletRecordDTO
import io.waveplatform.blockchain.db.services.Batch
import io.waveplatform.blockchain.wallet.Transaction
import io.waveplatform.blockchain.wallet.TransactionPool
import io.waveplatform.server.handlers.p2p.messages.Message
import io.waveplatform.server.handlers.p2p.messages.Packet
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI


class P2PPeerClient(
    uri: URI,
    transactionPool: TransactionPool,
    blockChain: BlockChain,
    wallet: WalletRecordDTO,
)  {
    val client : P2PClient = P2PClient(URI.create("ws://localhost:8282"))

    var webSocketClient: WebSocketClient = object : WebSocketClient(uri) {
        override fun onOpen(handshakedata: ServerHandshake?) {
        }

        override fun onMessage(message: String) {
            println("p2pClientOnMessage")
            println(message)
            val mapper = jacksonObjectMapper()

            try{
                val message = mapper.readValue(message, Message::class.java)
                println(message)
                when(message.type!!) {
                    MessageTypes.SYNC -> handleSync(message.height!!)
                    MessageTypes.BLOCK -> handleReceivedBlock(message.block!!)
                    MessageTypes.TRANSACTION -> handleReceivedTransaction(message.transaction!!)
                    MessageTypes.FREEZE -> handleReceivedFreeze(message.transaction!!)
                    MessageTypes.UNFREEZE -> handleReceivedUnfreeze(message.transaction!!)
                }
            }catch (jpe : JsonProcessingException){

            }catch (jmpe : JsonMappingException){

            }
        }


        fun handleSync(height : CurrentChain?){
            println("handle sync p2pPeerClient")
            var chain : CurrentChain = height!!
            val chainHeight = blockChain.getHeight()
            println("p2pPeerClient our db block height is  " + chainHeight)
            println("p2pPeerClient given height  " + chain.height)

            when(chain.state) {

                ChainMode.NEXT -> {
                    //here we need create block and send back check request
                    val synced = blockChain.syncChain(chain)
                    if (synced){
                        State.progress = (( chainHeight*100) / chain.height).toInt()
                        chain = CurrentChain(
                            chain.height,
                            blockChain.getBlockByHeight(chain.height),
                            state = ChainMode.CHECK
                        )
                        State.unSync()
                    }else{
                        chain = CurrentChain(
                            chain.height,
                            blockChain.getBlockByHeight(chain.height),
                            state = ChainMode.FINISHED
                        )
                        State.synced()
                    }
                }
                ChainMode.FINISHED -> {
                    //chain mode check if our chain less it means need syn
                    println("chain check finished p2p")
                    if (chain.height > chainHeight){
                        State.unSync()
                    }else{
//                        blockChain.syncFreezeExtraFeature(chain.block.transactions.filter {
//                            it.type.type == "FREEZE"
//                        })
                        println("synced freeze")
                        State.synced()
                    }
                }
            }

            //and then need to broadcast back
            send(
                Packet.serve(chain)
            )
        }


        private fun handleReceivedFreeze(transaction: Transaction){
            if (!transactionPool.transactionExists(transaction)){
                val added = transactionPool.addFreeze(transaction)
                if (added){
//                    broadcastFreeze(transaction)
                }
            }
        }

        private fun handleReceivedUnfreeze(transaction: Transaction){
            if (!transactionPool.transactionExists(transaction)){
                val added = transactionPool.addUnfreeze(transaction)
                if (added){
//                    this.broadcastFreeze(transaction)
                }
            }
        }

        fun broadcastFreeze(transaction: Transaction){
            client.broadcastFreeze(transaction)
        }

        private fun handleReceivedTransaction(transaction: Transaction){
            println("FromClientHandleReceiveTransaction")
            println(transaction.id)
            if (!transactionPool.transactionExists(transaction)){
                val added = transactionPool.addTransaction(transaction)
                val lastTransaction = transactionPool.getTransaction(transaction.txId)
                if (added){
                    broadcastTransaction(lastTransaction)
                }
            }

            if (transactionPool.thresholdReached()){
                if (blockChain.poolLeaderExists(wallet.pubKey)){
                    println("FromClient creating block")
                    val block = blockChain.createBlock(
                        transactionPool.unfinishedTransactions()
                    )
                    println("FromClient broadcastingBlock")
                    broadcastBlock(block)
                }
            }
        }


        private fun handleReceivedBlock(block: Block){
            if (!blockChain.isLastBlock(block)){
                if (blockChain.isValidBlock(block)){
                    blockChain.addBlock(block)
                    broadcastBlock(block)
                    println("FromClient transaction pool clearing")
                    transactionPool.clear()
                }
            }else{
                //todo feature
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {

        }
        override fun onError(ex: Exception?) {
            println("onErrorP2p")
            ex?.printStackTrace()
        }
    }

    public fun getWsClient(): WebSocket{
        return this.webSocketClient
    }

    init {
        this.webSocketClient.connect()
    }

    fun broadcastTransaction(transaction: Transaction){
        this.sendTransaction(transaction)
    }

    private fun sendTransaction(transaction: Transaction){
        client.broadcastTransaction(transaction)
    }

    fun broadcastBlock(block: Block){
        this.sendBlock(this.webSocketClient,block)
    }

    private fun sendBlock(conn: WebSocket, block: Block){
        println("sendBlockFromClient")
        client.broadcastBlock(block)
//        conn.send(
//            Packet.serve(block)
//        )
    }

}