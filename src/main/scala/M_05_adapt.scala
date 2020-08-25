import M_05_adapt.Root.Stop
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object M_05_adapt {

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
    // TODO: remove reference to Worker
    case class Get(replyTo: ActorRef[Worker.Count]) extends Message

    def apply(cnt: Int = 0): Behavior[Message] = Behaviors.receiveMessage {
      case Inc(i) => Counter(cnt + i)
      case Get(replyTo) =>
        replyTo ! Worker.Count(cnt)
        Behaviors.same
    }
  }

  import Counter._

  object Worker {
    sealed trait Message
    case class Count(cnt: Int) extends Message

    def apply(counter: ActorRef[Counter.Message], root: ActorRef[Stop]): Behavior[Message] = Behaviors.setup { ctx =>
      counter ! Inc()
      counter ! Inc()
      counter ! Inc()
      counter ! Inc(100)
      counter ! Get(ctx.self)

      Behaviors.receiveMessage {
        case Count(cnt) =>
          ctx.log.info(s"Received count: $cnt")
          root ! Stop("Done")
          Behaviors.stopped
      }
    }
  }

  def main(args: Array[String]): Unit = {
    ActorSystem(Root(), "root")
  }

}
