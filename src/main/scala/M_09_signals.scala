import java.util.UUID

import M_09_signals.Root.Stop
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object M_09_signals {

  object Root {
    case class Stop(reason: String)

    def apply(): Behavior[Stop] = Behaviors.setup { ctx =>
      val counter = ctx.spawn(Counter(), "counter")
      ctx.spawn(Worker(counter, ctx.self), "worker")

      Behaviors.receiveMessage {
        case Stop(reason) =>
          ctx.log.info(s"Stopping with reason: $reason")
          Behaviors.stopped
      }
    }
  }

  object Counter {
    sealed trait Message
    case class Inc(i: Int = 1) extends Message
    case class Get(replyTo: ActorRef[Count]) extends Message
    case object Error extends Message
    case object Fail extends Message
    case object UnhandledError extends Message

    class CounterError extends RuntimeException
    class CounterFail extends RuntimeException
    class CounterUnhandledError extends RuntimeException

    case class Count(cnt: Int)

    def behavior(cnt: Int = 0): Behavior[Message] = Behaviors.receiveMessage {
      case Inc(i) => behavior(cnt + i)
      case Get(replyTo) =>
        replyTo ! Count(cnt)
        Behaviors.same
      case Error => throw new CounterError
      case Fail => throw new CounterFail
      case UnhandledError => throw new CounterUnhandledError
    }

    def apply() =
      Behaviors.supervise(
        Behaviors.supervise(behavior()).onFailure[CounterFail](SupervisorStrategy.restart)
      ).onFailure[CounterError](SupervisorStrategy.resume)
  }

  import Counter._

  object Worker {

    sealed trait Message

    case class Count(requestId: UUID, cnt: Int) extends Message

    case class CountTimeout(requestId: UUID, reason: Throwable) extends Message

    def apply(counter: ActorRef[Counter.Message], root: ActorRef[Stop]): Behavior[Message] = Behaviors.setup { ctx =>
      counter ! Inc()
      counter ! Inc()
      counter ! Fail
      counter ! Fail
      counter ! Inc()
      counter ! Error
      counter ! UnhandledError
      counter ! Inc(100)

      implicit val timeout: Timeout = 3.seconds

      val requestId = UUID.randomUUID()
      ctx.ask(counter, Get) {
        case Success(Counter.Count(cnt)) => Count(requestId, cnt)
        case Failure(reason) => CountTimeout(requestId, reason)
      }

      Behaviors.receiveMessage {
        case Count(`requestId`, cnt) =>
          ctx.log.info(s"Received count: $cnt")
          root ! Stop("Done")
          Behaviors.stopped
        case CountTimeout(`requestId`, reason) =>
          ctx.log.error("Count timeout", reason)
          root ! Stop("Timeout")
          Behaviors.stopped
        case msg =>
          ctx.log.error(s"Unexpected message: $msg")
          Behaviors.same
      }
    }
  }

  def main(args: Array[String]): Unit = {
    ActorSystem(Root(), "root")
  }

}
