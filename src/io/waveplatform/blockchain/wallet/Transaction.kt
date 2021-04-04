package io.waveplatform.blockchain.wallet

import io.waveplatform.blockchain.BlockChain
import io.waveplatform.blockchain.Constants
import io.waveplatform.blockchain.crypto.Convert
import io.waveplatform.blockchain.crypto.Crypto
import io.waveplatform.blockchain.db.WalletRecordDTO
import io.waveplatform.blockchain.db.util.convertDecimal
import io.waveplatform.blockchain.hash
import io.waveplatform.server.exceptions.InsufficientBalance
import io.waveplatform.server.handlers.p2p.MessageTypes
import java.lang.RuntimeException
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.util.*



data class TransactionType(
    val type: String
)

data class TransactionOutput(
    val to: String,
    val amount: BigDecimal,
    val fee : BigDecimal = Constants.TRANSACTION_FEE
)

data class TransactionInput(
    val signature: String,
    val from: String,
    val timestamp: Long = Instant.now().toEpochMilli()
)

class Transaction(
    var id : Long = 0,
    var type : TransactionType,
    var input : TransactionInput?,
    var output : TransactionOutput?,
    var txId : String = UUID.randomUUID().toString(),
){

    private var bytes : ByteArray? = null

    fun bytes(): ByteArray {
        if(bytes == null){
            try {
                val buffer = ByteBuffer.allocate(getSize())
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                buffer.put(type.type.toByteArray())
                buffer.putInt(input!!.timestamp.toInt())
                buffer.put(input!!.from.toByteArray())
                buffer.put(input!!.signature.toByteArray())
                bytes = buffer.array()

            }catch (rE : RuntimeException){
                rE.printStackTrace()
            }
        }
        return bytes!!
    }

    private fun getSize(): Int {
        return signatureOffset() + 64
    }

    private fun signatureOffset(): Int {
        return 1 + 1 + 4 + 2 + 32 + 8 + 4 + 4 + 8
    }

    companion object {


        fun newStake(stakerWallet: WalletRecordDTO, to: String,amount: BigDecimal) : Transaction{
            return generateTransaction(stakerWallet,to,amount,"stake")
        }

        fun unfreeze(senderWallet: WalletRecordDTO) : Transaction{
            val unfreeze = Transaction(
                type = TransactionType(MessageTypes.UNFREEZE.name),
                output = TransactionOutput(senderWallet.pubKey, BigDecimal(0.0)),
                input = null
            )

            unfreeze.input = TransactionInput(
                signature = senderWallet.sign("${unfreeze.output}".hash()).toString(),
                from = senderWallet.pubKey
            )
            return unfreeze
        }

        fun newFreeze(amount:BigDecimal,senderWallet: WalletRecordDTO) : Transaction{
            if (amount > senderWallet.balance){
                throw InsufficientBalance("doens't have amount")
            }

            val freez = Transaction(
                type = TransactionType(MessageTypes.FREEZE.name),
                output = TransactionOutput(senderWallet.pubKey,amount),
                input = null
            )

            freez.input = TransactionInput(
                signature = senderWallet.sign("${freez.output}".hash()).toString(),
                from = senderWallet.pubKey
            )
            val digest = Crypto.sha256()
            val timestamp = Instant.now().toEpochMilli()
            digest.update(timestamp.toByte())
            digest.update(freez.bytes())
            freez.txId = Convert.toHexString(digest.digest())
            return freez

        }


        fun newTransaction(senderWallet: WalletRecordDTO, to: String, amount: BigDecimal, type: String, blockchain:BlockChain): Transaction {

            when(type) {
                "transaction", -> {
                    if ( (amount + calculateFee(amount))  > senderWallet.balance ){
//                    if ( (amount +  blockchain.getNonSpendableBalance(senderWallet.pubKey) + calculateFee(amount))  > senderWallet.balance ){
                        throw InsufficientBalance("doesn't have amount")
                    }
                }
                "validator" -> {
                    if(convertDecimal(Constants.MIN_VALIDATOR_AMOUNT) > amount){
                        throw InsufficientBalance("The minimum stake amount is 10,000 Wave")
                    }
                    if ( (amount +  blockchain.getNonSpendableBalance(senderWallet.pubKey) + calculateFee(amount))  > senderWallet.balance ){
                        throw InsufficientBalance("doesn't have amount")
                    }
                }
                "stake" -> {
                    if(convertDecimal(Constants.MIN_STAKER_AMOUNT) > amount){
                        throw InsufficientBalance("The minimum stake amount is 100,000 Wave")
                    }
                }
                "unstake"->{
                   try{
                       blockchain.getPoolUser(senderWallet.pubKey).let { poolValidator ->
                           if ((amount + blockchain.getNonSpendableBalance(senderWallet.pubKey)) > poolValidator.amount){
                               throw InsufficientBalance("You don't have enough staked amount")
                           }
                       }
                   }catch(np : NullPointerException){
                       throw InsufficientBalance("account not have any validator account")
                   }

                }
            }

            return generateTransaction(senderWallet, to ,amount,type)
        }

        private fun generateTransaction(senderWallet: WalletRecordDTO, to: String, amount: BigDecimal, type: String): Transaction{
            val transaction = Transaction(
                type = TransactionType(type),
                output = TransactionOutput(to , amount, calculateFee(amount)),
                input = null
            )
            return signTransaction(transaction,senderWallet)
        }

        /**
         * transaction hash txId mod fun @todo rep 1
         */
        private fun signTransaction(transaction: Transaction, senderWallet: WalletRecordDTO): Transaction {
            transaction.input = TransactionInput(
                signature = senderWallet.sign("${transaction.output}".hash()).toString(),
                from =  senderWallet.pubKey,
            )
            val digest = Crypto.sha256()
            val timestamp = Instant.now().toEpochMilli()
            digest.update(timestamp.toByte())
            digest.update(transaction.bytes())
            transaction.txId = Convert.toHexString(digest.digest())
            return transaction
        }


        /**
         * calculate fee
         */
        fun calculateFee(amount : BigDecimal) : BigDecimal {
            val bt = amount.toInt()
            if (bt == 0){
                return Constants.TRANSACTION_FEE
            }
            return convertDecimal(
                (( convertDecimal(bt.toDouble()) * Constants.TRANSACTION_FEE_PER_BYTE ) + Constants.TRANSACTION_FEE).toDouble()
            )

        }

        fun isValid(transaction: Transaction) : Boolean{
            return transaction.verifyTransaction()
        }

    }

    fun verifyTransaction(): Boolean{
        return true
    }

}


