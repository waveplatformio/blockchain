package io.waveplatform.blockchain.wallet

import io.waveplatform.blockchain.Constants
import io.waveplatform.blockchain.db.services.BlockService
import io.waveplatform.blockchain.db.services.TransactionService
import io.waveplatform.blockchain.db.services.WalletService
import io.waveplatform.server.requests.WalletResponse
import java.math.BigDecimal

class TransactionPool{

    val blockService = BlockService()
    private val transactionService: TransactionService = blockService.transactionService

    fun addTransaction(transaction: Transaction): Boolean {
        if (Transaction.isValid(transaction)){
            this.transactionService.addTransaction(transaction)
            return true
        }
        return false
    }


    fun addFreeze(transaction: Transaction) : Boolean {
        if (Transaction.isValid(transaction)){
            if (this.transactionService.addFreeze(transaction)){
                return true
            }
            return  false
        }
        return false
    }

    fun hasFreeze(address: String): Boolean{
        return this.transactionService.hasFreeze(address);
    }

    fun addUnfreeze(transaction: Transaction) : Boolean {
        if (Transaction.isValid(transaction)){
            if (this.transactionService.removeFreeze(transaction)){
                return true
            }
            return  false
        }
        return false
    }

    fun getFreezers() : List<WalletResponse> {
        return this.transactionService.getFreezers()
    }

    fun getStakeRewards(address: String) : List<Transaction>{
        return this.transactionService.getStakeRewards(address)
    }

    fun getStakeRewardSum(address: String) : BigDecimal {
        return this.transactionService.getStakerRewardSum(address)
    }

    fun getTransaction(txId : String): Transaction {
        return this.transactionService.getTransaction(txId)!!
    }

    fun unfinishedTransactions(): MutableList<Transaction>{
       return this.transactionService.unfinishedTransactions()
    }

    fun hasValidTransactions(blockId : Long?) : Boolean {
        this.transactionService.getBlockTransactions(blockId).forEach {
           return !Transaction.isValid(it)
        }
        return true
    }

    fun transactionExists(transaction: Transaction): Boolean {
        return this.transactionService.getTransaction(transaction.txId) != null
    }


    fun clear(){
        //do nothings
    }

    fun thresholdReached(): Boolean {
        return this.transactionService.unfinishedTransactions().filter {
            it.type.type == "transaction"
        }.size >= Constants.TRANSACTIONS_PER_BLOCK
    }
}