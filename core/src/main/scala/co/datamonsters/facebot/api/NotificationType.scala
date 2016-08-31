package co.datamonsters.facebot.api

sealed abstract class NotificationType(val value: String)

object NotificationType {
  case object Regular extends NotificationType("REGULAR")
  case object SilentPush extends NotificationType("SILENT_PUSH")
  case object NoPush extends NotificationType("NO_PUSH")
}
