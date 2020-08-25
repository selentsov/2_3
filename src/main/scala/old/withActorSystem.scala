package old

import akka.actor.ActorSystem

import scala.concurrent.Await
import scala.concurrent.duration._

object withActorSystem {
  def apply[T](name: String)(f: ActorSystem => T) = {
    val system = ActorSystem(name)

    try f(system)
    finally Await.ready(system.terminate(), 1.minute)
  }
}
