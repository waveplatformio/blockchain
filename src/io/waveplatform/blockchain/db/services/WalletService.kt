package io.waveplatform.blockchain.db.services

import io.waveplatform.blockchain.RecordInitializationNotExecutedException
import io.waveplatform.blockchain.db.*
import io.waveplatform.blockchain.db.util.convertDecimal
import io.waveplatform.blockchain.getPublicKey
import io.waveplatform.blockchain.wallet.Transaction
import io.waveplatform.server.exceptions.InsufficientBalance
import io.waveplatform.server.requests.WalletLoginRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal


class WalletService(
    val blockService: BlockService
) {

    val stakerService = StakerService(this)

    fun holders() : Long = transaction {
        WalletRecordRepository.select {
            WalletRecordRepository.balance greater convertDecimal(0.0)
        }.count()
    }

    fun freezers() : BigDecimal = transaction {
        WalletRecordRepository.select {
            WalletRecordRepository.blocked greater convertDecimal(0.0)
        }.sumOf {
            it[WalletRecordRepository.blocked]
        }
    }

    fun addresses() : Long = transaction {
        WalletRecord.all().count()
    }


    fun logIn(pubKey: String): WalletRecordDTO{
        return this.syncWallet(pubKey,null)
    }

    fun create(pKey: String) : Boolean{
        if (getWallet(pKey,null) == null){
            this.syncWallet(pKey,null)
            this.createOrGet(pKey,null)
            return true
        }
        return false
    }

    fun logIn(signRequest : WalletLoginRequest) : WalletRecordDTO{
        val pubKey = getPublicKey(signRequest.secret)
        return this.syncWallet(pubKey,signRequest.secret)
    }

    fun transactionCreated(address: String, to:String ,amount: BigDecimal,fee:BigDecimal) = transaction {
        getWallet(address,null).let { walletRecord ->
            if (walletRecord == null){
                throw InsufficientBalance("not have balance")
            }else{
                WalletRecordRepository.update({ WalletRecordRepository.pubKey eq address }){
                    it[balance] = walletRecord.balance - (amount+fee)
                    it[pending] = walletRecord.pending + (amount+fee)
                }
            }
        }
    }

    fun increment(address : String , amount : BigDecimal, pnd:Boolean = false) = transaction {
        createOrGet(address,null).let { walletRecord ->
            WalletRecordRepository.update({ WalletRecordRepository.pubKey eq address }){
                it[balance] = walletRecord.balance + amount
//                if (pnd){
//                    it[pending] = walletRecord.pending - amount
//                }
            }
        }
    }

    fun resolveInvalidValidator(address : String , amount : BigDecimal){
        getWallet(address,null)?.let { walletRecord ->
            if (walletRecord.balance < amount) throw RecordInitializationNotExecutedException("cannot done decrement")
            transaction {
                WalletRecordRepository.update({ WalletRecordRepository.pubKey eq address }){
                    if (walletRecord.pending >= amount + Transaction.calculateFee(amount)){
                        it[pending] = walletRecord.pending - (amount + Transaction.calculateFee(amount))
                    }else{
                        it[balance] = walletRecord.balance - (amount + Transaction.calculateFee(amount))
                    }
                }
            }
        }
    }

    fun setZero(address: String,amount:BigDecimal){
        getWallet(address,null)!!.let {
            WalletRecordRepository.update({ WalletRecordRepository.pubKey eq address }){
                it[balance] = convertDecimal(0.0)
                it[blocked] = amount
            }
        }
    }
    fun decrement(address : String , amnt : BigDecimal) = transaction {
        getWallet(address,null)?.let { walletRecord ->

            if(walletRecord.pending < amnt){
                if (walletRecord.balance < amnt) throw RecordInitializationNotExecutedException("cannot done decrement to :" + walletRecord.pubKey + " wallet has :"+walletRecord.balance)
                WalletRecordRepository.update({ WalletRecordRepository.pubKey eq address }){
                    it[balance] = walletRecord.balance - amnt
                    it[pending] = BigDecimal(0)
                }
            }else{
                if (walletRecord.pending < amnt) throw RecordInitializationNotExecutedException("cannot done decrement to :" + walletRecord.pubKey + " wallet has :"+walletRecord.balance)
                WalletRecordRepository.update({ WalletRecordRepository.pubKey eq address }){
//                it[pending] = walletRecord.pending - amnt
                    it[pending] = walletRecord.pending - amnt
                }
            }
        }
    }

    //todo here we need stake provement
    fun incrementPoolBalance(address : String , amount : BigDecimal) = transaction {
        createOrGet(address,null).let { walletRecord ->
            WalletRecordRepository.update({ WalletRecordRepository.pubKey eq address }){
                it[blocked] = walletRecord.blocked + amount
            }
        }
    }

    fun decrementPoolBalance(address: String,amount: BigDecimal) = transaction {
        createOrGet(address,null).let { walletRecord ->
            //todo  pooler check validators before make transaction or spent
            if(walletRecord.blocked < amount ){
                WalletRecordRepository.update({ WalletRecordRepository.pubKey eq address }){
                    it[blocked] = walletRecord.blocked - amount
                }
            }else{
                WalletRecordRepository.update({ WalletRecordRepository.pubKey eq address }){
                    it[blocked] = walletRecord.blocked - amount
                }
            }
        }
    }

    fun executeWalletFreezeTransaction(transaction :Transaction) {
        println("executeWalletFreezeTransactions")
        val from = createOrGet(transaction.input!!.from,null)
        transaction {
            when(transaction.type.type) {
                "FREEZE" -> {
                    WalletRecordRepository.update({WalletRecordRepository.pubKey eq from.pubKey}) {
                        it[balance] = from.balance - transaction.output!!.amount
                    }
                    WalletRecordRepository.update({WalletRecordRepository.pubKey eq from.pubKey}) {
                        it[blocked] = transaction.output!!.amount
                    }
                }
                else -> {}
            }
        }

    }


    fun executeWalletFreezeTransactions(transactions : MutableList<Transaction>) {
        println("executeWalletFreezeTransactions")
        transactions.forEach { transaction ->
            val from = createOrGet(transaction.input!!.from,null)
            transaction {
                when(transaction.type.type) {
                    "FREEZE" -> {
                        println(from.blocked == convertDecimal(0.0))
                        println(convertDecimal(0.0))
                        println(from.blocked)
                        println("blocked ===")
                        if (from.blocked == convertDecimal(0.0)){
                            WalletRecordRepository.update({WalletRecordRepository.pubKey eq from.pubKey}) {
                                it[balance] = from.balance - transaction.output!!.amount
                            }
                            WalletRecordRepository.update({WalletRecordRepository.pubKey eq from.pubKey}) {
                                it[blocked] = transaction.output!!.amount
                            }
                        }
                    }
                }
            }
        }

    }

    fun executeWalletSyncSpendableTransactions(transactions: MutableList<Transaction>){
        println("executeWalletSyncSpendableTransactions")
        transactions.forEach { transaction ->
            println(transaction.type.type)
            val from = createOrGet(transaction.input!!.from,null)
            transaction {
                when(transaction.type.type){
                    "FREEZE" -> {

                    }
                    "reward" -> {

                    }
                    "unstake" -> {

                    }
                    else -> {
                        WalletRecordRepository.update({WalletRecordRepository.pubKey eq from.pubKey}) {
                            it[balance] = from.balance - transaction.output!!.amount
                        }
                        WalletRecordRepository.update({WalletRecordRepository.pubKey eq from.pubKey}) {
                            it[pending] = from.pending + transaction.output!!.amount
                        }
                    }
                }
            }
        }
    }

    fun executeWalletSyncTransactions(transactions : MutableList<Transaction>){
        println("executeWalletSyncTransactions")
        transactions.forEach { transaction ->
            println(transaction.type.type)
            val from = createOrGet(transaction.input!!.from,null)
            val to = createOrGet(transaction.output!!.to,null)
            transaction {
                when(transaction.type.type) {
                    "FREEZE" -> {

                    }
                    "reward" -> {
                        WalletRecordRepository.update({WalletRecordRepository.pubKey eq to.pubKey}) {
                            it[balance] = to.balance + transaction.output!!.amount
                        }
                    }
                    "unstake" -> {
                        stakerService.getPool(to.pubKey).let { stake ->

                            if (stake == null){
                                resolveInvalidValidator(from.pubKey,transaction.output!!.amount)
                            }else{
                                WalletRecordRepository.update({WalletRecordRepository.pubKey eq from.pubKey}) {
                                    it[balance] = from.balance +  transaction.output!!.amount
                                }
                                if(to.balance < transaction.output!!.amount) {
                                    WalletRecordRepository.update({WalletRecordRepository.pubKey eq to.pubKey}) {
                                        it[balance] = to.balance - transaction.output!!.amount
                                    }
                                }else{
                                    WalletRecordRepository.update({WalletRecordRepository.pubKey eq to.pubKey}) {
                                        it[balance] = BigDecimal(0)
                                    }
                                }
                                stakerService.decrementToStake(to.pubKey,transaction.output!!.amount, from.pubKey )
                            }
                        }
                    }
                    "fee" -> {
                        //now fee doesn't have from now need it decrement when creatint
//                        WalletRecordRepository.update({WalletRecordRepository.pubKey eq from.pubKey}) {
//                            it[balance] = from.balance - transaction.output!!.amount
//                        }
                        WalletRecordRepository.update({WalletRecordRepository.pubKey eq to.pubKey}) {
                            it[balance] = to.balance + transaction.output!!.amount
                        }
                    }
                    "transaction" -> {
                        WalletRecordRepository.update({WalletRecordRepository.pubKey eq from.pubKey}) {
                            it[balance] = from.balance - (transaction.output!!.amount + Transaction.calculateFee(transaction.output!!.amount))
                        }
                        WalletRecordRepository.update({WalletRecordRepository.pubKey eq to.pubKey}) {
                            it[balance] = to.balance + transaction.output!!.amount
                        }
                    }
                    "validator" -> {
                        //if pool doesn't exists that's amount returned to sender
                        stakerService.getPool(to.pubKey).let { stake ->
                            if (stake ==null){
                                transaction {
                                    WalletRecordRepository.update({ WalletRecordRepository.pubKey eq from.pubKey }){
                                        it[balance] = from.balance - (transaction.output!!.amount + Transaction.calculateFee(transaction.output!!.amount))
                                    }
                                }
                            }else{
                                WalletRecordRepository.update({WalletRecordRepository.pubKey eq from.pubKey}) {
                                    it[balance] = from.balance - (transaction.output!!.amount)
                                }
                                WalletRecordRepository.update({WalletRecordRepository.pubKey eq to.pubKey}) {
                                      it[balance] = to.balance + transaction.output!!.amount
                                }
                                stakerService.addToStake(to.pubKey,transaction.output!!.amount, from.pubKey )
                            }
                        }
                    }
                }
            }
        }
    }

    fun createOrGet(pubKey : String, secret: String?) : WalletRecordDTO{
        var wallet = getWallet(pubKey,secret)
        if (wallet == null) createWallet(pubKey,secret)
        return getWallet(pubKey,secret)!!
    }

    private fun syncWallet(pubKey: String,secret:String?): WalletRecordDTO{
        var wallet = createOrGet(pubKey,secret)


        return wallet
    }

    fun createFreeze(pubKey: String,amount: BigDecimal) = transaction{
        println("creating freeze")
        createOrGet(pubKey,null).let { record ->
            if (record.balance < amount){
                throw InsufficientBalance("not have balance")
            }
            WalletRecordRepository.update({WalletRecordRepository.pubKey eq pubKey}){
                it[blocked] = amount
                it[balance] = record.balance - amount
            }
        }
    }

    fun removeFreeze(pubKey: String) : Boolean = transaction {
        getWallet(pubKey,null).let { record ->
            println("remove pubkey")
            println(pubKey)
            if (record != null){
                WalletRecordRepository.update({WalletRecordRepository.pubKey eq pubKey}){
                    it[blocked] = BigDecimal(0.0)
                    it[balance] = record.balance + record.blocked
                }
                true
            }else{
                false
            }
        }
    }

    fun getWallet(pubKey : String, secret:String?) : WalletRecordDTO?  =  transaction {
        WalletRecordRepository.select {
            WalletRecordRepository.pubKey eq pubKey
        }.firstOrNull()?.let {
            WalletRecord.wrapRow(it).convert(secret)
        }
    }

    fun getWallets(page : Long = 0) : List<WalletRecordDTO> = transaction {
        WalletRecordRepository.selectAll().orderBy(
            WalletRecordRepository.balance,SortOrder.DESC
        ).limit(20, page ).map {
            WalletRecord.wrapRow(it).convert(null)
        }
    }

    private fun createWallet(pKey: String,secret: String?) : WalletRecordDTO? = transaction {
        WalletRecordRepository.insert {
            it[pubKey] = pKey
        }
        getWallet(pKey,secret)
    }
}