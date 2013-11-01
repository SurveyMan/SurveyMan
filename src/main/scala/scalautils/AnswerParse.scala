package scalautils

import survey._
import com.amazonaws.mturk.requester.Assignment
import scala.xml._
import scala.collection.JavaConversions._
import java.util.ArrayList
import java.lang.Integer

case class Response(quid : String, qIndexSeen : Integer, opts : java.util.List[OptData])

case class OptData(optid : String, optIndexSeen : Integer)

object AnswerParse {
 
  def parse(survey : Survey, a : Assignment) : ArrayList[Response] = {
    val retval = new ArrayList[Response]
    val xmlAnswer = XML.loadString(a.getAnswer())
    for (answer <- (xmlAnswer \\ "Answer")) {
      val quid = (answer \\ "QuestionIdentifier").text
      val opts = (answer \\ "FreeText").text
      if (quid.equals("commit") && opts.equals("Submit"))
        println("WARNING: Submit button is being returned")
      else {
        val optList : Seq[String] = opts.split("\\|")
        //println(optList)
        val optDataList : ArrayList[OptData] = new ArrayList
        var qIndexSeen : Integer = -1
        for (opt <- optList) {
          val optstuff : Seq[String] = opt.split(";")
          //println("OPTSTUFF:", optstuff)
          if (optstuff.length == 3) {
            if (qIndexSeen == -1)
              qIndexSeen = Integer.parseInt(optstuff.get(1))
            optDataList.add(new OptData(optstuff.get(0), Integer.parseInt(optstuff.get(2))))
          } else optDataList.add(new OptData(optstuff.get(0), -1))
        }
        retval.add(new Response(quid, qIndexSeen, optDataList))
      }
    }
    return retval
  }

}