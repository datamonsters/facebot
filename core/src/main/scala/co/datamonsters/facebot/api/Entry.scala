package co.datamonsters.facebot.api

import pushka.annotation.pushka

@pushka case class Entry(
    id: String,
    time: Long,
    messaging: Seq[Messaging]
)
