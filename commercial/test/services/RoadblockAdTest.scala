package services

import java.net.URLDecoder
import java.util.concurrent.atomic.AtomicInteger

import net.lightbody.bmp.core.har.{HarEntry, HarNameValuePair}
import net.lightbody.bmp.proxy.ProxyServer
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.{CapabilityType, DesiredCapabilities}
import org.scalactic.TimesOnInt._
import org.scalatest.selenium.Chrome
import org.scalatest.selenium.WebBrowser.go
import org.scalatest.time.{Span, _}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.collection.JavaConversions._

class RoadblockAdTest extends FlatSpec with Matchers with Chrome with BeforeAndAfterAll with
BeforeAndAfterEach {

  private val proxy = new ProxyServer(8081)

  override implicit val webDriver: ChromeDriver = {
    val capabilities = new DesiredCapabilities()
    capabilities.setCapability(CapabilityType.PROXY, proxy.seleniumProxy())
    new ChromeDriver(capabilities)
  }

  override protected def beforeAll(): Unit = {
    implicitlyWait(Span(10, Seconds))
    setCaptureDir("screenshot")
    proxy.start()
  }

  override protected def afterEach(): Unit = {
    close()
  }

  override protected def afterAll(): Unit = {
    proxy.stop()
    quit()
  }

  private def checkCreative(url: String, lineItemId: Int): Unit = {
    withScreenshot {

      val adCalls = AdCall.getAdCalls(proxy,
        switchTo(frame(cssSelector("[id$='-" + 1 + "'] > div > iframe")))
        ,
        url)

      println(url)
      println("*1")
      println(adCalls.size)

      adCalls foreach { call =>
        println(call.url)
        println(call.method)
        println(if (call.secure) {"secure"} else {"not"})
        println(call.slotLevelTargeting)
        println(call.lineItemIds)
      }

      adCalls.head.lineItemIds should contain(lineItemId)
    }
  }

  "Post non-SRA" should "show 4 expected creatives" in {
    10 times {
      checkCreative("http://dfpgpt.appspot.com/s/14f1c8ea000000", 123)
    }
  }

  "Ad-feature logo" should "be shown" in {
    val count: AtomicInteger = new AtomicInteger()
    10 times {

      def waitForAdLoad(domSlotId: String): Unit = {
        switchTo(frame(cssSelector(s"[id=$domSlotId] > div > iframe")))
      }

      val host = "http://www.theguardian.com"
      val url = s"$host/rolex-partner-zone/2014/dec/03/rolex-entries-2016-enterprise-awards"

      withScreenshot {
        val adCalls = AdCall.getAdCalls(proxy, waitForAdLoad("dfp-ad--adbadge"), url)

        println()
        println(count.incrementAndGet())

        adCalls foreach { call =>
          println(url)
          println(call.method)
          println(if (call.secure) {"secure"} else {"not"})
          println(call.slotLevelTargeting)
          println(call.lineItemIds)
        }

        adCalls.head.lineItemIds should contain("76519527")
      }
    }
  }
}


//Get non-SRA:
//http://dfpgpt.appspot.com/s/104f835a000000
//
//Post SRA:
//http://dfpgpt.appspot.com/s/17f41dca000000
//
//Get SRA:
//http://dfpgpt.appspot.com/s/104f835a000000

case class AdCall(url: String,
                  method: String,
                  requestHeaders: Map[String, String],
                  responseStatus: Int,
                  responseHeaders: Map[String, String]) {

  def mkList(strVal: Option[String], sep: String): List[String] = {
    strVal.map(_.split(sep).toList).getOrElse(Nil)
  }

  val secure = url.startsWith("https://")

  val requestParameters = {
    if (method == "GET") {
      val queryString = url.split("\\?").lastOption
      queryString.map(_.split("&")).map { paramStrings =>
        paramStrings.map { paramString =>
          val keyValue = paramString.split("=")
          keyValue.head -> URLDecoder.decode(keyValue.last, "utf-8")
        }.toMap
      }.getOrElse(Map.empty)
    } else {
      Map.empty[String, String]
    }
  }

  val slotLevelTargeting = mkList(requestParameters.get("prev_scp"), "\\|")

  val lineItemIds = mkList(responseHeaders.get("Google-LineItem-Id"), ",")

  val creativeIds = mkList(responseHeaders.get("Google-Creative-Id"), ",")
}

object AdCall {

  def getAdCalls(proxy: ProxyServer, wait: => Unit, url: String)
                (implicit driver: WebDriver): Seq[AdCall] = {
    proxy.setCaptureHeaders(true)

    proxy.newHar("har")

    go to url

    wait

    def adCallFilter(entry: HarEntry): Boolean = {
      val url = entry.getRequest.getUrl
      url.contains("pubads.g.doubleclick.net/gampad/ads?") &&
        url.contains("&callback=callbackProxy&")
    }

    proxy.getHar.getLog.getEntries.filter(adCallFilter).map { entry =>

      def headerMap(headers: Seq[HarNameValuePair]): Map[String, String] = {
        headers.map(header => header.getName -> header.getValue).toMap
      }

      val request = entry.getRequest
      val response = entry.getResponse
      AdCall(
        request.getUrl,
        request.getMethod,
        headerMap(request.getHeaders),
        response.getStatus,
        headerMap(response.getHeaders)
      )
    }
  }
}
