package co.datamonsters.facebot.exceptions

/**
  * @author Aleksey Fomkin <aleksey.fomkin@gmail.com>
  */
case class TokenValidationException(message: String) extends Exception(message) with FacebotException
