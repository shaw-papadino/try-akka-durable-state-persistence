package sample.persistence

import akka.actor.typed.scaladsl.ActorContext
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
  sealed trait Command[ReplyMessage] extends CborSerializable
  final case class Increment(replyTo: ActorRef[Done]) extends Command[Done]
  final case class IncrementBy(value: Int, replyTo: ActorRef[Done])
      extends Command[Done]
  final case class GetValue(replyTo: ActorRef[State]) extends Command[Done]

  final case class State(value: Int) extends CborSerializable {
    def +(num: Int): State = copy(value = value + num)
  }
  object State {
    val empty: State = State(0)
  }

  def apply(persistenceId: PersistenceId): Behavior[Command[_]] = {
    Behaviors.setup[Command[_]] { context =>
      counter(context, persistenceId)
    }
  }
  def counter(
    ctx: ActorContext[Command[_]],
    persistenceId: PersistenceId
  ): DurableStateBehavior[Command[_], State] = {
    DurableStateBehavior[Command[_], State](
      persistenceId,
      emptyState = State.empty,
      commandHandler = (state, command) =>
        command match {
          case Increment(replyTo) =>
            println("increment")
            Effect.persist(state + 1).thenReply(replyTo)(_ => Done)
          case IncrementBy(by, replyTo) =>
            Effect.persist(state + by).thenReply(replyTo)(_ => Done)
          case GetValue(replyTo) =>
            Effect.reply(replyTo)(state)
          case _ =>
            println("boo")
            Effect.noReply
      }
    )
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
