package io.waveplatform.blockchain.db.services

import io.waveplatform.blockchain.Block
import io.waveplatform.blockchain.crypto.Convert
import io.waveplatform.blockchain.db.BlockRecord
import io.waveplatform.blockchain.db.BlockRecordRepository
import io.waveplatform.blockchain.wallet.Transaction
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class BlockService {

    val transactionService = TransactionService(this)

    val size : Long = transaction {
        BlockRecordRepository.selectAll().count()
    }

    fun createBlock(block: Block, tp :String = "Block", f : Boolean = false) = transaction {
        val chkBlock = getLastBlock()
        if (chkBlock == null){
            BlockRecordRepository.insert {
                it[previousHash] = block.previousHash
                it[timestamp] = block.timestamp
                it[validator] = block.validator ?: ""
                it[hash] = block.hash!!
                it[type] = tp
                it[finished] =  f
            }
        }
    }

    fun finishBlock(block: Block) = transaction {
        BlockRecordRepository.update({BlockRecordRepository.id eq block.id}){
            it[finished] = true
            it[signature] = Convert.toHexString(block.signature)
            it[confirmations] = block.confirmations
        }
        println("finishBlock "+block.id)
        transactionService.finishTransactions(block.id)
    }

    fun incrementConfirmation(block: Block) = transaction {
        BlockRecordRepository.update({BlockRecordRepository.id eq block.id}){
            it[confirmations] = block.confirmations + 1
        }
    }

    fun getBlock(hash : String) : Block? = transaction {
        BlockRecordRepository.select {
            BlockRecordRepository.hash eq hash
        }.limit(1,0).firstOrNull()?.let {
           bindTransactions(it)
        }
    }

    fun getChainLastBlock() : Block = transaction {
        BlockRecordRepository.select {
            BlockRecordRepository.finished eq true
        }.orderBy(BlockRecordRepository.id , SortOrder.DESC).firstOrNull()!!.let {
            bindTransactions(it)
        }
    }


    fun getLastBlock(extra : String = "") : Block? = transaction {
       BlockRecordRepository.select {
            BlockRecordRepository.finished eq false
       }.limit(1,0).firstOrNull()?.let {
           println("getting last block")
           bindTransactions(it,false,extra=extra)
       }
    }


    fun getLastBlocks(offs : Long = 0) : MutableList<Block> = transaction {
        BlockRecordRepository.selectAll()
            .orderBy(BlockRecordRepository.timestamp, SortOrder.DESC)
            .limit(20,offset = offs).map {
                bindTransactions(it,true)
            }.toMutableList()
    }

    fun getHeight() : Long = transaction {
        try{
            BlockRecordRepository.selectAll().last().let {
                it[BlockRecordRepository.id].value
            }
        }catch (ns:NoSuchElementException){
            0L
        }
    }

    fun getBlockByHeight(id : Long, extra : String = "") : Block = transaction {
        BlockRecordRepository.select {
            BlockRecordRepository.id eq id
        }.last().let {
            bindTransactions(it,true, extra = extra)
        }
    }

    fun createGenesis(block: Block) : Block {
        var genesis = getGenesis()
        if (genesis == null){
            this.createBlock(block,  "GENESIS", true)
            genesis = getGenesis()!!
            runBlocking {
                transactionService.finishTransactions(block)
                transactionService.makeGenesisFinished(block)
            }
        }
        return genesis
    }

    private fun bindTransactions(rs:ResultRow, finished : Boolean = true, extra : String = ""): Block {
        println("binding transactions")
        var transactions:MutableList<Transaction>
        if (!finished){
            transactions = transactionService.unfinishedTransactions( extra = extra)
        }else{
            transactions = transactionService.getBlockTransactions(rs[BlockRecordRepository.id].value, extra = extra).toMutableList()
        }

        return BlockRecord.wrapRow(rs).convert(transactions)
    }

    private fun getGenesis() : Block? = transaction {
        BlockRecordRepository.select{
            BlockRecordRepository.type eq  "GENESIS"
        }.limit(1,0).firstOrNull()?.let {
            BlockRecord.wrapRow(it).convert(mutableListOf())
        }
    }
}