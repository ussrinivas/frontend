package expiry

import java.nio.ByteBuffer

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.regions.Region.getRegion
import com.amazonaws.regions.Regions.EU_WEST_1
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient
import com.amazonaws.services.kinesis.model.{PutRecordRequest, PutRecordResult}
import com.gu.contentapi.client.model.{Content, SearchResponse}
import common.Edition._
import conf.Configuration.aws.mandatoryCredentials
import conf.Configuration.commercial.dfpAdFeatureReportKey
import conf.LiveContentApi
import dfp.GuLineItem
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
    if (ambiguous.nonEmpty) println("++++++++++++++++++++++++++++++++ ambiguous: " + ambiguous.size)
    unambiguous flatMap (_.matchingCapiTagIds)
  }

  def fetchTagIdsExpiredSince(time: DateTime): Seq[String] = {
    fetchTagIds { lineItem =>
      lineItem.isExpired && lineItem.endTime.exists(_.isAfter(time))
    }
  }

  def fetchTagIdsResurrectedSince(time: DateTime): Seq[String] = {
    // lifecycle is created > expired > unexpired > expired > ...
    // so need to know what has been updated in last x mins and did not expire in last x mins
    fetchTagIds { lineItem =>
      lineItem.lastModified.isAfter(time) && lineItem.endTime.exists(_.isAfterNow)
    }
  }

  def fetchContentIds(tagId: String)(implicit ec: ExecutionContext): Future[Seq[String]] = {

    def fetch(pageIndex: Int, acc: Seq[String]): Future[Seq[String]] = {

      def fetchPage(i: Int): Future[SearchResponse] = {
        val query = LiveContentApi.search(defaultEdition).tag(tagId).pageSize(100).page(i)
        LiveContentApi.getResponse(query)
      }

      def ids(contents: List[Content]): Seq[String] = contents map (_.id)

      fetchPage(pageIndex) flatMap { response =>
        response.pages match {
          case 0 => Future.successful(Nil)
          case i if i == pageIndex => Future.successful(acc ++ ids(response.results))
          case _ => fetch(pageIndex + 1, acc ++ ids(response.results))
        }
      }
    }

    fetch(1, Nil)
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

    def stream(tagIds: Seq[String], expired: Boolean): Future[Seq[Future[PutRecordResult]]] = {
      Future.sequence(tagIds map fetchContentIds) map (_.flatten) map { ids =>

        val hasDuplicates = ids.groupBy(identity).values.exists(_.size > 1)
        if (hasDuplicates) println("+++++++++++++++++++++++++++++++++++++++++++++++++ duplicates!")

        ids.sorted map { id =>
          putOntoStream(CommercialStatusUpdate(id, expired))
        }
      }
    }

    val threshold = DateTime.now().minusMonths(2)
    stream(fetchTagIdsExpiredSince(threshold), expired = true)
    stream(fetchTagIdsResurrectedSince(threshold), expired = false)
  }
}
