package old

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.routing.FromConfig
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

object M_11_dispatcher {
  def main(args: Array[String]): Unit = {

    val system = ActorSystem("system")

    val fastBlocking = system.actorOf(Props[FastBlockingCounter](), "fast-blocking")
    val blocking     = system.actorOf(FromConfig.props(Props[BlockingCounter]()), "broadcast-router1")

    (1 to 10).foreach(blocking ! _)
    (1 to 10).foreach(fastBlocking ! _)

    implicit val timeout: Timeout = 1.minute
    val cnt =
      Await.result((blocking ? "get").mapTo[Int], 10.seconds)

    println(s"Result1: $cnt")

    Await.result(system.terminate(), 1.minute)

  }
}
