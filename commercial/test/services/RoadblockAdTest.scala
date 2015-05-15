package services

import java.util.concurrent.atomic.AtomicInteger

import net.lightbody.bmp._
import net.lightbody.bmp.client._
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.{CapabilityType, DesiredCapabilities}
import org.scalactic.TimesOnInt._
import org.scalatest.selenium.Chrome
import org.scalatest.time.{Span, _}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.collection.JavaConversions._

class RoadblockAdTest extends FlatSpec with Matchers with Chrome with BeforeAndAfterAll {

  val server = new BrowserMobProxyServer()

  override protected def beforeAll(): Unit = {
    implicitlyWait(Span(10, Seconds))
    setCaptureDir("screenshot")

    server.start()
    val proxy = ClientUtil.createSeleniumProxy(server)
    val capabilities = new DesiredCapabilities()
    capabilities.setCapability(CapabilityType.PROXY, proxy)
    val driver = new ChromeDriver(capabilities)
    server.newHar("yahoo.com")
    driver.get("http://yahoo.com")
    val har = server.getHar()
    println("*1")
//    println(har.getLog.getPages.head.getTitle)
    println(har.getLog.getEntries.headOption.map(_.getResponse.getHeaders.headOption.map(_.getName)))
  }

  override protected def afterAll(): Unit = {
    quit()
    server.stop()
  }

  private def checkCreative(i: String, expected: String*): Unit = {
    withScreenshot {
      switchTo(frame(cssSelector("[id$='-" + i + "'] > div > iframe")))
      val creative = cssSelector("div > a > img").element.attribute("src")
      switchTo(defaultContent)

      creative shouldBe defined

      val creativePath = creative.get
        .stripPrefix("http://pagead2.googlesyndication.com")
        .stripPrefix("https://tpc.googlesyndication.com")

      expected should contain(creativePath)
    }
  }

  //  "Post non-SRA" should "show 4 expected creatives" in {
  //    100 times {
  //
  //      go to "http://dfpgpt.appspot.com/s/14f1c8ea000000"
  //
  //      checkCreative("1", "/simgad/3290263305859068221")
  //      checkCreative("2",
  //        "/simgad/14130908295974611552",
  //        "/simgad/5242505441099743741",
  //        "/simgad/8594783620388763154")
  //      checkCreative("3",
  //        "/simgad/14130908295974611552",
  //        "/simgad/5242505441099743741",
  //        "/simgad/8594783620388763154")
  //      checkCreative("4",
  //        "/simgad/14130908295974611552",
  //        "/simgad/5242505441099743741",
  //        "/simgad/8594783620388763154")
  //    }
  //  }

  "Ad-feature logo" should "be shown" in {
    val count: AtomicInteger = new AtomicInteger()
    100 times {
      go to "http://www.theguardian.com/rolex-partner-zone/2014/dec/03/rolex-entries-2016" +
        "-enterprise-awards"
      val logo = cssSelector("[id='dfp-ad--adbadge'] > div > a > img").findElement
      logo shouldBe defined
      val style = cssSelector("[id='dfp-ad--adbadge']").element.attribute("style")
      println(count.incrementAndGet())
      style shouldBe Some("")
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
