package old

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume}
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

object M_09_actor_selection {
  def main(args: Array[String]): Unit = {

    val system = ActorSystem("system")

    object ServiceManager {
      sealed trait Message
      case class GetNamedService(name: String) extends Message
    }

    class ServiceManager extends Actor {
      import ServiceManager._

      override def supervisorStrategy: SupervisorStrategy =
        OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 5.seconds){
          case _: Service.ResumeException => Resume
          case _: Service.RestartException => Restart
          case t => super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
        }

      def behavior(state: Map[String, ActorRef]): Receive = {
        case GetNamedService(name) if state.contains(name) => sender() ! state(name)
        case GetNamedService(name) =>
          val res = context.actorOf(Props(new Service(name)), name)
          sender() ! res
          context.become(behavior(state.updated(name, res)))
      }

      def receive: Receive = behavior(Map.empty)
    }

    object Service {
      sealed trait Message
      case object GetName extends Message
      case class Throw(e: Exception) extends Message

      class ResumeException extends RuntimeException
      class RestartException extends RuntimeException

    }

    class Service(name: String) extends Actor {
      val x = util.Random.nextInt()

      import Service._
      def receive: Receive = {
        case GetName => sender() ! s"$name $x"
        case Throw(e) => throw e
      }

      override def preStart(): Unit = {
        super.preStart()
        println("starting")
      }
    }

    val manager = system.actorOf(Props(new ServiceManager), "manager")

    implicit val timeout: Timeout = 1.minute
    val service: ActorRef =
      Await.result((manager ? ServiceManager.GetNamedService("myService")).mapTo[ActorRef], 2.seconds)

    println(s"Result1: ${Await.result(service ? Service.GetName, 1.second)}")

    val selection = system.actorSelection("/user/manager/*")

    import Service._

    service ! Throw(new RestartException)

    Thread.sleep(1000)

    println(s"Result after restart: ${Await.result(service ? Service.GetName, 1.second)}")

    println(s"Result2: ${Await.result(selection ? Service.GetName, 1.second)}")


    Await.result(system.terminate(), 1.minute)

  }
}
