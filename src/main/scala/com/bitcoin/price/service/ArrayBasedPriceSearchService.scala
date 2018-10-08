package com.bitcoin.price.service

import java.text.DecimalFormat
import java.time._
import java.time.temporal.TemporalAdjusters

import com.bitcoin.price.model.MovingAverageResponse
import com.bitcoin.price.service.DateTimeDefaults._

import scala.collection.mutable

class ArrayBasedPriceSearchService(prices: Seq[Price]) extends IPriceSearchService {

  private lazy val priceValues = prices.map(_.price)
  private lazy val timeValues = prices.map(p => p.time.replaceAll("Z", ""))
  lazy val headTimeValue = timeValues.head

  override def search(start: String, end: String): Seq[Price] = {
    val res =
    (bstSearchForTimeValues(start), bstSearchForTimeValues(end)) match {
      case (Some(a), Some(b)) =>
        prices.slice(a, b + 1)
      case (Some(a), None) => prices.slice(a, prices.length)
      case (None, Some(b)) => prices.slice(0, b)
      case (None, None) => Seq.empty
    }
    res
  }


  private def bstSearchForTimeValues(time: String): Option[Int] = {
    val res = bs(time, 0, timeValues.length - 1, timeValues)
    res
  }


  private def bs(str: String, start: Int, end: Int, seqStr: Seq[String]): Option[Int] = {
    val mid = start + (end - start) / 2
    if (end >= start) {
      if (seqStr(mid) == str) {
        Some(mid)
      } else if (seqStr(mid).compareTo(str) > 0) {
        bs(str, start, mid - 1, seqStr)
      } else if (seqStr(mid).compareTo(str) < 0) {
        bs(str, mid + 1, end, seqStr)
      } else {
        None
      }
    } else {
      None
    }
  }

  override def searchLastWeek(): Seq[Price] = {
    val dateTimeNow = LocalDate.now(ZoneId.of("UTC")).atStartOfDay()
    val firstDateLastWeek = dateTimeNow.minusWeeks(1).`with`(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastDateLastWeek = firstDateLastWeek.plusDays(6)
    search(dateTimeFormatter.format(firstDateLastWeek), dateTimeFormatter.format(lastDateLastWeek))
  }

  override def searchLastMonth(): Seq[Price] = {
    val dateTimeNow = LocalDate.now(ZoneId.of("UTC")).atStartOfDay()
    val firstDateLastWeek = dateTimeNow.minusMonths(1).`with`(TemporalAdjusters.firstDayOfMonth())
    val lastDateLastWeek = dateTimeNow.minusMonths(1).`with`(TemporalAdjusters.lastDayOfMonth())
    search(dateTimeFormatter.format(firstDateLastWeek), dateTimeFormatter.format(lastDateLastWeek))
  }

  override def movingAverage(start: String, end: String, period: Int): Seq[MovingAverageResponse] = {
    val headDate = LocalDateTime.parse(headTimeValue, dateTimeFormatter).toLocalDate
    val startDate = LocalDateTime.parse(start, dateTimeFormatter).toLocalDate

    val endDate = LocalDateTime.parse(end, dateTimeFormatter).toLocalDate
    val absoluteEndDate = LocalDateTime.parse(timeValues.last, dateTimeFormatter).toLocalDate
    val extraDates = if(endDate.toEpochDay - absoluteEndDate.toEpochDay > 0) {
      val noDays = endDate.toEpochDay - absoluteEndDate.toEpochDay
      val dateRange = (1 to noDays.toInt).map(absoluteEndDate.plusDays(_).atStartOfDay())
      dateRange.map(p => Price("NA", dateTimeFormatter.format(p)))
    } else {
      Seq.empty[Price]
    }
    if(startDate.toEpochDay - headDate.toEpochDay >= period) {
      val startDate1 = dateTimeFormatter.format(startDate.minusDays(period).atStartOfDay())
      val tmpPrices = search(startDate1, end) ++ extraDates
      movingAverageCalc(tmpPrices, period).map(x => MovingAverageResponse(x._1.time, x._2)).splitAt(period - 1)._2
    }
    else {
      val startDate1 = dateTimeFormatter.format(headDate.plusDays(period).atStartOfDay())
      val tmpPrices = search(headTimeValue, end) ++ extraDates
      movingAverageCalc(tmpPrices, period).map(x => MovingAverageResponse(x._1.time, x._2)).splitAt(period - 1)._2
    }
  }

  private def movingAverageCalc(prices1: Seq[Price], p: Int): Seq[(Price, Double)] = {
    val decimalFormat = new DecimalFormat("###.##")
    var queue = mutable.Queue[Price]()
    var sum: Double = 0.0
    var prev = prices1.head
    var map = mutable.Map.empty[String, Double]
    prices1.tail.map {
      price =>
        val prevPrice = prev.price match {
          case "NA" => map(prev.time)
          case a => a.toDouble
        }
        sum += prevPrice
        queue.enqueue(Price(prevPrice.toString, price.time))
        if (queue.size > p) {
          sum -= queue.dequeue().price.toDouble
        }
        prev = price
        map += price.time -> sum/p

        (price, decimalFormat.format(sum/p).toDouble)
    }
  }

  override def next15DaysPredict(period: Int): Seq[Price] = {
    val dateNow = LocalDate.now(ZoneId.of("UTC")).atStartOfDay()
    val dateNowStr = dateTimeFormatter.format(dateNow.plusDays(1))
    val next15Days = dateTimeFormatter.format(dateNow.plusDays(15))
    movingAverage(dateNowStr, next15Days, period).map(t => Price(t.value.toString, t.date))
  }
}
