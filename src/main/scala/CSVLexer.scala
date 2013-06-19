import scala.io.BufferedSource
import java.io.FileInputStream
import java.io.EOFException

object CSVLexer {

  var counter = 0
  def inc() = { counter+=1 ; counter }

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
    line.split(",").map((s : String) => Symbol(if (s == "") "gen"+inc() else s.toLowerCase))
  }
  
  def main(args : Array[String]) {
    val encoding = ""
    val bs = new BufferedSource(new FileInputStream(args(0)))
    var line = readLine(bs)
    // process first line
    val headers : Array[Symbol] = getHeaders(line)
    do {
      line = readLine(bs)
      // split each remaining line as data of the associated type
      print(line)
    } while ( line != "")
  }

}
