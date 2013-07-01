package scalautils

import java.util.HashMap
import java.io.{BufferedReader, FileReader}

object ParamParse {
  
  def getMap() : java.util.HashMap[String, String]= {
    
    var retval = new HashMap[String, String]()

    val comment = (s : String) => s.trim.startsWith("#")
    val params = (s : String) => s.contains("=")
    val reader = new BufferedReader(new FileReader("params"))
    var line = reader.readLine
    
    while (line!=null) {
      if (! comment(line) && params(line)) {
        val kv = line.split("=")
        retval.put(kv(0).trim, kv(1).trim)
      }
      line = reader.readLine
    }
    
    return retval
  }
}