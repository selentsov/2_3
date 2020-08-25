package old

import akka.actor.{Actor, Props}
import akka.util.Timeout
import akka.pattern.ask
import akka.pattern.pipe

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object M_05_behavior {
  def main(args: Array[String]): Unit = withActorSystem("system"){system =>


    object Counter {
      sealed trait Message
      case class Inc(i: Int = 1) extends Message
      case object Get extends Message

      case class Result(s: String)
    }

    import Counter._

    class Counter extends Actor {
      def asyncAction(): Future[String] = {
        import akka.pattern.after
        after(100.millis, system.scheduler)(Future.successful("res"))
      }

      def receive: Receive = behavior(0)

      def behavior(cnt: Int): Receive = {
        case Inc(i) => context.become(behavior(cnt + i))
        case Get =>
          asyncAction().map(r => Result(s"$r -> $cnt")).pipeTo(sender())
      }
    }

    implicit val timeout: Timeout = 1.second

    val counter = system.actorOf(Props(new Counter), "counter")

    counter ! Inc(5)
    counter ! Inc()
    counter ! Inc()
    counter ! Inc()
    val result: Future[Result] = (counter ? Get).mapTo[Result]
    (1 to 100).foreach(_ => counter ! Inc())

    println(s"Result: ${Await.result(result, 1.second)}")
  }
}
