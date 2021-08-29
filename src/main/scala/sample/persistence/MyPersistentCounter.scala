package sample.persistence

import akka.{Done, NotUsed}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.DurableStateBehavior.CommandHandler
import akka.persistence.typed.state.scaladsl.{
  DurableStateBehavior,
  Effect,
  ReplyEffect
}

object MyPersistentCounter {
  sealed trait Command extends CborSerializable
  final case class Increment(replyTo: ActorRef[StatusReply[Done]])
      extends Command
  final case class IncrementBy(value: Int, replyTo: ActorRef[StatusReply[Done]])
      extends Command
  final case class GetValue(replyTo: ActorRef[StatusReply[State]])
      extends Command

  final case class State(value: Int) extends CborSerializable {
    def +(num: Int): State = copy(value = value + num)
  }
  object State {
    val empty: State = State(0)
  }

  val commandHandler: (State, Command) => ReplyEffect[State] =
    (state, command) =>
      command match {
        case Increment(replyTo) =>
          println("increment")
          Effect.persist(state + 1).thenReply(replyTo)(_ => StatusReply.Ack)
        case IncrementBy(by, replyTo) =>
          Effect.persist(state + by).thenReply(replyTo)(_ => StatusReply.Ack)
        case GetValue(replyTo) =>
          Effect.reply(replyTo)(StatusReply.success(state))
        case _ =>
          println("boo")
          Effect.noReply
    }
  def apply(persistenceId: PersistenceId): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
      println("apply")
      context.log.debug("apply")
      DurableStateBehavior[Command, State](
        persistenceId,
        emptyState = State.empty,
//        commandHandler = CommandHandler.command[Command, State] { cmd =>
//          println("Got commnd {}", cmd)
//          context.log.debug("Got command {}", cmd)
//          Effect.none
//        }
        commandHandler = commandHandler
      )
    }
  }
}

object PersistentClient extends App {
  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>
    val persistenceId = PersistenceId.ofUniqueId("123")
    val p = context.spawn(MyPersistentCounter(persistenceId), "persistence")
    p ! MyPersistentCounter.Increment(context.system.ignoreRef)
    Behaviors.empty
  }
  val persistence =
    ActorSystem(apply(), "persistence")
}
