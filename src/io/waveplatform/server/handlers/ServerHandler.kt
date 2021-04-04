package io.waveplatform.server.handlers

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.waveplatform.blockchain.BlockChain
import io.waveplatform.blockchain.db.WalletRecordDTO
import io.waveplatform.blockchain.db.services.BlockService
import io.waveplatform.blockchain.db.services.WalletService
import io.waveplatform.blockchain.wallet.TransactionPool
import io.waveplatform.server.handlers.p2p.P2PClient
import io.waveplatform.server.respondError


fun Route.serverHandler(blockChain: BlockChain,transactionPool: TransactionPool, wallet: WalletRecordDTO ,blockService: BlockService,walletService : WalletService,p2pClient:P2PClient){


    walletHandler(blockChain,transactionPool,p2pClient,walletService)


    get("/block/{hash}") {
        val hash = call.parameters["hash"]
        if (hash != null) {
            var block = blockChain.blockService.getBlock(hash)
            if (block != null) {
                call.respond(block)
            }else{
                call.respondError("EMPTY_RESPONSE","block not found", HttpStatusCode.NotFound)
            }
        }else{
            call.respondError("REQUEST_ERROR","Hash field is required", HttpStatusCode.BadRequest)
        }
    }

    get("/blockchain/statistics") {
        // max supply = how many coins 175m
        // current_supply = how many coin release
        // holders  = how many holders we have quantity
        // freezers_sum = freezers sum amount
        // wallet_address  = wallet count

        val currentSupply = blockService.transactionService.currentSupply()
        val holders = walletService.holders();
        val freezers = walletService.freezers();
        val addresses = walletService.addresses();

        call.respond(
            mapOf(
               "max_supply" to 175000000.00000000,
               "currency_supply" to currentSupply,
               "holders" to holders,
               "freezers_sum" to freezers,
               "wallet_addresses" to addresses,
            )
        )
    }

    get("/transactions/{txId}") {
        val txId = call.parameters["txId"]
        if (txId != null){
            val transaction = blockChain.blockService.transactionService.getTransaction(txId)
            if (transaction != null){
                call.respond(transaction)
            }else{
                call.respondError("EMPTY_RESPONSE","Transactions Not Found",HttpStatusCode.NotFound)
            }
        }else{
            call.respondError("REQUEST_ERROR","TXID field is required", HttpStatusCode.BadRequest)
        }
    }

    get ("/chain"){
        val page = call.parameters["page"]
        if (page == null){
            call.respond(blockChain.getLastBlocks(0))
        }else{
            call.respond(blockChain.getLastBlocks(page.toLong()))
        }
    }

    get("/freezes") {
        call.respond(transactionPool.getFreezers())
    }

    get ("/validators/{pool}") {
        val pool = call.parameters["pool"]
         call.respond(
             blockChain.stake.stakeService.getPoolUsers(pool!!)
         )
    }

    get ("/validators") {
        call.respond(
            blockChain.stake.stakeService.getValidators()
        )
    }




    get ("/transactions"){
        var page = call.parameters["page"]?.toLong()
        if (page == null){
            page = 0L
        }
        call.respond(blockChain.blockService.transactionService.getLast10Transactions(page))
    }

    get("/transactions/unfinished"){
        call.respond(transactionPool.unfinishedTransactions())
    }

    /**
     * top accounts
     */
    get("/accounts"){
        val accounts = walletService.getWallets()
        call.respond(accounts)
    }

    get("/wallet/{address}"){
        val addr = call.parameters["address"]
        if (addr !=  null) {
            val wl = walletService.getWallet(addr,null)
            if (wl != null){
                val transactions = blockService.transactionService.getWalletTransactions(addr)
                call.respond(
                    mapOf(
                        "wallet" to wl ,
                        "transactions" to transactions
                    )
                )
            }
        }

    }




}
