package io.waveplatform.blockchain

enum class BlockchainState(val value: String){
    SYNCED("SYNCED"),
    NOTSYNCED("NOTSYNCED"),
    SYNCSTARTED("SYNCSTARTED")
}


object State {
    var state : BlockchainState = BlockchainState.NOTSYNCED
    var progress : Int = 0


    fun isSynced() : Boolean{
        return state == BlockchainState.SYNCED
    }

    fun set(state:BlockchainState){
        this.state = state
    }

    fun synced(){
        this.progress = 0;
        this.updateState(BlockchainState.SYNCED)
    }

    fun unSync(){
       this.updateState(BlockchainState.NOTSYNCED)
    }

    private fun updateState(state: BlockchainState){
        this.state = state
        //other state works like a shut up/down services
    }

}