package scalautils

import java.io.{PrintStream, Serializable, FileReader, BufferedReader}
import java.lang.Boolean
import java.nio.ByteBuffer
import java.nio.charset.Charset


object QuotMarks {

  private val quotpairs = getQuotPairs();

  private case class UnicodeQuot(left : String, right : String)
  private case class HTMLQuot(left : String, right : String)

  private def getQuotPairs() : List[(UnicodeQuot, HTMLQuot)] = {
    
    var retval : List[(UnicodeQuot, HTMLQuot)] = Nil; //new ArrayList[(UnicodeQuot,HTMLQuot)]();
    var unicode : Int = 1
    var html : Int = 1
    
    val comment = (s : String) => s.trim.startsWith("#")
    val params = (s : String) => s.contains("=")
    val toTupes = (s : String) =>
      s.split("\\s+").map((s1 : String) => {
        val tupe = s1.split("=")
        (tupe(0), tupe(1))
      })
    
    val reader = new BufferedReader(new FileReader(".metadata/quots"))
    var line = reader.readLine
    
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
          retval = ((UnicodeQuot(unileft, uniright), HTMLQuot(htmlleft, htmlright)))::retval;
        }
      }
      line = reader.readLine()
    }
    return retval
  }

  def isA(quot : String) : java.lang.Boolean = {
    for (quotpair <- quotpairs) {
      quotpair match {
        case (UnicodeQuot(unileft, uniright), _) => {
          if (quot.equals(unileft) || quot.equals(uniright))
            return true
        }
        case _ => ???
      }
    }
    return false
  }
  
  def startingQuot (quot : String) : String = {
    val maxQuotLength = quotpairs.map((t : (UnicodeQuot, HTMLQuot)) => t._1.left.length).foldLeft(0) { (l1, l2) => 
      if (l2 > l1) l2 else l1 }
    for (i <- 0 to maxQuotLength) {
      if (i >= quot.length)
        return ""
      else if (isA(quot.substring(0, i)))
        return quot.substring(0,i)
    }
    return ""
  }
  
  def endingQuot (quot : String) : String = {
    val maxQuotLength = quotpairs.map((t : (UnicodeQuot, HTMLQuot)) => t._1.right.length).foldLeft(0) { (l1, l2) =>
      if (l2 > l1) l2 else l1 }
    for (i <- 0 to maxQuotLength) {
      if (i >= quot.length)
        return ""
      else if (isA(quot.substring(quot.length-i, quot.length)))
        return quot.substring(quot.length-i, quot.length)
    }
    return ""
  }

  def getMatch(quot : String) : List[java.lang.String] = {
    var retval = List[java.lang.String]()
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