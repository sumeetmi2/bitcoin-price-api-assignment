package com.bitcoin.price.app

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging

import scala.sys.ShutdownHookThread

trait BaseApp {
  implicit val config: Config

  def start(): Unit

  def stop(): Unit
}

object BaseApp extends StrictLogging {
  def manageApp(app: BaseApp): ShutdownHookThread = {
    val shutdownHook = sys.addShutdownHook {
      logger.info("Shutting down app...")
      app.stop()
    }
    val appName = sys.props.getOrElse("appName", app.getClass.getSimpleName)
    val version = sys.props.getOrElse("version", "dev")
    logger.info(s"Starting $appName version $version...")
    app.start()
    shutdownHook
  }
}
