import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object M_02_state {
  object Counter {
    def apply(): Behavior[String] = ??? // setup
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
