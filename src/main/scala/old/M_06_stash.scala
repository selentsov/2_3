package old

import akka.actor.{Actor, ActorSystem, FSM, Stash}
import akka.pattern.after
import akka.pattern.pipe

import scala.concurrent.Future
import scala.concurrent.duration._

object M_06_stash {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("system")

    object CounterFSM {
      sealed trait Message
      case class Inc(i: Int = 1) extends Message
      case object Get extends Message

      private case class SetToken(token: String)

      case class Count(cnt: Int, token: String)

      sealed trait State
      case object Initializing extends State
      case object Ready extends State

      case class Data(cnt: Int, token: Option[String])
    }

    import CounterFSM.{Data, State}

    class CounterFSM extends FSM[State, Data] with Stash {
      import CounterFSM._
      import context.dispatcher

      def getToken(): Future[String] =
        after(100.millis, system.scheduler)(Future.successful("token"))

      onTransition {
        case Initializing -> _ =>
          unstashAll()
      }

      onTransition {
        case _ -> Initializing =>
          getToken()
            .map(SetToken)
            .pipeTo(self)
      }

      when(Initializing) {
        case Event(SetToken(t), Data(cnt, _)) =>
          goto(Ready) using Data(cnt, Some(t))
        case _ =>
          stash()
          stay()
      }

      when(Ready) {
        case Event(_, Data(_, None)) =>
          stash()
          goto(Initializing)

        case Event(Inc(i), data @ Data(cnt, _)) =>
          stay() using data.copy(cnt = cnt + i)

        case Event(Get, Data(cnt, Some(t))) =>
          sender() ! Count(cnt, t)
          stay()
      }

      initialize()
    }

    object Counter {
      sealed trait Message
      case class Inc(i: Int = 1) extends Message
      case object Get extends Message

      private case class SetToken(token: String)

      case class Count(cnt: Int, token: String)
    }


    class Counter extends Actor with Stash {
      import Counter._
      import context.dispatcher

      def getToken(): Future[String] =
        after(100.millis, system.scheduler)(Future.successful("token"))

      override def preStart(): Unit = {
        super.preStart()
        getToken()
          .map(SetToken)
          .pipeTo(self)
      }


      def initializing: Receive = {
        case SetToken(t) =>
          unstashAll()
          context.become(ready(0, t))
        case _ => stash()
      }

      def ready(cnt: Int, token: String): Receive = {
        case Inc(i) => context.become(ready(cnt+i, token))
        case Get => sender() ! Count(cnt, token)
      }

      def receive: Receive = initializing
    }







//
//    val counter = system.actorOf(Props(new Counter), "counter")
//
//    counter ! Inc(5)
//    counter ! Inc()
//    counter ! Inc()
//    counter ! Inc()
//
//    Thread.sleep(1000) // TODO remove
//    implicit val timeout: Timeout = 1.minute
//    val result = counter ? Get
//    result foreach println
//    Await.result(result, 1.minute)
//
//
//    Await.result(system.terminate(), 1.minute)

  }
}
