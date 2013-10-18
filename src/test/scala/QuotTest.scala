import csv.CSVLexer
import java.io.IOException
import org.apache.log4j.{SimpleLayout, Level, FileAppender, Logger}
import org.junit._
import scalautils.QuotMarks
import scalautils.QuotMarks.UnicodeQuot

class QuotTest {
  private val LOGGER : Logger = Logger.getRootLogger()
  LOGGER.setLevel(Level.ALL)
  private var txtHandler : FileAppender = null
  try {
    txtHandler = new FileAppender(new SimpleLayout(), "logs/CSVTest.log")
    txtHandler.setEncoding("UTF-8")
    LOGGER.addAppender(txtHandler)
  } catch {
    case io : IOException => {
      println (io.getMessage())
      System.exit(-1)
    }
  }
  @Test def testQuots {
    for ((UnicodeQuot(l, r), _) <- QuotMarks.quotpairs)
      LOGGER.info(f"left: $l%s, right: $r%s")
  }
}