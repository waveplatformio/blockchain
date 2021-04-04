package io.waveplatform.rpc.server

data class MethodIdentifier(val intface: String, val methodName: String) {
    companion object {
        fun createFromFullName(fullName: String): MethodIdentifier
        {
            if(fullName.indexOf('.') == -1)
            {
                throw IllegalArgumentException("method name $fullName is invalid")
            }
            val parts = fullName.split(delimiters = *charArrayOf('.'), ignoreCase = true, limit = 2)
            return MethodIdentifier(parts[0], parts[1])
        }
    }
}