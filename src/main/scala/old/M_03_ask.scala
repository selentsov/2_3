package old

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object M_03_ask {
  def main(args: Array[String]): Unit = withActorSystem("system"){ system =>


    object Counter {
      sealed trait Message
      case class Inc(i: Int = 1) extends Message
      case object Get extends Message

      case class Count(cnt: Int)
    }

    import Counter._

    class Counter extends Actor {
      var cnt = 0

      def receive: Receive = {
        case Inc(i) => cnt += i
        case Get => sender() ! Count(cnt)
      }
    }

    val counter = system.actorOf(Props(new Counter), "counter")

    counter ! Inc(5)
    counter ! Inc()
    counter ! Inc()
    counter ! Inc()

    implicit val timeout: Timeout = 1.minute

    val result = counter ? Get

    result.foreach(r => println(s"Result: $r"))


    Thread.sleep(1000)

  }
}
