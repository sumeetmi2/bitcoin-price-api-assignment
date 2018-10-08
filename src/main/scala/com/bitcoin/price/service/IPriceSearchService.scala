package com.bitcoin.price.service

import com.bitcoin.price.model.MovingAverageResponse

//:TODO ArraybasedPriceSearchService to be replaced by Reddis based
trait IPriceSearchService {
  def search(start: String, end: String): Seq[Price]

  def searchLastWeek(): Seq[Price]

  def searchLastMonth(): Seq[Price]

  def movingAverage(start: String, end: String, period: Int): Seq[MovingAverageResponse]

  def next15DaysPredict(period: Int = 10): Seq[Price]
}
