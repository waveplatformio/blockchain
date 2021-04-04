package io.waveplatform.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.waveplatform.blockchain.*
import io.waveplatform.blockchain.db.WalletRecordDTO
import io.waveplatform.blockchain.wallet.Transaction
import io.waveplatform.blockchain.wallet.TransactionPool
import io.waveplatform.server.handlers.p2p.MessageTypes
import io.waveplatform.server.handlers.p2p.P2PPeerClient
import io.waveplatform.server.handlers.p2p.messages.Message
import io.waveplatform.server.handlers.p2p.messages.Packet
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.net.InetSocketAddress
import org.slf4j.Logger
import java.net.URI

class P2PServer (
    val blockChain: BlockChain,
    val transactionPool: TransactionPool,
    val wallet: WalletRecordDTO,
    var nodes : MutableList<WebSocket> = mutableListOf(),
    val peer : LinkedHashSet<String> = linkedSetOf(),
    inetSocketAddress: InetSocketAddress
) : WebSocketServer(inetSocketAddress) {

    fun listen(){
        this.connectToPeers()
    }

    private val log: Logger = LoggerFactory.getLogger(P2PServer::class.java)

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        this.connectNode(conn)
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        this.nodes = this.nodes.filter { con ->
            con !== conn
        } as MutableList<WebSocket>
    }


    override fun onMessage(conn: WebSocket?, message: String) {
        println("onMessageF")
        println(conn?.remoteSocketAddress?.address)
        println(message)
        val mapper = jacksonObjectMapper()
        val message = mapper.readValue(message,Message::class.java)

        println(message)
        when(message.type!!) {
            MessageTypes.SYNC -> handleSync(message.height)
            MessageTypes.BLOCK -> handleReceivedBlock(message.block!!)
            MessageTypes.TRANSACTION -> handleReceivedTransaction(message.transaction!!)
            MessageTypes.FREEZE -> handleReceivedFreeze(message.transaction!!)
            MessageTypes.UNFREEZE -> handleReceivedUnfreeze(message.transaction!!)
            MessageTypes.WALLETTRANSACTIONS -> handleReceivedWalletTransactions(message.transactions!!)
        }

    }

    override fun onError(conn: WebSocket, ex: Exception) {
        ex.printStackTrace()
//        conn.send(ex.message)
    }

    override fun onStart() {
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

    fun handleSync(height : CurrentChain?){
        println("handle sync server p2pserver")
        var chain : CurrentChain = height!!
        var responseChain : CurrentChain? = null
        val chainHeight = blockChain.getHeight()

        println("our db block height is  " + chainHeight)
        println("given height  " + chain.height)
        println("chain state  " + chain.state)

        when(chain.state) {
            ChainMode.INIT -> {
                println("chainMode INIT")
                responseChain = CurrentChain(
                    chainHeight,
                    blockChain.getChainLastBlock(),
                    state = ChainMode.CHECK
                )
            }

            ChainMode.CHECK -> {
                println("chainMode check")
                if (chain.height == chainHeight){
                    println("sending last block with freeze")
                    responseChain = CurrentChain(
                        chainHeight,
                        blockChain.getLastBlock(extra = "FREEZE"),
                        state = ChainMode.FINISHED,
                    )
                    State.synced()
                }
                //if node ur chain is bigger sending his chain next block
                else if (chainHeight > chain.height){
                    println("chain not synced")
                    val newHeight = chain.height + 1
                    var extra =""
                    if (chainHeight >= newHeight) extra = "FREEZE"
                    responseChain = CurrentChain(
                        newHeight,
                        blockChain.getBlockByHeight(newHeight,extra = extra),
                        state = ChainMode.NEXT
                    )
                }
            }

            ChainMode.PREVIOUS -> {
                //todo decrement feature
            }
            ChainMode.FINISHED -> {
                println("finishedP2pServer")
                //my height == responseHeight
            }
        }
        this.nodes.forEach {
            try{
                it.send(
                    Packet.serve(responseChain!!)
                )
            }catch (nlp : NullPointerException){
                //not handled chanMode
            }
        }
    }

    private fun handleReceivedWalletTransactions(transactions : MutableList<Transaction>){
        this.blockChain.account.accountService.executeWalletSyncTransactions(transactions)
    }

    private fun handleReceivedChain(chain: CurrentChain){
        this.blockChain.syncChain(chain)
    }

    //incremental confirmation
    private fun handleReceivedBlock(block: Block){
        println("handleReceivedBlock")
        when(!this.blockChain.isLastBlock(block)) {
            true -> {
                if (this.blockChain.isValidBlock(block)){
                    this.blockChain.addBlock(block)
                    this.broadcastBlock(block)
                    println("transaction pool clearing")
                    this.transactionPool.clear()
                }
            }
            else -> {
                println("incrementconfirmation")
                this.blockChain.incrementConfirmation(block)
            }
        }
    }

    private fun handleReceivedFreeze(transaction: Transaction){
        println("handle rfeceive transaction")
//        if (!this.transactionPool.transactionExists(transaction)){
            val added = this.transactionPool.addFreeze(transaction)
            if (added){
                this.broadcastFreeze(transaction)
            }
//        }
    }

    private fun handleReceivedUnfreeze(transaction: Transaction){
            val added = this.transactionPool.addUnfreeze(transaction)
            if (added){
                this.broadcastUnfreeze(transaction)
            }
    }

    private fun handleReceivedTransaction(transaction: Transaction){
        println("handleReceivedTransaction")
        val block = this.blockChain.createBlock(
            this.transactionPool.unfinishedTransactions()
        )

        if (!this.transactionPool.transactionExists(transaction)){
            println("transactionAddedExists")
            val added = this.transactionPool.addTransaction(transaction)
            val lastTransaction = this.transactionPool.getTransaction(transaction.txId)
            println(added)
            if (added){
                println("transactionAdded")
                this.broadcastTransaction(lastTransaction)
                block.transactions.add(lastTransaction)
            }
        }
        //if leader
        if (this.transactionPool.thresholdReached()){
            println(wallet.pubKey)
            println("threeesholdreached")
            if ( blockChain.poolLeaderExists(wallet.pubKey,block.validator!!)){
                this.blockChain.addBlock(block)
                println("blockadded into chain")
                this.broadcastBlock(block)
            }
        }
    }

    fun broadcastFreeze(transaction: Transaction){
        println("broadcastFreeze")
        this.nodes.forEach {
            it.send(
                Packet.serve(transaction,MessageTypes.FREEZE)
            )
        }
        println("freezebroadcasted")
    }

    fun broadcastUnfreeze(transaction: Transaction){
        println("unfreezing")
        this.nodes.forEach {
            it.send(
                Packet.serve(transaction,MessageTypes.UNFREEZE)
            )
        }
    }

    fun broadcastTransaction(transaction: Transaction){
        println("broadcasting tx")
        this.nodes.forEach {
            this.sendTransaction(it,transaction)
        }
    }

    private fun sendTransaction(conn: WebSocket,transaction: Transaction){
        conn.send(
            Packet.serve(transaction)
        )
    }


    //broadcast modified coin records
    private fun broadcastWallet(transactions : MutableList<Transaction>){
        println("broadcastingWalletData")
        this.nodes.forEach { conn ->
            conn.send(
                Packet.serve(transactions)
            )
        }
    }

    private fun broadcastBlock(block: Block){
        println("broadcastBlock")
        this.nodes.forEach {
            this.sendBlock(it,block)
        }
    }

    private fun sendBlock(conn: WebSocket, block: Block){
        println("sendBlock")
        conn.send(
            Packet.serve(block)
        )
    }

    private fun connectNode(conn: WebSocket){
        this.nodes.add(conn)
        this.sendChain(conn)
    }
    

    private fun connectToPeers(){
        this.peer.forEach {
            val client = P2PPeerClient(
                URI.create(it),
                transactionPool,
                blockChain,
                wallet
            )
            this.nodes.add(client.getWsClient())
            println("adding peers")
        }
    }


    private fun sendChain(conn: WebSocket, state : ChainMode = ChainMode.INIT  ){
        conn.send(
            Packet.serve(
                CurrentChain(this.blockChain.getHeight(),this.blockChain.getChainLastBlock(), state = state)
            )
        )
    }


}