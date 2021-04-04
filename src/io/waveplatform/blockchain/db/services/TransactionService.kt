package io.waveplatform.blockchain.db.services

import io.waveplatform.blockchain.Block
import io.waveplatform.blockchain.db.*
import io.waveplatform.blockchain.db.util.convertDecimal
import io.waveplatform.blockchain.wallet.Transaction
import io.waveplatform.server.requests.WalletResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant

class TransactionService(
    val blockService: BlockService
) {

    private val walletService = WalletService(blockService)

    fun currentSupply() : BigDecimal = transaction {
        val coinGenerations = TransactionRecordRepository.select{
            TransactionRecordRepository.type eq "Coin"
        }.sumOf {
            it[TransactionRecordRepository.amount]
        }


        val coinRewards = TransactionRecordRepository.select{
            TransactionRecordRepository.type eq "reward"
        }.sumOf {
            it[TransactionRecordRepository.amount]
        }
        coinGenerations + coinRewards
    }

    fun getBalance(address : String) : BigDecimal = transaction {
        val wTransactions = getWalletTransactions(address)
        val sumIncoming : BigDecimal = wTransactions["INCOMING"]!!.sumOf {
            it.output!!.amount
        }
        val sumOutgoing : BigDecimal = wTransactions["OUTGOING"]!!.sumOf {
            it.output!!.amount
        }
        sumIncoming - sumOutgoing
    }

    fun finishTransactions(bId: Long) = transaction {
        unfinishedTransactions().forEach { uTransaction ->
            TransactionRecordRepository.update({TransactionRecordRepository.id eq uTransaction.id}) {
                it[finished] = true
                it[blockId] = bId
            }
        }
    }

    /**
     * only for blockchain coin generation transaction
     */
    fun finishTransactions(block:Block) = transaction {
        block.transactions.forEach { transaction ->

            TransactionRecordRepository.insert {
                it[txId] = transaction.txId
                it[finished] = true
                it[to] = transaction.output!!.to
                it[amount] = transaction.output!!.amount
                it[fee] = transaction.output!!.fee
                it[signature] = transaction.input!!.signature
                it[from] = transaction.input!!.from
                it[timestamp] = transaction.input!!.timestamp
                it[type] = transaction.type.type
                it[blockId] = block.id
            }
            val toWallet = walletService.createOrGet(transaction.output!!.to,null);

            when(transaction.type.type){
                "Coin" -> {
                    println(toWallet.pubKey)
                    println("towallet")
                    walletService.increment(toWallet.pubKey,transaction.output!!.amount,false)
                }
            }
        }
    }

    fun makeGenesisFinished(block : Block) = transaction {
        block.transactions.forEach {  transaction ->
            when(transaction.type.type){
                "transaction" -> {
                    println("transactionsss")
                    val toWallet = walletService.createOrGet(transaction.output!!.to,null);
                    val fromWallet = walletService.getWallet(transaction.input!!.from,null)

                    walletService.decrement(fromWallet!!.pubKey, transaction.output!!.amount)
                    walletService.increment(toWallet.pubKey, transaction.output!!.amount)
                    println("incrementDone")
                }

                "validator" -> {
                    val toWallet = walletService.getWallet(transaction.output!!.to,null)!!
                    val fromWallet = walletService.getWallet(transaction.input!!.from,null)!!
                    walletService.setZero(fromWallet.pubKey, transaction.output!!.amount)
                    walletService.incrementPoolBalance(toWallet.pubKey, transaction.output!!.amount)
                    walletService.stakerService.addToStake(
                        fromWallet.pubKey,
                        transaction.output!!.amount,
                        fromWallet.pubKey
                    )
                }
            }
        }
    }

    fun finishTransaction(txId: String,bId : Long) = transaction {
        TransactionRecordRepository.update({TransactionRecordRepository.txId eq txId}) {
            it[finished] = true
            it[blockId] = bId
        }
    }

    fun unfinishedTransactions(extra: String = "" ) : MutableList<Transaction> = transaction {
        var trs : ArrayList<Transaction> = ArrayList()

        val transactions =   TransactionRecordRepository.select {
            TransactionRecordRepository.finished eq false
        }.mapNotNull {
            TransactionRecord.wrapRow(it).convert()
        }.toMutableList()
        trs.addAll(transactions)

        if (extra != ""){
            TransactionRecordRepository.select {
                TransactionRecordRepository.type eq "FREEZE"
            }.orderBy(TransactionRecordRepository.timestamp,SortOrder.DESC).mapNotNull {
                trs.add(
                    TransactionRecord.wrapRow(it).convert()
                )
            }
        }
        trs
    }

    fun getTransaction(transaction: Transaction) : Transaction?  = transaction {
        TransactionRecordRepository.select {
            TransactionRecordRepository.id eq transaction.id
        }.limit(1,0).firstOrNull()?.let {
            TransactionRecord.wrapRow(it).convert()
        }
    }

    fun getTransaction(txId: String) : Transaction? = transaction {
        TransactionRecordRepository.select{
            TransactionRecordRepository.txId eq txId
        }.limit(1,0).firstOrNull()?.let {
            TransactionRecord.wrapRow(it).convert()
        }
    }

    fun walletFreezeExists(address: String) : Boolean = transaction {
        var exists = false
        val trs  = TransactionRecordRepository.select {
            TransactionRecordRepository.from eq address
        }.toList()

        trs.forEach {
            if (it[TransactionRecordRepository.type] == "FREEZE"){
                exists = true
            }
        }
        exists
    }

    fun getStakeRewards(address : String ) : List<Transaction> = transaction {
        TransactionRecordRepository.select {
            TransactionRecordRepository.to eq address
        }.filter {
            it[TransactionRecordRepository.type].equals("fee") || it[TransactionRecordRepository.type].equals("reward")
        }.map { rs ->
            TransactionRecord.wrapRow(rs).convert()
        }
    }

    fun getStakerRewardSum(address: String) : BigDecimal = transaction {
        TransactionRecordRepository.select {
            TransactionRecordRepository.to eq address
        }.filter {
            it[TransactionRecordRepository.type].equals("fee") || it[TransactionRecordRepository.type].equals("reward")
        }.sumOf {
            it[TransactionRecordRepository.amount]
        }
    }

    fun getLastTransaction() : Transaction = transaction {
        TransactionRecordRepository.select {
            TransactionRecordRepository.finished eq false
        }.limit(1,0).orderBy(TransactionRecordRepository.id,SortOrder.DESC)
            .mapNotNull {
                TransactionRecord.wrapRow(it).convert()
            }
            .single()
    }

    fun getLast10Transactions(ofs : Long) : List<Transaction> = transaction {
        TransactionRecordRepository.selectAll()
            .orderBy(TransactionRecordRepository.timestamp,SortOrder.DESC)
            .limit(10,offset = ofs).map {
                TransactionRecord.wrapRow(it).convert()
            }
    }

    fun addFeeTransaction(transaction: Transaction, bId : Long, tos : String) = transaction {
        TransactionRecordRepository.insert {
            it[txId] = transaction.txId
            it[amount] = transaction.output!!.fee
            it[fee] = BigDecimal(0.0)
            it[signature] = transaction.input!!.signature
            it[from] = "Fee-Reward"
            it[timestamp] = transaction.input!!.timestamp
            it[type] = "fee"
            it[finished] = true
            it[to] = tos
            it[blockId] = bId
        }
    }

    fun addRewardTransaction(amnt : BigDecimal,  bId : Long, frm:String, tos : String) = transaction {
        TransactionRecordRepository.insert {
            it[txId] = "reward-transaction-"+bId
            it[amount] = amnt
            it[fee] = convertDecimal(0.0)
            it[signature] = "transactionSignature".toByteArray().toString()
            it[from] = "Block-Reward"
            it[timestamp] = Instant.now().toEpochMilli()
            it[type] = "reward"
            it[finished] = true
            it[to] = tos
            it[blockId] = bId
        }
    }

    fun getFreezers() : List<WalletResponse> = transaction {
        TransactionRecordRepository.select {
            TransactionRecordRepository.type eq "FREEZE"
        }.mapNotNull {
            WalletResponse(
                it[TransactionRecordRepository.from],
                it[TransactionRecordRepository.amount],
                it[TransactionRecordRepository.timestamp]
            )
        }

    }

    fun hasFreeze(address: String) : Boolean = transaction {
        var rsp = false
        val frzs = TransactionRecordRepository.select{
            TransactionRecordRepository.type eq "FREEZE"
        }.toList()
        frzs.forEach {
            if (it[TransactionRecordRepository.from] == address){
                println("iiif")
                rsp = true
            }
        }
        rsp
    }


    fun removeFreeze(transaction: Transaction) :Boolean {
        //clear freeze transaction
        transaction {
            val trs = TransactionRecordRepository.select{
                TransactionRecordRepository.from eq transaction.input!!.from
            }.toList()
            trs.forEach {
                if (it[TransactionRecordRepository.type] == "FREEZE"){
                    TransactionRecordRepository.deleteWhere {
                        TransactionRecordRepository.id eq it[TransactionRecordRepository.id]
                    }
                }
            }
        }
        return walletService.removeFreeze(transaction.input!!.from)
    }

    fun addFreeze(transaction: Transaction) : Boolean = transaction {
        println("Addding freeeze")
        if(!hasFreeze(transaction.input!!.from)){
            walletService.createOrGet(transaction.input!!.from,null).let {
                TransactionRecordRepository.insert {
                    it[txId] = transaction.txId
                    it[amount] = transaction.output!!.amount
                    it[fee] = transaction.output!!.fee
                    it[signature] = transaction.input!!.signature
                    it[from] = transaction.input!!.from
                    it[timestamp] = transaction.input!!.timestamp
                    it[type] = transaction.type.type
                    it[blockId] = 0
                    it[finished] = true
                    it[to] = transaction.output!!.to
                }
                walletService.createFreeze(transaction.input!!.from,transaction.output!!.amount)
            }
            true
        }else{
            false
        }

    }

    fun addTransaction(transaction: Transaction, entityId : Long = 0L) = transaction {
        val lastBlock = blockService.getLastBlock()
        var bId = entityId
        if (lastBlock != null){
            bId = lastBlock.id
        }
        TransactionRecordRepository.insert {
            it[txId] = transaction.txId
            it[amount] = transaction.output!!.amount
            it[fee] = transaction.output!!.fee
            it[signature] = transaction.input!!.signature
            it[from] = transaction.input!!.from
            it[timestamp] = transaction.input!!.timestamp
            it[type] = transaction.type.type
            it[blockId] = bId
            it[finished] = false
            it[to] = transaction.output!!.to
        }
    }

    fun getRawWalletTransactions(address: String) : List<Transaction> = transaction {
        TransactionRecordRepository.select {
            (TransactionRecordRepository.to eq address) or (TransactionRecordRepository.from eq address)
        }.orderBy(TransactionRecordRepository.id,SortOrder.DESC).map{
            TransactionRecord.wrapRow(it).convert()
        }
    }

    fun getWalletTransactions(address: String) : Map<String,List<Transaction>> = transaction {
        val mutableList = mutableMapOf<String,List<Transaction>>()

        val incoming = TransactionRecordRepository.select {
            TransactionRecordRepository.to eq address
        }.orderBy(TransactionRecordRepository.timestamp,SortOrder.DESC).map {
            TransactionRecord.wrapRow(it).convert()
        }

        val outgoing = TransactionRecordRepository.select{
            TransactionRecordRepository.from eq address
        }.orderBy(TransactionRecordRepository.timestamp,SortOrder.DESC).mapNotNull {
            TransactionRecord.wrapRow(it).convert()
        }
        mutableList["INCOMING"] = incoming.toList()
        mutableList["OUTGOING"] = outgoing.toList()
        mutableList
    }

    fun getBlockTransactions(blockId:Long? , extra : String = "") : List<Transaction> = transaction {

        var blockId = blockId
        if (blockId == null){
            blockId = blockService.getLastBlock()!!.id
        }

        var trs : ArrayList<Transaction> = ArrayList()


        val transactions =  TransactionRecordRepository.select {
            TransactionRecordRepository.blockId eq blockId
        }.orderBy(TransactionRecordRepository.timestamp,SortOrder.DESC).mapNotNull {
            TransactionRecord.wrapRow(it).convert()
        }

        trs.addAll(transactions)

        if (extra != ""){
            println("era dont dont is ")
            TransactionRecordRepository.select {
                TransactionRecordRepository.type eq "FREEZE"
            }.orderBy(TransactionRecordRepository.timestamp,SortOrder.DESC).mapNotNull {
                trs.add(
                    TransactionRecord.wrapRow(it).convert()
                )
            }
        }
        trs

    }

    fun getBlockTransactions(blockHash:String) = transaction {
        blockService.getBlock(blockHash)?.let { b ->
            TransactionRecordRepository.select {
                TransactionRecordRepository.blockId eq b.id
            }.map {
                TransactionRecord.wrapRow(it).convert()
            }
        }
        null
    }

    fun getTransactionByTxId(txId: String) = transaction {
        TransactionRecordRepository.select {
            TransactionRecordRepository.txId eq txId
        }.first().let {
            TransactionRecord.wrapRow(it).convert()
        }
    }


}