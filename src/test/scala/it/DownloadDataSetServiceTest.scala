package it

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.bitcoin.price.service.DownloadDataSet
import com.typesafe.config.ConfigFactory
import org.scalatest.FunSuite

class DownloadDataSetServiceTest extends FunSuite{
  val conf =
    """
      |coinbase.url="https://www.coinbase.com/api/v2/prices/BTC-USD/historic?period=year"
    """.stripMargin
  val config = ConfigFactory.parseString(conf)
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val downloadService = new DownloadDataSet(config)
  test("download should work for coinbase database") {
    assert(downloadService.dataSet.data.prices.nonEmpty)
  }

}
