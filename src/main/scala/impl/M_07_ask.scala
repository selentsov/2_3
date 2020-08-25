package impl

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import impl.M_07_ask.Root.SystemMessage

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object M_07_ask {

  object Root {
    sealed trait SystemMessage
    case class Stop(reason: String) extends SystemMessage
    case class GetCounter(replyTo: ActorRef[ActorRef[Counter.Message]]) extends SystemMessage

    def apply(): Behavior[SystemMessage] = Behaviors.setup { ctx =>
      val counter = ctx.spawn(Counter(), "counter")

      Behaviors.receiveMessage {
        case Stop(reason) =>
          ctx.log.info(s"Stopping with reason: $reason")
          Behaviors.stopped
        case GetCounter(replyTo) =>
          replyTo ! counter
          Behaviors.same
      }
    }
  }

  object Counter {
    sealed trait Message
    case class Inc(i: Int = 1) extends Message
    case class Get(tkey: String, replyTo: ActorRef[Count]) extends Message
    
    case class Count(cnt: Int)

    def apply(cnt: Int = 0): Behavior[Message] = Behaviors.receiveMessage {
      case Inc(i) => Counter(cnt + i)
      case Get(_, replyTo) =>
        replyTo ! Count(cnt)
        Behaviors.same
    }
  }

  import Counter._

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[SystemMessage] = ActorSystem(Root(), "root")

    implicit val timeout: Timeout = 3.seconds

    val counterFuture: Future[ActorRef[Counter.Message]] = system ? Root.GetCounter
    val counter = Await.result(counterFuture, 1.second)

    counter ! Inc()
    counter ! Inc()
    counter ! Inc()
    counter ! Inc(100)

    val eventualCount: Future[Count] = counter ? (Get("", _))

    val count: Count = Await.result(eventualCount, 3.seconds)

    system.log.info(s"Count: $count")

    system.terminate()
  }

}
