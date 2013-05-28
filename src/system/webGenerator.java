import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class webGenerator
{

    static ArrayList<String []> randomize(ArrayList<String []> questions)
    {
        return questions;
    }

    static String generateScaleQuestion(int uid, String text, String resource, String[] options, boolean exclusive, boolean ordered, boolean perturb)
    {
        String HTMLString = "<div id=question"+Integer.toString(uid)+">";
        HTMLString+=text;
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
                HTMLString+="<option>"+options[i]+"</option>";
            }
            HTMLString+="</select>";
        }
        else if (exclusive)
        {
            for (int i = 0; i < options.length; i++)
            {
                HTMLString+="<input type=\"radio\" name=\"q"+Integer.toString(uid)+"\" value=\"option"+Integer.toString(i)+"\">";
                HTMLString+=options[i];
            }
        }
        else
        {
            for (int i = 0; i < options.length; i++)
            {
                HTMLString+="<input type=\"checkbox\" name=\"q"+Integer.toString(uid)+"\" value=\"option"+Integer.toString(i)+"\">";
                HTMLString+=options[i];
            }
        }
        HTMLString+="<br><input type=\"button\" name=\"prev\" value=\"Previous\">";
        HTMLString+="<input type=\"button\" name=\"next\" value=\"Next\">";
        HTMLString+="</div>";
        return HTMLString;
    }

    static String generateRadioButtonQuestion(String text, String resource, String[] options, boolean exclusive, boolean ordered, boolean perturb)
    {
        String HTMLString = "";
        return HTMLString;
    }

    static String generateCheckboxQuestion(String text, String resource, String[] options, boolean exclusive, boolean ordered, boolean perturb)
    {
        String HTMLString = "";
        return HTMLString;
    }

    static String generateDropdownQuestion(String text, String resource, String[] options, boolean exclusive, boolean ordered, boolean perturb)
    {
         String HTMLString = "";
        return HTMLString;
    }

    static String generateFreeTextQuestion(String text, String resource)
    {
        String HTMLString = "";
        return HTMLString;
    }

    static String generateQuestion(int uid, String[] question)
    {
        int block = -1;
        String questionText = "";
        String resource = "";
        List<String> options = new ArrayList<String>();
        boolean exclusive=true, ordered=true, perturb=true;
        for (int i = 0; i < question.length; i++)
        {
            System.out.println(question[i]);
        }
        System.out.println();
        if (question.length < 1)
        {
            return "Error";
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
                                System.out.println("start: " + start);
                                System.out.println("end: " + end);
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
        return generateScaleQuestion(uid, questionText, resource, options.toArray(new String[options.size()]), exclusive, ordered, perturb);
    }

    static void generateSurvey(ArrayList<String[]> questions)
    {
        try
        {
        
            PrintWriter out = new PrintWriter(new FileWriter("survey.html"));
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<script type=\"text/javascript\" src=\"http://jqueryjs.googlecode.com/files/jquery-1.3.2.min.js\" charset=\"utf-8\"></script>");
            out.println("<script type=\"text/javascript\">");

            out.println("\t$(document).ready(function() {");
            out.println("\t\t$('div[id^=\"question\"]').addClass('questionDiv').hide();");
            out.println("\t\t$('.questionDiv:first').show();");

                // A listener on the radios to hide the
                // clicked question and show the next question
            out.println("\t\t$('input[name=\"next\"]').click(function(){");
            out.println("\t\t\tif($(this).parents('.questionDiv').nextAll('.questionDiv').eq(0).length > 0) {");
            out.println("\t\t\t\t$(this).parents('.questionDiv').hide();");
            out.println("\t\t\t\t$(this).parents('.questionDiv').nextAll('.questionDiv').eq(0).show();");
            out.println("\t\t\t}");
            out.println("\t\t});");
            out.println("\t\t$('input[name=\"prev\"]').click(function(){");
            out.println("\t\t\tif($(this).parents('.questionDiv').prevAll('.questionDiv').eq(0).length > 0) {");
            out.println("\t\t\t\t$(this).parents('.questionDiv').hide();");
            out.println("\t\t\t\t$(this).parents('.questionDiv').prevAll('.questionDiv').eq(0).show();");
            out.println("\t\t\t}");
            out.println("\t\t});");
            out.println("\t});");
            out.println("</script>");
            out.println("</head>");
            out.println("<body>");
            out.println("<form>");
            int count = 0;
            questions = randomize(questions);
            for (String[] question : questions)
            {
                count++;
                out.println(generateQuestion(count, question));
            }
            out.println("</form>");
            out.println("</body>");
            out.println("</html>");
            out.flush();
        }
        catch (java.io.IOException e)
        {
            System.out.println("Could not create output file.");
            System.exit(0);
        }
        return;
    }
}
