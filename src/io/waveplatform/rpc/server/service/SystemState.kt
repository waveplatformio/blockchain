package io.waveplatform.rpc.server.service


data class ResponseStatus(var success:Boolean = true ,var response:Any? = null){
    fun notSynced() : ResponseStatus {
       return error("Node Not Synced")
    }

    fun error(msg : String?) : ResponseStatus{
        return this.apply {
            success = false
            response = msg
        }
    }

}

data class SystemState(var lastAction: Any? = null)


