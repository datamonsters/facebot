package co.datamonsters.facebot.api

import pushka.annotation.{forceObject, key, pushka}

@pushka @forceObject case class Id(@key("id") value: String)
