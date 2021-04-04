package io.waveplatform.server.handlers

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.waveplatform.blockchain.*
import io.waveplatform.blockchain.crypto.Convert
import io.waveplatform.blockchain.db.WalletRecordDTO
import io.waveplatform.blockchain.db.services.StakerService
import io.waveplatform.blockchain.db.services.WalletService
import io.waveplatform.blockchain.db.util.convertDecimal
import io.waveplatform.blockchain.wallet.Transaction
import io.waveplatform.blockchain.wallet.TransactionPool
import io.waveplatform.server.exceptions.AccountNotFoundException
import io.waveplatform.server.handlers.p2p.P2PClient
import io.waveplatform.server.receiveJson
import io.waveplatform.server.requests.*
import io.waveplatform.server.respondError
import java.math.BigDecimal
import java.time.Instant


fun Route.walletHandler(blockChain: BlockChain,transactionPool: TransactionPool,p2pClient: P2PClient,walletService : WalletService){
    var account : WalletRecordDTO?

    val stakeService = StakerService(walletService)

    route("/wallet"){

        post ("/sign-in") {
            try{
                val signRequest = call.receiveJson<WalletLoginRequest>()
                validateMnemonic(signRequest.secret)
                account = walletService.logIn(signRequest)
                account!!.privKey = getKeyPair(account!!.secret!!).second
                call.sessions.set(account)
                call.respond(account!!)

            }catch (m : InvalidMnemonicException){
                call.respond("illegal account")
            }
        }

        post("/stake") {
            val account  = (call.sessions.get("account") as WalletRecordDTO?)!!
            val stakeReq = call.receiveJson<CreateStakeRequest>()
            account.createStake(stakeReq.pool, convertDecimal(stakeReq.amount)).let {
                p2pClient.broadcastTransaction(it)
            }
        }

        post("/stake-rewards") {
            val account  = (call.sessions.get("account") as WalletRecordDTO?)!!
            call.respond(
                mapOf(
                "rewards" to transactionPool.getStakeRewards(account.pubKey),
                "rewardSum" to transactionPool.getStakeRewardSum(account.pubKey)
                )
            )
        }

        post("/transact/calculate"){
            val calculateReq = call.receiveJson<CalculateFee>()
            call.respond( mapOf(
                "fee" to Transaction.calculateFee(convertDecimal(calculateReq.amount))
            ))
        }

        post("/transact"){
            when(State.isSynced()) {
                false -> {
                    call.respondError("NODE_SYNC_ERROR","Node Not Synced",null)
                }
                else -> {
                    val transactionReq = call.receiveJson<TransactionRequest>()

                    if(transactionReq.amount <= 0.16){
                        call.respondError("INVALID_DATA","amount must be greater than 0.16",null)
                    }

                    else if(transactionReq.to == "" && transactionReq.to.length != 64){
                        call.respondError("INVALID_DATA","Invalid To address",null)

                    }

                    else if(call.sessions.get("account") == null){
                        throw AccountNotFoundException("account is null")
                    }else{

                        var account  = (call.sessions.get("account") as WalletRecordDTO?)!!
                        account = walletService.createOrGet(account.pubKey,null)
                        try{
                            Convert.parseHexString(transactionReq.to)
                            account.createTransaction(
                                transactionReq.to,
                                convertDecimal(transactionReq.amount),
                                transactionReq.type,
                                blockChain,
                                transactionPool
                            ).let {
                                if (transactionReq.type != "unstake"){
                                    walletService.transactionCreated(account.pubKey,it.output!!.to,it.output!!.amount , it.output!!.fee)
                                }
                                p2pClient.broadcastTransaction(it)
                                call.respond(it)
                            }
                        }catch (nmFrmE : NumberFormatException ){
                            call.respondError("INVALID_DATA","TO addr error invalid hexstring",null)
                        }catch (ttE : TransactionTransferException){
                            call.respondError("INVALID_DATA",ttE.message,null)
                        }
                    }

                }
            }
        }

        post("/freeze") {
            val transactionReq = call.receiveJson<FreezeRequest>()
            if(call.sessions.get("account") == null){
                throw AccountNotFoundException("account is null")
            }
            var account  = (call.sessions.get("account") as WalletRecordDTO?)!!
            account = walletService.createOrGet(account.pubKey,null)

            if (transactionPool.hasFreeze(account.pubKey)){
                call.respondError("INVALID_DATA","wallet almost have freeze",null)
            }else{
                account.createFreeze(convertDecimal(transactionReq.amount),account).let {
                    p2pClient.broadcastFreeze(it)
                }
                call.respond("ok")
            }
        }

        post("/my-freeze") {
            if(call.sessions.get("account") == null){
                throw AccountNotFoundException("account is null")
            }
            val account  = (call.sessions.get("account") as WalletRecordDTO?)!!
            call.respond(mapOf("freezed" to account.blocked, "timestamp" to Instant.now().toEpochMilli() ))
        }


        post("/unfreeze"){
            if(call.sessions.get("account") == null){
                throw AccountNotFoundException("account is null")
            }
            val account  = (call.sessions.get("account") as WalletRecordDTO?)!!
            account.createUnfreeze().let {
                p2pClient.broadcastUnfreeze(it)
            }
            call.respond("ok")
        }

        post ("/create") {
            val mnemonics = generateMnemonic()
            val keys = getKeyPair( mnemonics )
            val wallet = walletService.create(keys.first)
            if (wallet){
                call.respond(
                    mapOf(
                        "pubKey" to keys.first,
                        "privKey" to keys.second,
                        "phrases" to mnemonics
                    )
                )
            }else{
                call.respond("cannot create")
            }
        }

        get("/public-key") {
            val account  = (call.sessions.get("account") as WalletRecordDTO?)!!
            call.respond(account.pubKey)
        }

    }









}
