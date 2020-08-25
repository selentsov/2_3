package old

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{ActorSystem, FSM, OneForOneStrategy, Props, Stash, SupervisorStrategy}
import akka.pattern.{BackoffSupervisor, after, ask, pipe}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object M_08_fault_tolerance {
  def main(args: Array[String]): Unit = {

    val system = ActorSystem("system")

    def getToken(): Future[String] =
      after(100.millis, system.scheduler)(Future.successful(s"token ${util.Random.nextInt()}"))

    object Counter {
      sealed trait Message
      case class Inc(i: Int = 1)     extends Message
      case object Get                extends Message
      case object UpdateToken        extends Message
      case class Throw(e: Exception) extends Message

      class ResumeException   extends RuntimeException
      class RestartException  extends RuntimeException
      class StopException     extends RuntimeException
      class EscalateException extends RuntimeException

      private case class SetToken(token: String)
      private case object RetryUpdate

      case class Count(cnt: Int, token: String)

      sealed trait State
      case object Initialising extends State
      case object Ready        extends State

      case class Data(cnt: Int, token: Option[String])
    }

    import Counter._

    class Counter extends FSM[State, Data] with Stash {
      override def supervisorStrategy: SupervisorStrategy =
        OneForOneStrategy(
          maxNrOfRetries = 10,
          withinTimeRange = 10.seconds,
          loggingEnabled = true
        ) {
          case _: ResumeException  => Resume
          case _: RestartException => Restart
          case _: StopException    => Stop
          case t                   => super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
        }

      import context.dispatcher

      val RETRY_UPDATE = "RETRY_UPDATE"

      def updateToken(): Unit =
        getToken().map(SetToken).pipeTo(self)

      startWith(Initialising, Data(0, None))

      onTransition {
        case _ => unstashAll()
      }

      onTransition {
        case from -> Initialising =>
          println(s"to Initialising from $from")
          updateToken()
          startTimerWithFixedDelay(RETRY_UPDATE, RetryUpdate, 1.minute)
        case Initialising -> _ =>
          cancelTimer(RETRY_UPDATE)
      }

      when(Initialising) {
        case Event(SetToken(token), data) =>
          goto(Ready) using data.copy(token = Some(token))
        case Event(RetryUpdate, _) =>
          updateToken()
          stay()
        case _ =>
          stash()
          stay()
      }

      whenUnhandled {
        case Event(Throw(e), _) => throw e
      }

      when(Ready) {
        case Event(UpdateToken, data) =>
          goto(Initialising) using data.copy(token = None)
        case Event(_, Data(_, None)) =>
          stash()
          goto(Initialising)
        case Event(Inc(i), data @ Data(cnt, _)) =>
          stay() using data.copy(cnt = cnt + i)
        case Event(Get, Data(cnt, Some(token))) =>
          sender() ! Count(cnt, token)
          stay()
      }

      onTermination {
        case StopEvent(reason, state, data) =>
          println(s"Stopped in state $state, with data $data, because of $reason")
      }

      initialize()
    }

    val supervisorProps = BackoffSupervisor.propsWithSupervisorStrategy(
      childProps = Props(new Counter),
      childName = "counter",
      minBackoff = 10.millis,
      maxBackoff = 1.second,
      randomFactor = 0.2,
      strategy = OneForOneStrategy(
        maxNrOfRetries = 10,
        withinTimeRange = 10.seconds,
        loggingEnabled = true
      ) {
        case _: ResumeException  => Resume
        case _: RestartException => Restart
        case _: StopException    => Stop
        case t                   => SupervisorStrategy.defaultDecider.applyOrElse(t, (_: Any) => Escalate)
      }
    )

    val counter = system.actorOf(supervisorProps, "supervisor")

    counter ! Inc(5)
    counter ! Inc()
    counter ! Inc()
    counter ! Inc()

    implicit val timeout: Timeout = 1.minute
    val result                    = (counter ? Get).mapTo[Count]

    println(s"Result1: ${Await.result(result, 1.minute)}")

    counter ! Inc()
    println(s"Result2: ${Await.result(counter ? Get, 1.minute)}")

    counter ! UpdateToken
    println(s"Result3: ${Await.result(counter ? Get, 1.minute)}")

//    counter ! Throw(new ResumeException)
//    (counter ? Get).foreach(r => println(s"Result after resume: $r"))
//
//    counter ! Throw(new RestartException)
//    (counter ? Get).foreach(r => println(s"Result after restart: $r"))

//    counter ! Throw(new StopException)
//    (counter ? Get).foreach(r => println(s"Result after stop: $r"))

    counter ! Throw(new EscalateException)
    (counter ? Get).foreach(r => println(s"Result after escalate: $r"))

    Thread.sleep(1000)

    Await.result(system.terminate(), 1.minute)

  }
}
