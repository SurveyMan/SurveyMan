import scala.io.BufferedSource
import java.io.FileInputStream
import java.io.EOFException

object CSVLexer {

  var encoding = 'UTF8
  var counter = 0
  def inc() = { counter+=1 ; counter }
  val quotMarks = Map('UTF8 -> List(new String(Character.toChars(0x22)), new String(Character.toChars(0x27))*2))

  def getQuotmark(line : String) : Option[String] = {
    for ( quotMark <- quotMarks.get(encoding).orNull )
      if ( line startsWith quotMark )
        return Option(quotMark)
    return None
  }

  def readLine(bs : BufferedSource) : String =  {
    try {
      new String(bs.takeWhile((c : Char) => c != '\n').toArray)
    } catch {
      case eof : EOFException => ""
    }
  }

  def getHeaders(line : String) : Array[Symbol] = {
    // if there's a header, set it to lower and make it a symbol
    // if there's no header, generate a symbol
    line.split(",").map((s : String) => 
      Symbol(if (s == "") "gen"+inc() else s.replaceAll("\"", "").trim.toUpperCase))
  }

  def easyLex(line : String, headers : Array[Symbol]) : Array[(String, Int)] = {
    val split = line.split(",")
    if (split.length == headers.length) {
      var lexed = new Array[(String, Int)](headers.length)
      val uid = inc()
      for (i <- 0 until lexed.length)
        lexed(i) = (split(i), uid)
      lexed
    } else new Array[(String, Int)](0)
  }

  def canBeEasyLexed(lexed : Array[(String, Int)]) : Boolean = lexed.length != 0

  def lex(line : String, headers : Array[Symbol]) : Array[(String, Int)] = {
    var lexed = new Array[(String, Int)](headers.length)
    var startIndex = 0
    val uid = inc()
    for (col <- 0 until headers.length) {
      val lexeme = headers(col) match {
        case 'QUESTION => lexQuestion(line, startIndex)
        case _ => ""
      }
      startIndex += lexeme.length
      lexed(col) = (lexeme, uid)
    }
    lexed
  }

  def lexQuoted(line : String, startIndex : Int, quotStr : String) : String = {
    var endIndex = startIndex + 1
    for ( i <- 0 until line.length ) {
      if ( line(endIndex).toString == quotStr && line(endIndex-1).toString != '\\')
        return line.substring(startIndex, endIndex+1)
    }
    ???
  }

  def lexQuestion(line : String, startIndex : Int) : String = {
    println(line)
    val qtext : StringBuffer = new StringBuffer("")
    var currIndex = startIndex
    while (true) {
      println(qtext)
      getQuotmark(line) match {
        case Some(quotTxt) => qtext.append(lexQuoted(line, startIndex, quotTxt))
        case None => {
          val tmp = line.substring(currIndex).takeWhile( (c : Char) => ! (c == ',' || c == '\'' || c == '"') )
          currIndex += tmp.length
          qtext.append(tmp)
          line(currIndex) match {
            case ',' => return qtext.toString
            case '\'' => if ( line(currIndex+1) != '\'' ) { qtext.append('\'') ; currIndex += 1 }
          }
        }
      }
    }
    ???
  }

  def main(args : Array[String]) {
    // set encoding from...somewhere?
    // assert that there is an encoding value in the quotes map
    val bs = new BufferedSource(new FileInputStream(args(0)))
    var line = ""
    var headers : Array[Symbol] = null 
    var entries : Array[List[(String, Int)]] = null
    do {
      if ( line == "" ) {
        line = readLine(bs)
        headers = getHeaders(line)
        entries = Array.fill( headers.length ) { List[(String, Int)]() }
      } else {
        var lexed : Array[(String, Int)] = easyLex(line, headers)
        if ( ! canBeEasyLexed(lexed) )
          lexed = lex(line, headers)
        for ( i <- 0 until entries.length )
          lexed(i)::entries(i)
        line = readLine(bs)
      }
    } while ( line != "" )
      // print out entries parsed thus far
    for (i <- 0 until entries.length)
      println(headers(i)+":"+entries(i))
  }
}
