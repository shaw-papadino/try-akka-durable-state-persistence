package sample.persistence

import java.util.concurrent.atomic.AtomicInteger

import akka.{Done, pattern}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.testkit.PersistenceTestKitDurableStateStorePlugin
import org.scalatest.wordspec.AnyWordSpecLike
import akka.persistence.typed.PersistenceId
import com.typesafe.config.{Config, ConfigFactory}

class MyPersistentCounterSpec
    extends ScalaTestWithActorTestKit(MyPersistentCounterSpec.conf)
    with AnyWordSpecLike {

  val pidCounter = new AtomicInteger(0)
  private def nextPid() =
    PersistenceId.ofUniqueId(s"c${pidCounter.incrementAndGet()})")

  "The Counter" should {
    "increment with confirmation" in {
      val counter = testKit.spawn(MyPersistentCounter(nextPid()))
      val probe = testKit.createTestProbe[Done]
      counter ! MyPersistentCounter.Increment(probe.ref)
      probe.expectMessage(Done)

    }
    "get value" in {
      val counter = testKit.spawn(MyPersistentCounter(nextPid()))
      val updateProbe =
        testKit.createTestProbe[Done]
      counter ! MyPersistentCounter.Increment(updateProbe.ref)
      counter ! MyPersistentCounter.Increment(updateProbe.ref)
      counter ! MyPersistentCounter.Increment(updateProbe.ref)

      val queryProbe =
        testKit.createTestProbe[MyPersistentCounter.State]
      counter ! MyPersistentCounter.GetValue(queryProbe.ref)
      queryProbe.expectMessage(MyPersistentCounter.State(3))
    }
  }
}

object MyPersistentCounterSpec {
  def conf: Config =
    PersistenceTestKitDurableStateStorePlugin.config.withFallback(
      ConfigFactory.parseString(s"""
       akka.loglevel = INFO
       """)
    )
}
