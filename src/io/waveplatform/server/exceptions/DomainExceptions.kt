package io.waveplatform.server.exceptions


class InsufficientBalance(message:String) : RuntimeException(message)
class AccountNotFoundException(message:String) : RuntimeException(message)
class MissingParameterException(message:String) : RuntimeException(message)
class UniqueConstraintException(message:String) : RuntimeException(message)


class InvalidDataException(message:String) : RuntimeException(message)
class RequestResponseException(message:String) : RuntimeException(message)
class ContentCompilerException(message: String) : RuntimeException(message)
class MessageBuildingException(message: String) : RuntimeException(message)