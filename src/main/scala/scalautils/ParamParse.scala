package scalautils

import java.util.HashMap

object ParamParse {
  
  def getMap() : java.util.HashMap[String, String]= {
    new HashMap[String, String]()
  }
}