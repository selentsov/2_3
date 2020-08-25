package impl

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import impl.M_03_protocol.Counter.{Inc, Print}

object M_03_protocol {

  object Counter {
    sealed trait Message
    case class Inc(i: Int = 1) extends Message
    case object Print extends Message

    def apply(cnt: Int = 0): Behavior[Message] = Behaviors.setup( ctx => Behaviors.receiveMessage {
      case Inc(i) => Counter(cnt + i)
      case Print =>
        ctx.log.info(s"Count: $cnt")
        Behaviors.same
    })
  }

  def main(args: Array[String]): Unit = {

    val system: ActorSystem[Counter.Message] = ActorSystem(Counter(), "counter")

    val actor: ActorRef[Counter.Message] = system

    actor ! Inc()
    actor ! Inc()
    actor ! Inc()
    actor ! Inc(100)
    actor ! Print
    Thread.sleep(1000)

    system.terminate()
  }

}
