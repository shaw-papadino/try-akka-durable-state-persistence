package sample.persistence

import java.util.UUID

import akka.{Done, pattern}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.DurableStateBehavior

import scala.concurrent.duration._

class MyPersistentCounterSpec extends ScalaTestWithActorTestKit(s"""
         akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
         akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
         akka.persistence.snapshot-store.local.dir = "target/snapshot"
         """) with AnyWordSpecLike {

  "The Counter" should {
    val persistenceId = PersistenceId.ofUniqueId("abc")
    "increment with confirmation" in {
      val counter = testKit.spawn(MyPersistentCounter(persistenceId))
      println(counter)
      val probe = testKit.createTestProbe[StatusReply[Done]]
      Thread.sleep(500)
      eventually {
        counter ! MyPersistentCounter.Increment(probe.ref)
        probe.receiveMessage()
      }

    }
    "get value" in {
      val counter = testKit.spawn(MyPersistentCounter(persistenceId))
      val probe =
        testKit.createTestProbe[StatusReply[MyPersistentCounter.State]]
      counter ! MyPersistentCounter.GetValue(probe.ref)
      probe.receiveMessage()
    }
  }
}
