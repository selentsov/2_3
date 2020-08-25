import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object M_07_ask {

  object Root {
    case class Stop(reason: String)

    def apply(): Behavior[Stop] = Behaviors.setup { ctx =>
      val counter = ctx.spawn(Counter(), "counter")

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

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(Root(), "root")

    val counter: ActorRef[Counter.Message] = ???

    counter ! Inc()
    counter ! Inc()
    counter ! Inc()
    counter ! Inc(100)

// TODO    counter ! Get()

    system.terminate()
  }

}
