package io.waveplatform.blockchain

import io.waveplatform.blockchain.crypto.Convert
import io.waveplatform.blockchain.crypto.Crypto
import io.waveplatform.blockchain.db.WalletRecordDTO
import io.waveplatform.blockchain.db.services.BlockService
import io.waveplatform.blockchain.db.services.WalletService
import io.waveplatform.blockchain.db.util.convertDecimal
import io.waveplatform.blockchain.wallet.Transaction
import java.math.BigDecimal

class Account(
    val blockService : BlockService,
    val wallet:WalletRecordDTO
)  {

    val accountService = WalletService(blockService)

    /**
     * transfer given amount to another address
     */
    fun transfer(from:String,to:String,amount: BigDecimal){
        try {
            //double spending check here
            this.decrement(from,amount)
            this.increment(to,amount)
        }catch (e : RecordInitializationNotExecutedException){
            throw TransactionTransferException("cannot make transfer reason : " + e.message)
        }
    }

    fun increment(to:String, amount:BigDecimal,pending:Boolean=false){
        this.accountService.increment(to,amount,pnd=pending)
    }

    fun invalidValidatorIncrement(from:String,amount: BigDecimal){
        this.accountService.resolveInvalidValidator(from,amount)
    }

    fun incrementPooler(to:String, amount:BigDecimal){
        this.accountService.incrementPoolBalance(to,amount)
    }
    fun decrementPooler(to: String,amount: BigDecimal){
        this.accountService.decrementPoolBalance(to,amount);
    }

    fun decrement(from: String, amount: BigDecimal){
        this.accountService.decrement(from,amount)
    }

    /**
     * executes transactions and income receive transactions
     */
    fun update(transaction: Transaction){
        val amount = transaction.output!!.amount
        val from = transaction.input!!.from
        val to = transaction.output!!.to
        //create wallets if not exists
        this.transfer(from,to,amount)
    }

    fun invalidStakeUppdate(transaction: Transaction){
        val amount = transaction.output!!.amount
        val from = transaction.input!!.from
        this.invalidValidatorIncrement(from,amount)
    }



    fun updateStake(transaction: Transaction){
        val amount = transaction.output!!.amount
        val from = transaction.input!!.from
        val to = transaction.output!!.to
        try {
            //double spending check here
            this.decrement(from,amount)
            this.incrementPooler(to,amount)
        }catch (e : RecordInitializationNotExecutedException){
            throw TransactionTransferException("cannot make transfer ")
        }
    }

    //update restake feture
    fun updateReStake(transaction: Transaction){
        val amount = transaction.output!!.amount
        val from = transaction.input!!.from
        val to = transaction.output!!.to
        try {

            this.increment(from,amount,pending=true)
            this.decrementPooler(to,amount)
        }catch (e : RecordInitializationNotExecutedException){
            throw TransactionTransferException("cannot make transfer ")
        }
    }


    /**
     * Send transaction fee to leader validator
     *
     * @param block
     * @param transaction
     */
    fun transferFee(block: Block, transaction: Transaction){
        val amount = transaction.output!!.fee
        val from =  transaction.input!!.from
        val to = block.validator!!

        val digest = Crypto.sha256()
        digest.update(transaction.input!!.timestamp.toByte())
        digest.update(transaction.bytes())
        transaction.txId = Convert.toHexString(digest.digest())

        this.blockService.transactionService.addFeeTransaction(transaction,block.id,to)
        this.transfer(from,to, amount)
    }

    fun giveReward(block: Block){
        val amount = convertDecimal(block.blockReward.toDouble())
        val from = block.validator!!
        val to = block.validator
        this.blockService.transactionService.addRewardTransaction(amount,block.id,from,to)
        this.increment(to,amount)
    }


    fun getBalance(address: String): BigDecimal {
        return this.accountService.getWallet(address,null)!!.balance
    }

    /**
     * non-spendable functionalities
     */
    fun getNonSpendableBalance(address: String) : BigDecimal {
        var sum = BigDecimal(0.0)
        this.blockService.transactionService.unfinishedTransactions().forEach {
            when(it.input!!.from) {
                address -> sum += it.output!!.amount
            }
        }
        return sum
    }
}
