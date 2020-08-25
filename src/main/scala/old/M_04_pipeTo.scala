package old

import akka.actor.{Actor, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object M_04_pipeTo {
  def main(args: Array[String]): Unit = withActorSystem("system"){system =>


    object Service {
      sealed trait Message
      case object Get extends Message

      case class Result(value: String)
    }

    import Service._

    class Service extends Actor {
      def asyncAction(): Future[Result] = {
        import akka.pattern.after
        after(100.millis, system.scheduler)(Future.successful(Result("res")))
      }

      def receive: Receive = {
        case Get =>
//          val replyTo = sender()
//          asyncAction().foreach(res => replyTo ! res)


          asyncAction().pipeTo(sender())
      }
    }

    val service = system.actorOf(Props(new Service), "service")

    implicit val timeout: Timeout = 1.minute

    val result = (service ? Get).mapTo[Result]

    result.foreach(r => println(s"Result: $r"))

    Await.result(result, 1.minute)
  }
}
