package expiry

import common.Edition._
import conf.Configuration.commercial.dfpAdFeatureReportKey
import conf.LiveContentApi
import dfp.GuLineItem
import model.Content
import org.joda.time.DateTime
import tools.Store

import scala.concurrent.{ExecutionContext, Future}

object Producer {

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

  def run()(implicit ec: ExecutionContext): Unit = {
    val tagIds = fetchTagIdsExpiredSince(DateTime.now().minusMonths(2))
    val contentIds = tagIds map fetchContentIds
    Future.sequence(contentIds) map (_.flatten) foreach { ids =>

      val hasDuplicates = ids.groupBy(identity).values.exists(_.size > 1)
      if (hasDuplicates) println("+++++++++++++++++++++++++++++++++++++++++++++++++++ duplicates!")

      ids.sorted foreach { id =>
        println(id)
      }
    }
  }
}
