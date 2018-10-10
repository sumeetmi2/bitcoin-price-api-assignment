package com.bitcoin.price.api

import java.time.LocalDate
import javax.ws.rs.Path

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, concat, path, _}
import akka.http.scaladsl.server._
import com.bitcoin.price.model._
import com.bitcoin.price.service.DateTimeDefaults._
import com.bitcoin.price.service.{IPriceSearchService, Price}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import io.swagger.annotations._
import org.json4s.DefaultFormats

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Path("/v1")
@Api
class BitcoinPriceApi(priceSearchService: IPriceSearchService)
                     (implicit config: Config, ec: ExecutionContext) extends Json4sSupport with StrictLogging {

  val routes = {
    implicit val formats = DefaultFormats
    implicit val serialization = org.json4s.jackson.Serialization

    concat(
      (path("getLastWeekPrices") & get & handleRejections(RejectionHandler.default) &
        handleExceptions(ExceptionHandler {
          case NonFatal(e) =>
            logger.error("failed getting prices for last week", e)
            complete(StatusCodes.InternalServerError)
        })) {
        request =>
          request.complete(searchForLastWeek)
      },
      (path("getLastMonthPrices") & get & handleRejections(RejectionHandler.default) &
        handleExceptions(ExceptionHandler {
          case NonFatal(e) =>
            logger.error("failed getting prices for last month", e)
            complete(StatusCodes.InternalServerError)
        })) {
        request =>
          request.complete(searchForLastMonth)
      },
      (path("getPricesForCustomDateRange") & post & handleRejections(RejectionHandler.default) &
        handleExceptions(ExceptionHandler {
          case NonFatal(e) =>
            logger.error("failed getting prices for custom date range", e)
            complete(StatusCodes.InternalServerError)
        })) {
        entity(as[CustomDateRangeRequest]) {
          request =>
            complete(searchForCustomDateRange(request))
        }
      },
      (path("getMovingAveragePrices") & post & handleRejections(RejectionHandler.default) &
        handleExceptions(ExceptionHandler {
          case NonFatal(e) =>
            logger.error("failed calculating moving average", e)
            complete(StatusCodes.InternalServerError)
        })) {
        entity(as[MovingAverageRequest]) {
          request =>
            complete(movingAverageForPrice(request))
        }
      },
      (path("getNext15DaysPrediction") & post & handleRejections(RejectionHandler.default) &
        handleExceptions(ExceptionHandler {
          case NonFatal(e) =>
            logger.error("failed predicting prices for next 15 days", e)
            complete(StatusCodes.InternalServerError)
        })) {
        entity(as[GetNext15DaysPriceRequest]) {
          param =>
            complete(getNext15DaysPrediction(param))
        }
      },
      (path("getBucketWiseMaxPriceCustomDateRange") & post & handleRejections(RejectionHandler.default) &
        handleExceptions(ExceptionHandler {
          case NonFatal(e) =>
            logger.error("failed getting max bucket price for custom date range", e)
            complete(StatusCodes.InternalServerError)
        })) {
        entity(as[BucketWiseMaxPriceRequest]) {
          param =>
            complete(getBucketWiseMaxPriceForCustomDateRange(param))
        }
      }
    )
  }

  @Path("/getLastWeekPrices")
  @ApiOperation(value = "Getting Prices For Last Week",
    httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success", response = classOf[Seq[Price]]),
    new ApiResponse(code = 400, message = "Malformed request data"),
    new ApiResponse(code = 403, message = "Unauthorized access"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def searchForLastWeek: Future[Seq[Price]] = {
    val res = priceSearchService.searchLastWeek()
    Future.successful(res)
  }

  @Path("/getLastMonthPrices")
  @ApiOperation(value = "Getting Prices For Last Month",
    httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success", response = classOf[Seq[Price]]),
    new ApiResponse(code = 400, message = "Malformed request data"),
    new ApiResponse(code = 403, message = "Unauthorized access"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def searchForLastMonth: Future[Seq[Price]] = {
    val res = priceSearchService.searchLastMonth()
    Future.successful(res)
  }

  @Path("/getPricesForCustomDateRange")
  @ApiOperation(value = "Getting prices In Custom Date Range (input format dd-MM-yyyy)",
    httpMethod = "POST")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success", response = classOf[Seq[Price]]),
    new ApiResponse(code = 400, message = "Malformed request data"),
    new ApiResponse(code = 403, message = "Unauthorized access"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def searchForCustomDateRange(customDateRangeRequest: CustomDateRangeRequest): Future[Seq[Price]] = {
    val startDate = LocalDate.parse(customDateRangeRequest.startDate, inputDateTimeFormatter).atStartOfDay()
    val endDate = LocalDate.parse(customDateRangeRequest.endDate, inputDateTimeFormatter).atStartOfDay()
    val res = priceSearchService.search(dateTimeFormatter.format(startDate), dateTimeFormatter.format(endDate))
    Future.successful(res)
  }

  @Path("/getMovingAveragePrices")
  @ApiOperation(value = "Get moving average of prices date range in format (yyyy-MM-dd) and period: Int",
    httpMethod = "POST")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success", response = classOf[Seq[MovingAverageResponse]]),
    new ApiResponse(code = 400, message = "Malformed request data"),
    new ApiResponse(code = 403, message = "Unauthorized access"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def movingAverageForPrice(movingAverageRequest: MovingAverageRequest): Future[Seq[MovingAverageResponse]] = {
    val customDateRangeRequest = movingAverageRequest.customDateRangeRequest
    val startDate = LocalDate.parse(customDateRangeRequest.startDate, inputDateTimeFormatter).atStartOfDay()
    val endDate = LocalDate.parse(customDateRangeRequest.endDate, inputDateTimeFormatter).atStartOfDay()
    val res = priceSearchService.movingAverage(dateTimeFormatter.format(startDate), dateTimeFormatter.format(endDate), movingAverageRequest.period)
    Future.successful(res)
  }

  @Path("/getNext15DaysPrediction")
  @ApiOperation(value = "Get prediction for next 15 days. Default period is 10 if you leave value 0",
    httpMethod = "POST")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success", response = classOf[Seq[Price]]),
    new ApiResponse(code = 400, message = "Malformed request data"),
    new ApiResponse(code = 403, message = "Unauthorized access"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getNext15DaysPrediction(price: GetNext15DaysPriceRequest): Future[Seq[Price]] = {
    val res = price.period match {
      case x if x > 0 => priceSearchService.next15DaysPredict(x)
      case _ => priceSearchService.next15DaysPredict()
    }
    Future.successful(res)
  }

  @Path("/getBucketWiseMaxPriceCustomDateRange")
  @ApiOperation(value = "Get max price per bucket for date range. date in format yyyy-MM-dd",
    httpMethod = "POST")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success", response = classOf[Seq[BucketWiseMaxPriceResponse]]),
    new ApiResponse(code = 400, message = "Malformed request data"),
    new ApiResponse(code = 403, message = "Unauthorized access"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getBucketWiseMaxPriceForCustomDateRange(req: BucketWiseMaxPriceRequest): Future[Seq[BucketWiseMaxPriceResponse]] = {
    val customDateRangeRequest = req.customDateRangeRequest
    val startDate = LocalDate.parse(customDateRangeRequest.startDate, inputDateTimeFormatter).atStartOfDay()
    val endDate = LocalDate.parse(customDateRangeRequest.endDate, inputDateTimeFormatter).atStartOfDay()
    val res = priceSearchService.bucketWiseMaxPriceSearch(dateTimeFormatter.format(startDate),
      dateTimeFormatter.format(endDate), req.bucket)
    Future.successful(res)
  }
}
