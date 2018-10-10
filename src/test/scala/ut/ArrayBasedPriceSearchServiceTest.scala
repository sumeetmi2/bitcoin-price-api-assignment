package ut

import java.time.temporal.TemporalAdjusters
import java.time.{LocalDate, YearMonth, ZoneId}

import com.bitcoin.price.model.{BucketWiseMaxPriceResponse, MovingAverageResponse}
import com.bitcoin.price.service.DateTimeDefaults._
import com.bitcoin.price.service.{ArrayBasedPriceSearchService, Price}
import org.scalatest.{FunSuite, Matchers}

class ArrayBasedPriceSearchServiceTest extends FunSuite with Matchers {

  val todayDate = LocalDate.now(ZoneId.of("UTC")).atStartOfDay()
  val pricesMap = (1 to 60).map {
    t => dateTimeFormatter.format(todayDate.minusDays(t)) -> (1000 + t * 100 toString)
  }.toMap

  val prices = pricesMap.map(t => Price(t._2, t._1)).toSeq.sortWith {
    (a, b) =>
      a.time.compareTo(b.time) < 0
  }


  val arrayBasedPriceSearch = new ArrayBasedPriceSearchService(prices)
  test("should give last week prices") {
    arrayBasedPriceSearch.searchLastWeek() should contain theSameElementsAs prices.reverse.splitAt(7)._1
  }

  test("should give last month prices") {
    val lastMonthPriceStart = todayDate.minusMonths(1).`with`(TemporalAdjusters.firstDayOfMonth())
    val expectedResultSeq = (0 until YearMonth.from(todayDate.minusMonths(1)).lengthOfMonth()).map {
      t =>
        val currDateStr = dateTimeFormatter.format(lastMonthPriceStart.plusDays(t))
        Price(pricesMap.getOrElse(currDateStr, "NA"), currDateStr)
    }.filterNot(_.price == "NA")
    arrayBasedPriceSearch.searchLastMonth() should contain theSameElementsAs expectedResultSeq
  }

  test("should give custom date range prices") {
    val startDate = todayDate.minusDays(5)
    val endDate = todayDate.minusDays(3)
    val startDateStr = dateTimeFormatter.format(startDate)
    val endDateStr = dateTimeFormatter.format(endDate)

    val expectedResultSeq = (0 to 2).map { t =>
      val currDateStr = dateTimeFormatter.format(startDate.plusDays(t))
      Price(pricesMap.getOrElse(currDateStr, "NA"), currDateStr)
    }.filterNot(_.price == "NA")

    arrayBasedPriceSearch.search(startDateStr, endDateStr) should contain theSameElementsAs expectedResultSeq
  }

  test("should give moving average for the custom date Range") {
    val prices1 = Seq(Price("900", "2018-10-06T00:00:00"), Price("1000", "2018-10-07T00:00:00"), Price("1100", "2018-10-08T00:00:00"), Price("1200", "2018-10-09T00:00:00"))
    val arrayBasedPriceSearchService1 = new ArrayBasedPriceSearchService(prices1)
    val expectedResult = Seq(MovingAverageResponse("2018-10-08T00:00:00", 950.0), MovingAverageResponse("2018-10-09T00:00:00", 1050.0))
    arrayBasedPriceSearchService1.movingAverage("2018-10-06T00:00:00", "2018-10-09T00:00:00", 2) should contain theSameElementsAs expectedResult
  }

  test("should give next 15 days price prediction") {
    val historicalPrices = Seq("900", "1000", "1100", "1200")
    val startDate = todayDate.minusDays(historicalPrices.size - 1)
    val next15DaysPredictionExpected = Seq("1150.0","1175.0","1162.5","1168.75","1165.62","1167.19","1166.41","1166.8","1166.6","1166.7","1166.65","1166.67","1166.66","1166.67", "1166.67")
    val inputPrices = historicalPrices.indices
      .map(t => Price(historicalPrices(t), dateTimeFormatter.format(startDate.plusDays(t))))

    val outputPredictions = next15DaysPredictionExpected.indices.map(t =>
      Price(next15DaysPredictionExpected(t), dateTimeFormatter.format(todayDate.plusDays(t + 1))))

    val arrayBasedPriceSearchService1 = new ArrayBasedPriceSearchService(inputPrices)
    arrayBasedPriceSearchService1.next15DaysPredict(2) should contain theSameElementsAs outputPredictions
  }


  test("should return bucket wise max price") {
    val prices1 = Seq(Price("900", "2018-10-06T00:00:00"), Price("1000", "2018-10-07T00:00:00"), Price("1200", "2018-10-08T00:00:00"), Price("1100", "2018-10-09T00:00:00"))
    val arrayBasedPriceSearchService2 = new ArrayBasedPriceSearchService(prices1)
    val start = "2018-10-06T00:00:00"
    val end = "2018-10-11T00:00:00"
    val expectResult = Seq(
      BucketWiseMaxPriceResponse(0, "2018-10-07T00:00:00", 1000.0),
      BucketWiseMaxPriceResponse(1, "2018-10-08T00:00:00", 1200.0)
    )
    val result = arrayBasedPriceSearchService2.bucketWiseMaxPriceSearch(start, end, 2)

    result should contain theSameElementsAs expectResult
  }


}
