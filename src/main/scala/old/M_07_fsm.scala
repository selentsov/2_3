package old

import akka.actor.{ActorLogging, ActorSystem, FSM, Props, Stash}
import akka.util.Timeout
import akka.pattern.after
import akka.pattern.pipe
import akka.pattern.ask

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object M_07_fsm {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("system")

    def getToken(): Future[String] =
      after(100.millis, system.scheduler)(Future.successful(s"token ${util.Random.nextInt()}"))

    object Counter {
      sealed trait Message
      case class Inc(i: Int = 1) extends Message
      case object Get            extends Message
      case object UpdateToken    extends Message

      case object DoUpdate

      private case class SetToken(token: String)

      case class Count(cnt: Int, token: String)
      case class Data(cnt: Int, token: Option[String])

      sealed trait State
      case object Initializing extends State
      case object Ready        extends State
    }

    import Counter._

    class Counter extends FSM[State, Data] with Stash with ActorLogging {
      import context.dispatcher

      startWith(Initializing, Data(0, None))

      onTransition {
        case _ => unstashAll()
      }

      val UPDATE_TOKEN = "UPDATE_TOKEN"

      onTransition {
        case _ -> Initializing =>
          self ! DoUpdate
          startTimerWithFixedDelay(UPDATE_TOKEN, DoUpdate, 1.minute)
        case Initializing -> _ =>
          cancelTimer(UPDATE_TOKEN)
      }

      when(Initializing) {
        case Event(DoUpdate, _) =>
          getToken().map(SetToken).pipeTo(self)
          stay()
        case Event(SetToken(token), data) =>
          goto(Ready) using data.copy(token = Some(token))
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
        case Event(Get, Data(cnt, Some(token))) =>
          sender() ! Count(cnt, token)
          stay()
        case Event(UpdateToken, _) =>
          goto(Initializing)
      }

      onTermination {
        case StopEvent(reason, state, data) =>
          log.debug(s"onTermination: $reason, $state, $data")
      }

      initialize()

    }

    val counter = system.actorOf(Props(new Counter), "counter")

    counter ! Inc(5)
    counter ! Inc()
    counter ! Inc()
    counter ! Inc()

    implicit val timeout: Timeout = 1.minute
    val result                    = (counter ? Get).mapTo[Count]

    println(s"Result: ${Await.result(result, 1.minute)}")

    counter ! UpdateToken

    val result2 = (counter ? Get).mapTo[Count]

    println(s"Result2: ${Await.result(result2, 1.minute)}")

    Await.result(system.terminate(), 1.minute)

  }
}
