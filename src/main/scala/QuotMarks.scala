import java.io.{PrintStream, Serializable, FileReader, BufferedReader}
import java.lang.Boolean
import java.nio.ByteBuffer
import java.nio.charset.Charset


object QuotMarks {

  private val quotpairs = getQuotPairs();

  private case class UnicodeQuot(left : String, right : String)
  private case class HTMLQuot(left : String, right : String)

  private def getQuotPairs() : List[(UnicodeQuot, HTMLQuot)] = {
    var retval : List[(UnicodeQuot, HTMLQuot)] = Nil;
    var unicode : Int = 1
    var html : Int = 1
    val reader = new BufferedReader(new FileReader(".metadata/quots"))
    var line = ""
    val comment = (s : String) => s.trim.startsWith("#")
    val params = (s : String) => s.contains("=")
    val toTupes = (s : String) =>
      s.split("\\s+").map((s1 : String) => {
        val tupe = s1.split("=")
        (tupe(0), tupe(1))
      })
    line = reader.readLine()
    while(line != null) {
      if (! comment(line) && ! line.equals("") ) {
        if (params(line))
          for ((k, v) <- toTupes(line)) {
            k match {
              case "unicode" => unicode = Integer.parseInt(v)
              case "html" => html = Integer.parseInt(v)
              case _ => throw new RuntimeException(f"Unrecognized parameter $k%s")
            }
          }
        else {
          val encodings = line.split("\\s+")
          val unileft = encodings.slice(0, unicode).map((s : String) =>
            Character.toChars(Integer.decode(s))).foldLeft("") { (s1, s2) =>
              s1 + new String(s2)
          }
          val uniright = encodings.slice(unicode, unicode*2).map((s : String) =>
            Character.toChars(Integer.decode(s))).foldLeft("") { (s1, s2) =>
              s1 + new String(s2)
          }
          val htmlleft = encodings.slice(unicode*2, unicode*2+html).foldLeft("") { (s1, s2) =>
            s1 + " " + new String(s2)
          }
          val htmlright = encodings.slice(unicode*2+html, unicode*2+html*2).foldLeft("") { (s1, s2) =>
            s1 + " " + new String(s2)
          }
          retval = (UnicodeQuot(unileft, uniright), HTMLQuot(htmlleft, htmlright))::retval
        }
      }
      line = reader.readLine()
    }
    return retval
  }

  def isA(quot : String) : java.lang.Boolean = {
    for ((UnicodeQuot(unileft, uniright), _) <- quotpairs) {
      if (quot.equals(unileft) || quot.equals(uniright))
        return true;
    }
    return false
  }

  def getMatch(quot : String) : List[String] = {
    var retval = List[String]()
    for ((UnicodeQuot(unileft, uniright), _) <- quotpairs) {
      if (quot.equals(unileft))
        retval = uniright::retval
    }
    if (retval.length == 0)
      throw new RuntimeException("Quot mark $quot%s matches no known left quot mark.")
    else return retval
  }

  def getLeftHTML(left : String, right : String) : java.lang.String = {
    for ((UnicodeQuot(unileft, uniright), HTMLQuot(htmlleft, htmlright)) <- quotpairs) {
      if (left.equals(unileft) && right.equals(uniright))
        return htmlleft
    }
    throw new RuntimeException("Quot mark $quot%s matches no known left quot mark.")
  }

  def getRightHTML(left : String, right : String) : java.lang.String = {
    for ((UnicodeQuot(unileft, uniright), HTMLQuot(htmlleft, htmlright)) <- quotpairs) {
      if (left.equals(unileft) && right.equals(uniright))
        return htmlright
    }
    throw new RuntimeException("Quot mark $quot%s matches no known left quot mark.")
  }

  def main(args : Array[String]) {
    val stdout : PrintStream = new PrintStream(System.out, true, "UTF-8")
    for ((UnicodeQuot(l, r), _) <- quotpairs)
      stdout.println(f"left: $l%s, right: $r%s")
  }
}