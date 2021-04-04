package io.waveplatform.blockchain

import io.waveplatform.blockchain.crypto.Convert
import io.waveplatform.blockchain.crypto.Crypto
import io.waveplatform.blockchain.db.PoolUser
import io.waveplatform.blockchain.db.WalletRecordDTO
import io.waveplatform.blockchain.db.services.BlockService
import io.waveplatform.blockchain.wallet.Transaction
import java.math.BigDecimal


class BlockChain(
    val blockService: BlockService = BlockService(),
    val wallet : WalletRecordDTO,
    var account: Account = Account(blockService,wallet),
    var stake: Stake = Stake(blockService)
) {


    init {
        println("creatiiing genesiis")
        blockService.createGenesis(Block.genesis())
    }

    fun addBlock(block: Block): Block{
        this.updateChain(block)
        return block
    }

    fun incrementConfirmation(block: Block){
        account.blockService.incrementConfirmation(block)
    }

    fun updateChain(block: Block){
        block.attachSignature(this.account.wallet.pubKey)
        block.confirmations = block.confirmations + 1
        account.blockService.finishBlock(block)
        this.executeTransaction(block)
    }

    fun getLastBlocks(offset : Long): MutableList<Block> {
        return blockService.getLastBlocks(offset)
    }

    fun createBlock(transactions: MutableList<Transaction>): Block{
        val previousBlock = blockService.getChainLastBlock()
        val block = Block.createBlock(
            previousBlock,
            transactions,
            Convert.toHexString(announceLeader())
        )
        blockService.createBlock(block)
        return blockService.getLastBlock()!!
    }



    fun isValidChain(chain:MutableList<Block>): Boolean{
        println("isValidChain")
        if (chain[0].hash != Block.genesis().hash){
            return false;
        }

        chain.forEachIndexed { index, ch ->
            if (index > 0){
                val block = ch
                val digest = Crypto.sha256()
                block.transactions.forEach { transaction ->
                    digest.update(transaction.bytes())
                }
                val generatedHash = Convert.toHexString(digest.digest())
                val previousBlock = chain[index - 1]
                if (this.blockService.getBlock(block.hash!!) == null){
                    return false
                }
                if ((block.previousHash != previousBlock.hash) || (block.hash != generatedHash)){
                    return false
                }
            }
         }
        return true
    }

    fun getHeight() : Long {
        return this.blockService.getHeight()
    }

    fun getBlockByHeight(hght : Long, extra : String = "") : Block  {
        return this.blockService.getBlockByHeight(hght, extra = extra)
    }

    fun getChainLastBlock() : Block {
        return this.blockService.getChainLastBlock()
    }

    fun getLastBlock(extra : String = "") : Block {
        if (this.blockService.getLastBlock() != null){
            return this.blockService.getLastBlock(extra = extra)!!
        }
        return getChainLastBlock()
    }

    fun syncFreezeExtraFeature(transaction:Transaction) {
         this.account.accountService.executeWalletFreezeTransaction(transaction)
    }

    fun syncChain(currentChain: CurrentChain) : Boolean {
        //todo block reward on synchronizatio nstate
        if (currentChain.height > 1.toLong() && currentChain.height > this.getHeight()){
            currentChain.block.validator!!
           //create and finish block
           this.blockService.createBlock(currentChain.block,f = currentChain.block.finished)
           //add transaction records
           currentChain.block.transactions.forEach {
               if(it.type.type == "FREEZE"){
                   if (!this.blockService.transactionService.walletFreezeExists(it.input!!.from)){
                       if (this.blockService.transactionService.getTransaction(it.txId) == null){
                           this.blockService.transactionService.addTransaction(it,currentChain.block.id)
                           this.syncFreezeExtraFeature(it)
                       }
                   }
               } else{
                   if(it.input!!.from != "Fee-Reward" && it.input!!.from != "Block-Reward"){
                       //just init if not initialized wallet
                       this.account.accountService.createOrGet(it.output!!.to,null)
                       this.account.accountService.createOrGet(it.input!!.from,null)
                   }

                   this.blockService.transactionService.addTransaction(it,currentChain.block.id)
                   if(currentChain.block.finished) this.blockService.transactionService.finishTransaction(it.txId,currentChain.block.id)

               }
           }
           if(currentChain.block.finished){
               this.account.accountService.executeWalletSyncTransactions(currentChain.block.transactions)
           }else{
               //not finished block we need just make spendable
               this.account.accountService.executeWalletSyncSpendableTransactions(currentChain.block.transactions)
           }
           return true
       }
        return false
    }

    fun getBalance(publicKey: String): BigDecimal{
       return this.account.getBalance(publicKey)
    }

    private fun announceLeader(): ByteArray{
        val validator = this.stake.announcePoolAndGetLeader()
        return Convert.parseHexString(validator)
    }

    fun poolLeaderExists(validator : String) : Boolean {
        return this.stake.stakeService.getPoolUser(wallet.pubKey,validator) != null
    }

    fun poolLeaderExists(pl:String, validator : String) : Boolean {
        return this.stake.stakeService.getPoolUser(pl,validator) != null
    }

    fun getNonSpendableBalance(pl: String) : BigDecimal{
        return this.account.getNonSpendableBalance(pl)
    }

    fun getPoolUser(validator: String) : PoolUser{
        return this.stake.stakeService.getPoolUser(validator)!!
    }


    /**
     * checks to bo completed
     * check hash
     * check last hash
     * check signature
     * check leader
     */
    fun isValidBlock(block: Block): Boolean{
        var previousBlock = this.blockService.getChainLastBlock()
        val digest = Crypto.sha256()
        block.transactions.forEach { transaction ->
            digest.update(transaction.bytes())
        }
        val generatedHash = Convert.toHexString(digest.digest())
        println("isValidBlock")
        println(block.checkSignature())
        when{
            block.previousHash == previousBlock.hash &&
                    block.hash == generatedHash &&
                    block.checkSignature() &&
                    block.verifyLeader(block, this.stake.poolLeaderExists(wallet.pubKey,block.validator!!)!!) -> return true
        }
        return false
    }

    fun executeTransaction(block: Block){
        println("executing transaction")
        this.account.giveReward(block)
        block.transactions.forEach { transaction ->
            when(transaction.type.type){
                "transaction" -> {
                    this.account.update(transaction)
                    this.account.transferFee(block,transaction)
                }
                "stake" -> {
//                    this.stake.update(transaction)
//                    this.account.update(transaction)
//                    this.account.transferFee(block,transaction)
                }
                "unstake" ->{
                    try{
                        if (this.stake.decrementUpdateValidator(transaction)){
                            this.account.updateReStake(transaction)
                            this.account.transferFee(block,transaction)
                        }else{
                            this.account.invalidStakeUppdate(transaction)
                        }
                    }catch (tt : TransactionTransferException){

                    }
                }
                "validator" -> {
                    if (this.stake.updateValidator(transaction)){
                        this.account.updateStake(transaction)
                        this.account.transferFee(block,transaction)
                    }else{
                        this.account.invalidStakeUppdate(transaction);
                    }
                }
            }
        }
    }

    fun isLastBlock(block: Block): Boolean{
        println("isLastBlock")
        println(blockService.getChainLastBlock().hash + " " +  block.hash)
        return blockService.getChainLastBlock().hash == block.hash
    }

}