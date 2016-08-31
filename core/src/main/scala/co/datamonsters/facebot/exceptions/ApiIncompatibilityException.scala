package co.datamonsters.facebot.exceptions

/**
  * @author Aleksey Fomkin <aleksey.fomkin@gmail.com>
  */
case class ApiIncompatibilityException(request: String, cause: Throwable)
    extends Exception(s"Can't parse\n$request", cause)
    with FacebotException
