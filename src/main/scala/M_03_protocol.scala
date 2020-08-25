import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object M_03_protocol {
  object Counter {
    // TODO: typed messages

    def apply(cnt: Int = 0): Behavior[String] = Behaviors.setup( ctx => Behaviors.receiveMessage {
      case "inc" => Counter(cnt + 1)
      case "print" =>
        ctx.log.info(s"Count: $cnt")
        Behaviors.same
    })
  }

  def main(args: Array[String]): Unit = {

    val system: ActorSystem[String] = ActorSystem(Counter(), "counter")

    val actor: ActorRef[String] = system

    actor ! "inc"
    actor ! "inc"
    actor ! "inc"
    actor ! "inc"
    actor ! "print"
    Thread.sleep(1000)

    system.terminate()
  }

}
