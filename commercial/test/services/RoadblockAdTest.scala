package services

import org.scalactic.TimesOnInt._
import org.scalatest.selenium.Chrome
import org.scalatest.time.{Span, _}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class RoadblockAdTest extends FlatSpec with Matchers with Chrome with BeforeAndAfterAll {

  implicitlyWait(Span(10, Seconds))
  setCaptureDir("screenshot")

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

  "Post non-SRA" should "show 4 expected creatives" in {
    go to "http://dfpgpt.appspot.com/s/14f1c8ea000000"

    100 times {
      checkCreative("1", "/simgad/3290263305859068221")
      checkCreative("2",
        "/simgad/14130908295974611552",
        "/simgad/5242505441099743741",
        "/simgad/8594783620388763154")
      checkCreative("3",
        "/simgad/14130908295974611552",
        "/simgad/5242505441099743741",
        "/simgad/8594783620388763154")
      checkCreative("4",
        "/simgad/14130908295974611552",
        "/simgad/5242505441099743741",
        "/simgad/8594783620388763154")
    }
  }

  override protected def afterAll(): Unit = {
    quit()
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
