package co.datamonsters

import scala.language.higherKinds

package object facebot {
  type EventHandler[F[_]] = PartialFunction[Event[F], F[_]]
}
