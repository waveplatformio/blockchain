package io.waveplatform.blockchain.db

import io.waveplatform.blockchain.BlockChain
import io.waveplatform.blockchain.Constants
import io.waveplatform.blockchain.TransactionTransferException
import io.waveplatform.blockchain.wallet.Transaction
import io.waveplatform.blockchain.wallet.TransactionPool
import io.waveplatform.server.exceptions.InsufficientBalance
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SizedIterable
import java.math.BigDecimal

/*
* local node record
*/
data class WalletRecordDTO(
    val pubKey:String,
    val signature: String?,
    val balance: BigDecimal,
    val pending:BigDecimal,
    val blocked:BigDecimal,
    val secret: String?,
    var privKey : String = ""
){

    companion object {
        fun createPool(walletRecordDTO: WalletRecordDTO,amount: BigDecimal){
            val stake = Transaction.newStake(walletRecordDTO,Constants.POOL_ADDRESS_RECEIVER,amount )
        }
    }

    fun createStake(pool : String, amount: BigDecimal) : Transaction{
        if (amount > balance){
            throw Exception("not have balance");
        }
        val stake = Transaction.newStake(this,pool,amount)
        return stake
    }


    fun createFreeze(amount: BigDecimal,walletRecord:WalletRecordDTO) : Transaction{
        if (amount > balance){
            throw InsufficientBalance("not have balance");
        }
        return Transaction.newFreeze(amount,walletRecord)
    }

    fun createUnfreeze() : Transaction{
        println("createing unfreeze")
        return Transaction.unfreeze(this)
    }

    /**
     * non-spendable functionalities
     */
    fun createTransaction(to:String, amount: BigDecimal, type: String, blockchain: BlockChain, transactionPool: TransactionPool): Transaction {
        //check addresses not same

        if(to == pubKey){
            throw TransactionTransferException("to and from addresses are same")
        }
        return Transaction.newTransaction(this,to,amount,type,blockchain)
    }

    fun sign(hash : String): ByteArray {
        //need secret or sign with wallet pubkey
        return io.waveplatform.blockchain.sign(hash,pubKey)
    }

}


class WalletRecord(id : EntityID<Long>) : Entity<Long>(id){
    companion object : EntityClass<Long,WalletRecord>(WalletRecordRepository)

    val pubKey by WalletRecordRepository.pubKey
    val signature by WalletRecordRepository.signature
    val balance by WalletRecordRepository.balance
    val pending by WalletRecordRepository.pending
    val blocked by WalletRecordRepository.blocked


    fun convert(secret: String?) : WalletRecordDTO {
        return WalletRecordDTO(
            pubKey,
            signature,
            balance,
            pending,
            blocked,
            secret
        )
    }

    override fun toString(): String = with(StringBuilder()){
        append("WalletRecord(")
        append("balance=${balance}")
        append("pending=${pending}")
        append("blocked=${blocked}")
        append("state=${blocked}")
        append("pubKey=${pubKey}")
        append(")")
    }.toString()

}

object WalletRecordRepository : LongIdTable("wallet_local") {
    val pubKey = varchar("public_key",855).index()
    val signature = varchar("signature",855).nullable()
    val balance = decimal("balance",8,8).default(BigDecimal.valueOf(0.0))
    val pending = decimal("pending",8,8).default(BigDecimal.valueOf(0.0))
    val blocked = decimal("blocked",8,8).default(BigDecimal.valueOf(0.0))

}
