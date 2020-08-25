import M_06_actor_ask.Root.Stop
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object M_06_actor_ask {

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
    
    case class Count(cnt: Int)

    def apply(cnt: Int = 0): Behavior[Message] = Behaviors.receiveMessage {
      case Inc(i) => Counter(cnt + i)
      case Get(replyTo) =>
        replyTo ! Count(cnt)
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

      // TODO: add timeout (withTimers?)
      // TODO request ID
      val countMapper = ctx.messageAdapter[Counter.Count](c => Count(c.cnt))
      counter ! Get(countMapper)

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
