package com.whisk.docker.specs2

import com.whisk.docker.DockerKit
import org.slf4j.LoggerFactory

import scala.concurrent._
import scala.concurrent.duration._

trait DockerTestKit extends BeforeAfterAllStopOnError with DockerKit {
  private lazy val log = LoggerFactory.getLogger(this.getClass)

  def beforeAll() = {
    Await.result(pullImages(), 1200.seconds)

    val allRunning = try {
      val initReady =
        initReadyAll()
          .map(xs => xs.map { case (_, b) => b }.forall(identity))
          .recover {
            case e =>
              log.error("Cannot run docker containers", e)
              false
          }

      Await.result(initReady, 1200.seconds)
    } catch {
      case e: Exception =>
        log.error("Exception during container initialization", e)
        false
    }

    if (!allRunning) {
      Await.result(stopRmAll(), 20.seconds)
      throw new RuntimeException("Cannot run all required containers")
    }
  }

  def afterAll() = {
    try {
      Await.result(stopRmAll(), 20.seconds)
    } catch {
      case e: Throwable =>
        log.error(e.getMessage, e)
        throw e
    }
  }
}
