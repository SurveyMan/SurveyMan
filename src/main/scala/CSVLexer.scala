import scala.io.BufferedSource
import java.io.FileInputStream
import java.io.EOFException

object CSVLexer {

  var encoding = 'UTF8
  var counter = 0
  def inc() = { counter+=1 ; counter }
  val quotMarks = Map('UTF8 -> List(new String(Character.toChars(0x22)), new String(Character.toChars(0x27))*2))
  val knownHeaders = List('QUESTION, 'BLOCK, 'OPTIONS, 'RESOURCE, 'EXCLUSIVE, 'ORDERED, 'PERTURB)

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
    // if there's a header, set it to upper and make it a symbol
    // if there's no header, generate a symbol
    val headers = line.split( "," ).map((s : String) => 
      Symbol(if (s == "") "gen"+inc() else s.replaceAll("\"", "").trim.toUpperCase))
    headers.foreach((header : Symbol) =>
      if (! knownHeaders.contains(header) )
        println(f"WARNING: Column header $header%s has unknown semantics. See README for more info."))
    return headers
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
        case 'QUESTION => lexText(line, startIndex)
        case 'BLOCK => lexNum(line, startIndex)
        case 'OPTIONS => lexOptions(line, startIndex)
        case 'RESOURCE => lexResource(line, startIndex)
        case 'EXCLUSIVE | 'ORDERED | 'PERTURB => lexBin(line, startIndex)
        case unknown => lexText(line, startIndex)
      }
      startIndex += lexeme.length
      lexed(col) = (lexeme, uid)
    }
    return lexed
  }

  def lexBin(line : String, startIndex : Int) : String = {
    lexText(line, startIndex)
  }

  def lexResource(line : String, startIndex : Int) : String = {
    lexText(line, startIndex)
  }

  def lexOptions(line : String, startIndex : Int) : String = {
    lexText(line, startIndex)
  }

  def lexNum(line : String, startIndex : Int) : String = {
    // blocks are numerically ordered (no alphas for now)
    // if properly formed, will end with a comma or a newline    
    val blockFormat = "\\s*[0-9](\\.[0-9])*\\s*[,\n]".r
    val block = blockFormat findFirstMatchIn line.substring(startIndex)
    block match {
      case None => throw new RuntimeException("Badly formed block match starting with : [" + line.substring(startIndex) + "]")
      case Some(txt) => txt.matched //not stripping out comma yet
    }
  }

  def lexText(line : String, startIndex : Int) : String = {
    // thought this would be more legible than a regex
    val qtext = new StringBuffer("")
    var currIndex = startIndex
    var inQuot = false
    var quotMark : String = null
    val addAndAdvance = {
      (c : Char) => 
        qtext.append(c) ; currIndex += 1
    }
    val pushQuot = { 
      (q : String) => 
        if ( ! inQuot ) {
          inQuot = true
          quotMark = q
        } else if ( quotMark == q ) inQuot = false
    }
    val ignore = (i : Int) => line(i)=='\\'

    while ( currIndex < line.length ) {
      line(currIndex) match {
        case ',' => addAndAdvance(',') ; if (! inQuot) return qtext.toString
        case '\'' => {
          addAndAdvance('\'')
          if (! ignore(currIndex-2) ) {
            if ( line(currIndex) == '\'' ) {
              addAndAdvance('\'')
              pushQuot("'")
            }
          }
        }
        case '"' => {
          addAndAdvance('"')
          if (! ignore(currIndex-2) ) {
	    // double quotation marks are sometimes used as escape characters
	     if ( line(currindex) == '"' && inQuot ) 
	      addAndAdvance('"')
	    else pushQuot("\"")
        }
        case c => addAndAdvance(c)
      }
    }
    if ( inQuot )
      ???
    else return qtext.toString
  }

  def getEntries(filename : String) : Array[List[(String, Int)]] = {
    // set encoding from...somewhere?
    // assert that there is an encoding value in the quotes map
    val bs = new BufferedSource(new FileInputStream(filename))
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
          entries(i) = lexed(i)::entries(i) //ugh
        line = readLine(bs)
      }
    } while ( line != "" )
      return entries
  }

  def main(args : Array[String]) {
    getEntries(args(0))
  }

}
