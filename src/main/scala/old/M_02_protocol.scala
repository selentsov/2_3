package old

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

object Counter {
  def apply(): Behavior[Nothing] = ???
}

object Worker {

}

object M_02_protocol {
  def main(args: Array[String]): Unit = {
    def greeter: Behavior[String] = Behaviors.receive { (context, name) =>
      context.log.info(s"Hello $name!")
      Behaviors.same
    }

    val system: ActorSystem[String] = ActorSystem(greeter, "greeter")

    val actor: ActorRef[String] = system

    actor.tell("World")
    actor ! "Akka"
    Thread.sleep(1000)

    system.terminate()
  }}