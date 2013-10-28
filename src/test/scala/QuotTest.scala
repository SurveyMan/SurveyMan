import org.apache.log4j.{Logger}
import org.junit._
import scalautils.QuotMarks
import scalautils.QuotMarks.UnicodeQuot

class QuotTest {
  private val LOGGER : Logger = Logger.getLogger(classOf[QuotTest])

  @Test def testQuots {
    for ((UnicodeQuot(l, r), _) <- QuotMarks.quotpairs)
      LOGGER.info(f"left: $l%s, right: $r%s")
  }
}