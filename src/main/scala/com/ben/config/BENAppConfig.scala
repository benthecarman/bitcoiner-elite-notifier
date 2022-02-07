package com.ben.config

import akka.actor.ActorSystem
import com.typesafe.config.Config
import grizzled.slf4j.Logging
import org.bitcoins.commons.config._
import org.bitcoins.core.currency.Satoshis
import org.bitcoins.lnd.rpc.LndRpcClient
import org.bitcoins.lnd.rpc.config.{LndInstance, LndInstanceLocal}

import java.io.File
import java.nio.file.{Path, Paths}
import scala.concurrent._
import scala.util.{Properties, Try}

/** Configuration for the Bitcoin-S wallet
  *
  * @param directory The data directory of the wallet
  * @param configOverrides Sequence of configuration overrides
  */
case class BENAppConfig(
    private val directory: Path,
    configOverrides: Vector[Config])(implicit system: ActorSystem)
    extends AppConfig
    with Logging {
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  override val moduleName: String = BENAppConfig.moduleName
  override type ConfigType = BENAppConfig

  override def newConfigOfType(configs: Vector[Config]): BENAppConfig =
    BENAppConfig(directory, configs)

  override val baseDatadir: Path = directory

  lazy val lndDataDir: Path =
    Paths.get(config.getString(s"$moduleName.lnd.datadir"))

  lazy val lndBinary: File =
    Paths.get(config.getString(s"$moduleName.lnd.binary")).toFile

  lazy val lndInstance: LndInstance =
    LndInstanceLocal.fromDataDir(lndDataDir.toFile)

  lazy val lndRpcClient: LndRpcClient =
    LndRpcClient(lndInstance, Some(lndBinary))

  lazy val telegramCreds: String =
    config.getString(s"$moduleName.telegramCreds")

  lazy val telegramId: String =
    config.getString(s"$moduleName.telegramId")

  lazy val maxPayment: Satoshis = {
    Try {
      val long = config.getLong(s"$moduleName.maxPayment")
      Satoshis(long)
    }.getOrElse(Satoshis.zero)
  }

  override def start(): Future[Unit] = Future.unit

  override def stop(): Future[Unit] = Future.unit

}

object BENAppConfig extends AppConfigFactoryBase[BENAppConfig, ActorSystem] {

  val DEFAULT_DATADIR: Path = Paths.get(Properties.userHome, ".ben")

  override def fromDefaultDatadir(confs: Vector[Config] = Vector.empty)(implicit
      ec: ActorSystem): BENAppConfig = {
    fromDatadir(DEFAULT_DATADIR, confs)
  }

  override def fromDatadir(datadir: Path, confs: Vector[Config])(implicit
      ec: ActorSystem): BENAppConfig =
    BENAppConfig(datadir, confs)

  override val moduleName: String = "ben"
}
