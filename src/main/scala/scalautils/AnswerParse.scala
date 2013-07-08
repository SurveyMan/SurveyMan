package scalautils

import survey._
import com.amazonaws.mturk.requester.Assignment
import scala.xml._
import scala.collection.JavaConversions._
import java.util.ArrayList
import java.lang.Integer

case class Response(quid : String, opts : java.util.List[String], indexSeen : Integer);

object AnswerParse {
 
  def parse(survey : Survey, a : Assignment) : ArrayList[Response] = {
    val retval = new ArrayList[Response]
    val xmlAnswer = XML.loadString(a.getAnswer())
    var qindexseen = 0
    for (answer <- (xmlAnswer \\ "Answer")) {
      val quid = (answer \\ "QuestionIdentifier").text
      val opts = (answer \\ "FreeText").text
      if (quid.equals("commit") && opts.equals("Submit"))
        println("WARNING: Submit button is being returned")
      else {
        val optList : Seq[String] = opts.split("\\|")
        println("optList: "+optList)
        retval.add(new Response(quid, optList, qindexseen))
        qindexseen += 1
      }
    }
    return retval
  }

}