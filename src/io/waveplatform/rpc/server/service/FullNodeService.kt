package io.waveplatform.rpc.server.service

import io.waveplatform.blockchain.*
import io.waveplatform.blockchain.crypto.Convert
import io.waveplatform.blockchain.crypto.Crypto
import io.waveplatform.blockchain.db.services.StakerService
import io.waveplatform.blockchain.db.services.WalletService
import io.waveplatform.blockchain.db.util.convertDecimal
import io.waveplatform.blockchain.wallet.Transaction
import io.waveplatform.blockchain.wallet.TransactionPool
import io.waveplatform.server.exceptions.InsufficientBalance
import io.waveplatform.server.handlers.p2p.P2PClient
import io.waveplatform.server.requests.CalculateFee
import io.waveplatform.server.requests.TransactionRequestSignable
import org.slf4j.LoggerFactory
import java.lang.NumberFormatException


class FullNodeService(
    val blockChain: BlockChain,
    val transactionPool: TransactionPool,
    val walletService: WalletService,
    val p2pClient : P2PClient

): FullNode {

    val stakeService = StakerService(walletService)


    companion object {
        val log = LoggerFactory.getLogger(FullNodeService::class.java)
    }

    override fun validators(pool : String): ResponseStatus {
        val poolUsers = blockChain.stake.stakeService.getPoolUsers(pool)
        return ResponseStatus().apply {
            response = poolUsers
        }
    }


    override fun getBlockByHash(hash: String): ResponseStatus {
        var block = blockChain.blockService.getBlock(hash)
        if (block != null) {
            return ResponseStatus().apply {
                response = block
            }
        }
        else{
            return ResponseStatus().apply {
                success = false
                response = "block not found"
            }
        }
    }

    override fun getTransactionById(txId: String): ResponseStatus {
        val transaction = blockChain.blockService.transactionService.getTransaction(txId)
        if (transaction != null){
            return ResponseStatus().apply {
                response = transaction
            }
        }else{
            return ResponseStatus().apply {
                success = false
                response = "transaction not found"
            }
        }
    }

    override fun unfinishedTransactions(): ResponseStatus {
        return ResponseStatus().apply {
            response = transactionPool.unfinishedTransactions()
        }
    }

    override fun getWalletByAddressRaw(pubKey: String) : ResponseStatus{
        val wl = walletService.getWallet(pubKey,null)
        if (wl != null){
            val transactions = this.blockChain.blockService.transactionService.getRawWalletTransactions(pubKey)
            return ResponseStatus().apply {
                response = mapOf(
                    "wallet" to wl ,
                    "transactions" to transactions
                )
            }
        }else{
            return ResponseStatus().apply {
                success = false
                response = "wallet not found"
            }
        }
    }

    override fun getWalletByAddress(pubKey: String) : ResponseStatus{
        val wl = walletService.getWallet(pubKey,null)
        if (wl != null){
            val transactions = this.blockChain.blockService.transactionService.getWalletTransactions(pubKey)
            return ResponseStatus().apply {
                response = mapOf(
                    "wallet" to wl ,
                    "transactions" to transactions
                )
            }
        }else{
            return ResponseStatus().apply {
                success = false
                response = "wallet not found"
            }
        }
    }

    override fun signWallet(secret:String) : ResponseStatus {
        validateMnemonic(secret)
        val keyPair = getKeyPair(secret)
        val wlRecord = walletService.logIn(keyPair.first)
        return ResponseStatus().apply {
            response = mapOf(
                "pubKey" to keyPair.first,
                "privKey" to keyPair.second,
                "balance" to wlRecord.balance,
                "blocked" to wlRecord.blocked
            )
        }
    }


    override fun chain(): ResponseStatus {
        return ResponseStatus().apply {
            response = blockChain.getLastBlocks(0)
        }
    }

    override fun freezes(): ResponseStatus {
        return ResponseStatus().apply {
            response = transactionPool.getFreezers()
        }
    }

    override fun sync():SystemState {
        if (State.progress > 0) {
            return SystemState().apply {
                lastAction = State.progress
            }
        }else{

            if(State.isSynced()){
                return SystemState().apply {
                    lastAction = "Node already synced"
                }
            }else{
                log.debug("p2pclient sending sync request")
                p2pClient.syncNode()
                log.debug("sync proccess started")
                return SystemState().apply {
                    lastAction = "sync started "
                }
            }
        }
    }

    override fun createMnemonics():SystemState {
        return SystemState().apply {
            lastAction = mapOf(
                "phrases" to generateMnemonic()
            )
        }
    }

    override fun calculateFee(calculateFee : CalculateFee) : ResponseStatus {
        return ResponseStatus().apply {
            response = mapOf(
                "fee" to Transaction.calculateFee(convertDecimal(calculateFee.amount))
            )
        }
    }

    override fun transact(transactionRequest: TransactionRequestSignable) : ResponseStatus {
        val pubKey = transactionRequest.pubKey
        val privKey = transactionRequest.privKey
        when(State.isSynced()) {
            true -> {
                try{
                    if (!verifyChallenge(Convert.parseHexString(pubKey),Convert.parseHexString(privKey))){
                        return ResponseStatus().apply {
                            success = false
                            response = "Invalid Key/Pair"
                        }
                    }
                    val account = walletService.createOrGet(pubKey,null)

                    account.createTransaction(
                        transactionRequest.to,
                        convertDecimal(transactionRequest.amount),
                        transactionRequest.type,
                        blockChain,
                        transactionPool
                    ).let {
                        p2pClient.broadcastTransaction(it)
                        return ResponseStatus().apply {
                            response = it
                        }
                    }
                }
                catch (ibE:InsufficientBalance){
                    return ResponseStatus().error(ibE.message)
                }
                catch (nmfE : NumberFormatException){
                    return ResponseStatus().error(nmfE.message)
                }
                catch (ttE : TransactionTransferException){
                    return ResponseStatus().error(ttE.message)
                }
            }
            else ->{
                return ResponseStatus().notSynced()
            }
        }

    }


    override fun createPool(poolRequest : CreatePoolRequest): SystemState {
        //validate signature
       //val pool = WalletRecordDTO.createPool()
        val signature = Crypto.sign(poolRequest.pool.toByteArray(),poolRequest.privKey)
        return SystemState().apply {
            lastAction = "poolCreated"
        }
    }

    override fun createWallet() : SystemState {
        val keys = generatePubKeyFromRandomMnemonics()
        val wallet = walletService.create(keys["pubKey"]!!)

        var lastaction : MutableMap<String,String>?

        if (wallet){
            lastaction = keys
        }else{
            lastaction = null
        }
        return SystemState().apply {
            lastAction = lastaction
        }
    }



}