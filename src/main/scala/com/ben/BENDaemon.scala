package com.ben

import akka.actor.ActorSystem
import com.ben.config.BENAppConfig
import grizzled.slf4j.Logging

import scala.concurrent._

object BENDaemon extends App with Logging {

  System.setProperty("bitcoins.log.location",
                     BENAppConfig.DEFAULT_DATADIR.toAbsolutePath.toString)

  implicit val system: ActorSystem = ActorSystem("ln-vortex")
  implicit val ec: ExecutionContext = system.dispatcher

  implicit val config: BENAppConfig =
    BENAppConfig.fromDefaultDatadir()

  val telegramHandler = new TelegramHandler(config.lndRpcClient)

  val f: Future[Unit] = for {
    _ <- telegramHandler.run()
  } yield ()

  f.failed.foreach { ex =>
    ex.printStackTrace()
    logger.error("Error", ex)
  }

}
