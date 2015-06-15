package expiry

import java.nio.ByteBuffer

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.regions.Region.getRegion
import com.amazonaws.regions.Regions.EU_WEST_1
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient
import com.amazonaws.services.kinesis.model.{PutRecordRequest, PutRecordResult}
import common.Edition._
import conf.Configuration.aws.mandatoryCredentials
import conf.Configuration.commercial.dfpAdFeatureReportKey
import conf.LiveContentApi
import dfp.GuLineItem
import model.Content
import org.joda.time.DateTime
import tools.Store

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object Producer {

  implicit class RichKinesisClient(client: AmazonKinesisAsyncClient) {

    def asyncPutRecord(request: PutRecordRequest): Future[PutRecordResult] = {
      val promise = Promise[PutRecordResult]()

      val handler = new AsyncHandler[PutRecordRequest, PutRecordResult] {
        override def onSuccess(request: PutRecordRequest, result: PutRecordResult): Unit =
          promise.complete(Success(result))
        override def onError(exception: Exception): Unit =
          promise.complete(Failure(exception))
      }

      client.putRecordAsync(request, handler)

      promise.future
    }
  }

  private lazy val client: AmazonKinesisAsyncClient =
    new AmazonKinesisAsyncClient(mandatoryCredentials).withRegion(getRegion(EU_WEST_1))

  private def fetchTagIds(p: GuLineItem => Boolean): Seq[String] = {
    val adFeatureTags = Store.getDfpPaidForTags(dfpAdFeatureReportKey).paidForTags
    val expired = adFeatureTags filter (_.lineItems.exists(p))
    val (ambiguous, unambiguous) = expired partition (_.matchingCapiTagIds.size > 1)
    if (ambiguous.nonEmpty) println("++++++++++++++++++++++++++++++++++++++ " + ambiguous.size)
    unambiguous flatMap (_.matchingCapiTagIds)
  }

  def fetchTagIdsExpiredSince(time: DateTime): Seq[String] = {
    fetchTagIds { lineItem =>
      lineItem.isExpired && lineItem.endTime.exists(_.isAfter(time))
    }
  }

  def fetchTagIdsResurrectedSince(time: DateTime): Seq[String] = ???

  def fetchContentIds(tagId: String)(implicit ec: ExecutionContext): Future[Seq[String]] = {
    val query = LiveContentApi.search(defaultEdition).tag(tagId).pageSize(100)
    val eventualResponse = LiveContentApi.getResponse(query)
    eventualResponse map { response =>
      if (response.total > 100) println("++++++++++++++++++++++++++++++++++++++ " + response.total)
      response.results map (Content(_)) map (_.id)
    }
  }

  def putOntoStream(update: CommercialStatusUpdate): Future[PutRecordResult] = {
    val status = ByteBuffer.wrap(update.expired.toString.getBytes("UTF-8"))
    val request = new PutRecordRequest()
      .withStreamName(Config.streamName)
      .withPartitionKey(update.contentId)
      .withData(status)
    client.asyncPutRecord(request)
  }

  def run()(implicit ec: ExecutionContext): Unit = {
    val tagIds = Producer.fetchTagIdsExpiredSince(DateTime.now().minusMonths(2))
    val contentIds = tagIds map Producer.fetchContentIds
    Future.sequence(contentIds) map (_.flatten) foreach { ids =>

      val hasDuplicates = ids.groupBy(identity).values.exists(_.size > 1)
      if (hasDuplicates) println("+++++++++++++++++++++++++++++++++++++++++++++++++++ duplicates!")

      ids.sorted foreach { id =>
        putOntoStream(CommercialStatusUpdate(id, expired = true))
      }
    }
  }
}
