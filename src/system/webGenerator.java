import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

public class webGenerator
{

    static ArrayList<String []> randomize(ArrayList<String []> questions)
    {
        ArrayList<Integer> blockNumbers = new ArrayList<Integer>();
        ArrayList<String []> allQuestions = new ArrayList<String []>();
        ArrayList<String []> randomQuestions = new ArrayList<String []>();
        int block = -1;
        Random generator = new Random();
        // First, get all the block numbers
        for (String[] question: questions)
        {
            if (question.length > 0)
            {
                try
                {
                    block = Integer.parseInt(question[0]);
                }
                catch (java.lang.NumberFormatException e)
                {
                    block = -1;
                    question[0] = "-1";
                }
                if (!blockNumbers.contains(block))
                {
                    blockNumbers.add(block);
                }
            }
        }
        // Sort the numbers
        Collections.sort(blockNumbers);
        // Go through all the blocks. For each question, if it is a member of this block, add it to an arrayList to be shuffled.
        for (int thisBlock: blockNumbers)
        {
            ArrayList<String []> blockQuestions = new ArrayList<String []>();
            for (String[] question: questions)
            {
                if (question.length > 0)
                {
                    if (Integer.parseInt(question[0]) == thisBlock && thisBlock != -1)
                    {
                        blockQuestions.add(question);
                    }
                    else if (thisBlock == -1)
                    {
                        randomQuestions.add(question);
                    }
                }
            }
            // Shuffle all the questions in this block
            Collections.shuffle(blockQuestions);
            // Add the newly shuffled questions to the final array
            allQuestions.addAll(blockQuestions);
        }
        // Lastly, address the questions that can go anywhere. Randomly place them in the array.
        if (randomQuestions.size() > 0)
        {
            for (String[] randomQuestion: randomQuestions)
            {
                int randomIndex = generator.nextInt(allQuestions.size());
                allQuestions.add(randomIndex, randomQuestion);
            }
        }
        // Print the questions (for testing)
        for (String[] test : allQuestions)
        {
            for (int i = 0; i < test.length; i++)
            {
                System.out.print(test[i] + ", ");
            }
            System.out.println();
        }
        // Return the final array
        return allQuestions;
    }

    static String replaceSpecialCharacters(String s)
    {
        return s.replaceAll("&", "&amp;").replaceAll("\"", "&quot;").replaceAll("'", "&apos;").replaceAll("<","&lt;").replaceAll(">","&gt;");
    }

    static String generateHTMLQuestion(int uid, String text, String resource, String[] options, boolean exclusive, boolean ordered, boolean perturb)
    {
        String HTMLString = "<div id=question"+Integer.toString(uid)+">";
        HTMLString+=replaceSpecialCharacters(text);
        if (!resource.equals(""))
        {
            if (resource.contains(".jpeg") || resource.contains(".jpg") || resource.contains(".gif") || resource.contains(".png"))
            {
                HTMLString+="<br><img src=\""+resource+"\"><br>";
            }
            else if (resource.contains(".wav") || resource.contains(".mp3") || resource.contains(".mpeg") || resource.contains(".ogg"))
            {
                HTMLString+="<br><embed src=\""+resource+"\"><br>";
            }
        }
        HTMLString+="<br>";
        if (options.length > 10)
        {
            HTMLString+="<select>";
            for (int i = 0; i < options.length; i++)
            {
                HTMLString+="<option>"+replaceSpecialCharacters(options[i])+"</option>";
            }
            HTMLString+="</select>";
        }
        else if (exclusive)
        {
            for (int i = 0; i < options.length; i++)
            {
                HTMLString+="<input type=\"radio\" name=\"q"+Integer.toString(uid)+"\" value=\"option"+Integer.toString(i)+"\">";
                HTMLString+=replaceSpecialCharacters(options[i]);
            }
        }
        else
        {
            for (int i = 0; i < options.length; i++)
            {
                HTMLString+="<input type=\"checkbox\" name=\"q"+Integer.toString(uid)+"\" value=\"option"+Integer.toString(i)+"\">";
                HTMLString+=replaceSpecialCharacters(options[i]);
            }
        }
        HTMLString+="<br><input type=\"button\" name=\"prev\" value=\"Previous\">";
        HTMLString+="<input type=\"button\" name=\"next\" value=\"Next\">";
        HTMLString+="<input type=\"submit\" name=\"submit\" value=\"Submit\">";
        HTMLString+="</div>";
        return HTMLString;
    }

    static String generateQuestion(int uid, String[] question)
    {
        int block = -1;
        String questionText = "";
        String resource = "";
        List<String> options = new ArrayList<String>();
        boolean exclusive=true, ordered=true, perturb=true;
        /*
        for (int i = 0; i < question.length; i++)
        {
            System.out.println(question[i]);
        }
        System.out.println();*/
        if (question.length < 1)
        {
            System.out.println("Error");
            System.exit(0);
        }
        if (question.length > 1 && !question[1].equals(""))
        {
            questionText = question[1];
        }
        if (question.length > 2 && !question[2].equals(""))
        {
            resource = question[2];
        }
        if (question.length > 3 && !question[3].equals(""))
        {
                List<String> temp = Arrays.asList(question[3].split(",| ,"));
                for (String option : temp)
                {
                    if (option.contains("[[") && option.contains("]]"))
                    {
                        String[] endpoints = option.split("-| -|- | - ");
                        if (endpoints.length == 2)
                        {
                            try
                            {
                                int start = Integer.parseInt(endpoints[0].replaceAll("[\\s\\[\\]]",""));
                                int end = Integer.parseInt(endpoints[1].replaceAll("[\\s\\[\\]]",""));
                                if (end > start)
                                {
                                    for (int i = start; i < end; i++)
                                    {
                                        options.add(Integer.toString(i));
                                    }
                                }
                                else
                                {
                                     for (int i = end; i > start; i--)
                                    {
                                        options.add(Integer.toString(i));
                                    }
                                }
                            }
                            catch (java.lang.NumberFormatException e)
                            {
                                System.out.println("Your ranges must be numeric values.");
                                System.exit(0);
                            }
                        }
                        else
                        {
                            options.add(option);
                        }
                    }
                    else
                    {
                        options.add(option);
                    }
                }
        }
        if (question.length > 4 && !question[4].equals(""))
        {
            if (question[4].equalsIgnoreCase("no") || question[4].equalsIgnoreCase("n"))
            {
                exclusive = false;
            }
        }
        if (question.length > 5 && !question[5].equals(""))
        {
            if (question[5].equalsIgnoreCase("no") || question[5].equalsIgnoreCase("n"))
            {
                ordered = false;
            }
        }
        if (question.length > 6 && !question[6].equals(""))
        {
            if (question[6].equalsIgnoreCase("no") || question[6].equalsIgnoreCase("n"))
            {
                perturb = false;
            }
            else
            {
                if (ordered == false)
                {
                    Collections.shuffle(options);
                }
                else
                {
                    Random generator = new Random();
                    int coin = generator.nextInt(2);
                    if (coin == 0)
                    {
                        Collections.reverse(options);
                    }
                }
            }
        }
        /*
        System.out.println("Block: " + block);
        System.out.println("Question text: " + questionText);
        System.out.println("Resource: " + resource);
        System.out.println("Options:");
        System.out.println(options);
        System.out.println("Exclusive? " + exclusive);
        System.out.println("Ordered? " + ordered);
        System.out.println("Perturb? " + perturb);
        System.out.println();
        */
        return generateHTMLQuestion(uid, questionText, resource, options.toArray(new String[options.size()]), exclusive, ordered, perturb);
    }

    static void generateSurvey(ArrayList<String[]> questions, String preview, int numSurveys)
    {
        Map<String[], Integer> identifiers = new HashMap<String[], Integer>();
        int count = 0;
        for (String [] question: questions)
        {
            count++;
            identifiers.put(question, count);
        }
        for (int ind = 0; ind < numSurveys; ind++)
        {
            try
            {
                PrintWriter out;
                File file;
                if (ind+1 < 10)
                {
                    file = new File("surveys/survey0" + (ind+1) + ".question");
                }
                else
                {
                    file = new File("surveys/survey" + (ind+1) + ".question");
                }
                file.getParentFile().mkdirs();
                out = new PrintWriter(new FileWriter(file));
                out.println("<HTMLQuestion xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2011-11-11/HTMLQuestion.xsd\">");
                out.println("<HTMLContent><![CDATA[");
                out.println("<!DOCTYPE html>");
                out.println("<html>");
                out.println("<head>");
                out.println("<script type=\"text/javascript\" src=\"https://jqueryjs.googlecode.com/files/jquery-1.3.2.min.js\" charset=\"utf-8\"></script>");
                out.println("<script type=\"text/javascript\" src=\"https://s3.amazonaws.com/mturk-public/externalHIT_v1.js\"></script>");
                out.println("<script type=\"text/javascript\">");

                out.println("\t$(document).ready(function() {");
                out.println("\t\tassignmentId = turkGetParam('assignmentId', \"\");");
                out.println("\t\tvar count = 0;");
                out.println("\t\t$('#preview').hide();");
                out.println("\t\t$(\"[name='submit']\").hide();");
                out.println("\t\t$('div[id^=\"question\"]').addClass('questionDiv').hide();");
                out.println("\t\tif (assignmentId==\"ASSIGNMENT_ID_NOT_AVAILABLE\") {");
                out.println("\t\t\t$('#preview').show();");
                out.println("\t\t}");
                out.println("\t\telse {");
                out.println("\t\t\t$('.questionDiv:first').show();");
                out.println("\t\t}");
                    // A listener on the radios to hide the
                    // clicked question and show the next question
                out.println("\t\t$('input[name=\"next\"]').click(function(){");
                out.println("\t\t\tif($(this).parents('.questionDiv').nextAll('.questionDiv').eq(0).length > 0) {");
		        out.println("\t\t\t\tcount = count + 1;");
		        out.println("\t\t\t\tif (count == $('.questionDiv').length - 1) {");
		        out.println("\t\t\t\t\t$(\"[name='next']\").hide();");
		        out.println("\t\t\t\t\t$(\"[name='submit']\").show();");
		        out.println("\t\t\t\t}");
                out.println("\t\t\t\t$(this).parents('.questionDiv').hide();");
                out.println("\t\t\t\t$(this).parents('.questionDiv').nextAll('.questionDiv').eq(0).show();");
                out.println("\t\t\t}");
                out.println("\t\t});");
                out.println("\t\t$('input[name=\"prev\"]').click(function(){");
                out.println("\t\t\tif($(this).parents('.questionDiv').prevAll('.questionDiv').eq(0).length > 0) {");
			    out.println("\t\t\t\tcount = count - 1;");
			    out.println("\t\t\t\t$(\"[name='next']\").show();");
			    out.println("\t\t\t\t$(\"[name='submit']\").hide();");
                out.println("\t\t\t\t$(this).parents('.questionDiv').hide();");
                out.println("\t\t\t\t$(this).parents('.questionDiv').prevAll('.questionDiv').eq(0).show();");
                out.println("\t\t\t}");
                out.println("\t\t});");
                out.println("\t\tvar warning = !(assignmentId==\"ASSIGNMENT_ID_NOT_AVAILABLE\");");
                out.println("\t\twindow.onbeforeunload = function() {");
                out.println("\t\t\tif(warning) {");
                out.println("\t\t\t\t return \"You have made changes on this page that you have not yet confirmed. If you navigate away from this page you will lose your unsaved changes\";");
                out.println("\t\t\t}");
                out.println("\t\t}");
                out.println("\t\t$('form').submit(function() {");
                out.println("\t\t\twindow.onbeforeunload = null");
                out.println("\t\t});");
                out.println("\t});");
                out.println("</script>");
                out.println("</head>");
                out.println("<body>");
                out.println("<form name='mturk_form' method='post' id='mturk_form' action='https://www.mturk.com/mturk/externalSubmit'>");
                out.println("<div id=preview>");
                out.println(preview);
                out.println("</div>");
                out.println("<input type='hidden' value='' name='assignmentId' id='assignmentId'/>");

                questions = randomize(questions);
                for (String[] question : questions)
                {
                    out.println(generateQuestion(identifiers.get(question), question));
                }
                out.println("</form>");
                out.println("<script language='Javascript'>turkSetAssignmentID();</script>");
                out.println("</body>");
                out.println("</html>");
                out.println("]]>");
                out.println("</HTMLContent>");
                out.println("<FrameHeight>450</FrameHeight>");
                out.println("</HTMLQuestion>");
                out.flush();
            }
            catch (java.io.IOException e)
            {
                System.out.println("Could not create output file.");
                System.exit(0);
            }
        }
        return;
    }
}
