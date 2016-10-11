package co.datamonsters.facebot.api

import pushka.annotation.{key, pushka}

@pushka case class UserInfo(
  @key("first_name") firstName: Option[String],
  @key("last_name") lastName: Option[String],
  @key("profile_pic") picture: Option[String],
  locale: Option[String],
  gender: Option[String],
  timezone: Option[Int]
)
