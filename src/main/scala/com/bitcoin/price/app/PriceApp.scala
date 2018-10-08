package com.bitcoin.price.app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.bitcoin.price.api.BitcoinPriceApi
import com.bitcoin.price.service.{ArrayBasedPriceSearchService, DownloadDataSet, IPriceSearchService}
import com.bitcoin.price.swagger.SwaggerDocService
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.language.implicitConversions

class SwaggerDocServiceImpl extends SwaggerDocService {
  // Add new Api type whenever adding new API, e.g. Set(classOf[RedactApi], classOf[XXXApi])
  override val apiClasses: Set[Class[_]] = Set(classOf[BitcoinPriceApi])
}

class PriceApp extends BaseApp with StrictLogging {

  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)

  implicit def asJavaDuration(d: scala.concurrent.duration.FiniteDuration): Duration =
    java.time.Duration.ofNanos(d.toNanos)

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  override implicit val config: Config = ConfigFactory.load("application.conf")

  val prices = new DownloadDataSet(config).prices.reverse
  val priceSearchService: IPriceSearchService = new ArrayBasedPriceSearchService(prices)
  val bitcoinPriceApi = new BitcoinPriceApi(priceSearchService)
  val bind: String = config.getString("api.http.bind")
  val port: Int = config.getInt("api.http.port")
  val shutdownWait: Duration = config.getDuration("api.http.shutdownWait")
  var bindingFutures: List[Future[Http.ServerBinding]] = Nil

  def routes: Route = concat(
    pathPrefix("api" / "v1") {
      concat(bitcoinPriceApi.routes)
    },
    new SwaggerDocServiceImpl().routes
  )

  override def start(): Unit = {
    bindingFutures = Http().bindAndHandle(routes, bind, port) :: bindingFutures
    logger.info(s"Running HTTP on http://$bind:$port")
  }

  override def stop(): Unit = {
    val future = Future.sequence(unbind(bindingFutures)).flatMap(_ => system.terminate())
    // Do not return until shutdown is done (Test by: sudo lsof -i tcp:8443)
    Await.result(future, Duration.Inf)
  }

  def unbind(bindings: Seq[Future[Http.ServerBinding]]): Seq[Future[Unit]] = {
    bindings.map { binding =>
      val result: Future[Unit] = binding
        .flatMap(_.unbind())
        .recover { case _ => Unit }
      result
    }
  }
}

object PriceApp extends App {
  BaseApp.manageApp(new PriceApp())
}

