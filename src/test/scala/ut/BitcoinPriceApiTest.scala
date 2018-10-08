package ut

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.bitcoin.price.api.BitcoinPriceApi
import com.bitcoin.price.model.{CustomDateRangeRequest, GetNext15DaysPriceRequest, MovingAverageRequest, MovingAverageResponse}
import com.bitcoin.price.service.{IPriceSearchService, Price}
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.DefaultFormats
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{FunSuite, Matchers}

class BitcoinPriceApiTest extends FunSuite with Matchers with Json4sSupport with ScalatestRouteTest {

  val conf =
    """
      |api {
      |  http {
      |    bind = "0.0.0.0"
      |    port = 8911
      |    shutdownWait = 10s
      |  }
      |}
      |
      |coinbase.url="https://www.coinbase.com/api/v2/prices/BTC-USD/historic?period=year"
    """.stripMargin
  implicit val config = ConfigFactory.parseString(conf)
  implicit val formats = DefaultFormats
  implicit val serialization = org.json4s.jackson.Serialization

  val mockPriceSearchService = mock(classOf[IPriceSearchService])
  val dummyReturnLastWeek = Seq(Price("100.0", "2018-10-07T00:00:00"), Price("101.0", "2018-10-08T00:00:00"))
  val dummyReturnLastMonth = Seq(Price("101.0", "2018-10-09T00:00:00"), Price("102.0", "2018-10-10T00:00:00"))
  val dummyReturnCustomSearch = Seq(Price("102.0", "2018-10-10T00:00:00"), Price("103.0", "2018-10-11T00:00:00"))
  val dummyReturnNext15Days  = Seq(Price("103.0", "2018-10-11T00:00:00"), Price("104.0", "2018-10-13T00:00:00"))
  val dummyReturnMovingAvg = Seq(MovingAverageResponse("2018-10-08T00:00:00", 110.0))
  when(mockPriceSearchService.searchLastWeek()) thenReturn dummyReturnLastWeek
  when(mockPriceSearchService.searchLastMonth()) thenReturn dummyReturnLastMonth
  when(mockPriceSearchService.search(any[String], any[String])) thenReturn dummyReturnCustomSearch
  when(mockPriceSearchService.next15DaysPredict(any[Int])) thenReturn dummyReturnNext15Days
  when(mockPriceSearchService.movingAverage(any[String], any[String], any[Int])) thenReturn dummyReturnMovingAvg

  val bitcoinPriceApi = new BitcoinPriceApi(mockPriceSearchService)
  val routes = bitcoinPriceApi.routes

  test("last week prices api route test") {
    Get(s"/getLastWeekPrices") ~>  routes ~> check {
      status should equal(StatusCodes.OK)
      val resp = responseAs[Seq[Price]]
      resp should contain theSameElementsAs dummyReturnLastWeek
    }
  }

  test("last month prices api route test") {
    Get(s"/getLastMonthPrices") ~>  routes ~> check {
      status should equal(StatusCodes.OK)
      val resp = responseAs[Seq[Price]]
      resp should contain theSameElementsAs dummyReturnLastMonth
    }
  }

  test("custom date range api route test") {
    val request = CustomDateRangeRequest("2017-10-29", "2017-10-30")
    Post(s"/getPricesForCustomDateRange", request) ~>  routes ~> check {
      status should equal(StatusCodes.OK)
      val resp = responseAs[Seq[Price]]
      resp should contain theSameElementsAs dummyReturnCustomSearch
    }
  }

  test("moving average api route test") {
    val request = MovingAverageRequest(CustomDateRangeRequest("2017-10-29", "2017-10-30"), 2)
    Post(s"/getMovingAveragePrices", request) ~>  routes ~> check {
      status should equal(StatusCodes.OK)
      val resp = responseAs[Seq[MovingAverageResponse]]
      resp should contain theSameElementsAs dummyReturnMovingAvg
    }
  }

  test("next 15 days prediction api route test") {
    val request = GetNext15DaysPriceRequest(2)
    Post(s"/getNext15DaysPrediction", request) ~>  routes ~> check {
      status should equal(StatusCodes.OK)
      val resp = responseAs[Seq[Price]]
      resp should contain theSameElementsAs dummyReturnNext15Days
    }
  }

}
