package com.bitcoin.price.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import org.json4s.DefaultFormats

import scala.concurrent.Await
import scala.concurrent.duration._

case class Price(price: String, time: String)
case class Coin(base: String, currency: String, prices: Seq[Price])
case class CoinbaseResponse(data: Coin)

class DownloadDataSet(config: Config)(implicit materializer: ActorMaterializer, actorSystem: ActorSystem) {

  import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
  lazy val dataSet = downloadDataSet
  lazy val prices = dataSet.data.prices
  private implicit val formats = DefaultFormats
  private implicit val serialization = org.json4s.jackson.Serialization

  private def downloadDataSet = {
    implicit val executionContext = actorSystem.dispatcher
    val response = Http().singleRequest(HttpRequest(uri = config.getString("coinbase.url")))
    Await.result(response.flatMap {
      res =>
        Unmarshal(res.entity).to[CoinbaseResponse]
    }, 10 seconds)

  }
}
