package system;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Scanner;
import java.io.File;

public class Parser
{
    String regex = "\\s*\"([^\"]*)\"|\\s*(\\[\\[[^\\[\\]]*\\]\\])\\s*|\\s*\\[([^\\[\\]]*)\\]|\\s?(?<=, ?|^)([^,]*)(?:, ?|$)";
    private final Pattern csvPattern = Pattern.compile(regex);
    private ArrayList<String> allMatches = null;
    private Matcher matcher = null;
    private String match = null;
    private int size;

    public Parser()
    {
        allMatches = new ArrayList<String>();
        matcher = null;
        match = null;
    }
    
    public String[] parse(String csv)
    {
        matcher = csvPattern.matcher(csv);
        allMatches.clear();
        String match;
        while (matcher.find()) {
            match = matcher.group(1);
            if (matcher.group(1)!=null)
            {
                allMatches.add(match);
            }
            else if (matcher.group(2)!=null)
            {
                allMatches.add(matcher.group(2));
            }
            
            else if (matcher.group(3)!=null)
            {
                allMatches.add(matcher.group(3));
            }
            else
            {
                allMatches.add(matcher.group(4));
            }
        }
        size = allMatches.size();
        if (size > 0)
        {
            return allMatches.toArray(new String[size]);
        }
        else
        {
            return new String[0];
        }
    }

    static String readFile(String path)
        throws java.io.FileNotFoundException
    {
        return new Scanner(new File(path)).useDelimiter("\\A").next();
    }

    static String[] split(String file)
    {
        String[] lines = file.split("\\r?\\n");
        return lines;
    }
    
    public static void main(String[] args)
    {
        WebGenerator generator = new WebGenerator();
        String fileContents = "";
        String previewContents = "";
        int numSurveys = 0;
        String[] lines;
        List<String> toQuestion = new ArrayList<String>();
        ArrayList<String[]> allQuestions = new ArrayList<String[]>();
        String[] parsedData;
        if (args.length < 3)
        {
            System.out.println("Usage: java Parser.java [file path] [preview form path] [number of surveys to create]");
            System.exit(0);
        }
        
        Parser csvParser = new Parser();
        try
        {
            System.out.println(args[0]);
            System.out.println(args[1]);
            fileContents = csvParser.readFile(args[0]);
            previewContents = csvParser.readFile(args[1]);
        }
        catch (java.io.FileNotFoundException e)
        {
            System.out.println("Invalid filename");
            System.exit(0);
        }
        try
        {
            numSurveys = Integer.parseInt(args[2]);
        }
        catch (java.lang.NumberFormatException e)
        {
            System.out.println("Invalid number of surveys");
            System.exit(0);
        }
        lines = csvParser.split(fileContents);
        for (int i = 0; i < lines.length; i++)
        {
            parsedData = csvParser.parse(lines[i]);
            boolean listSeparate = false;
            if (parsedData.length > 7)
            {
                if (parsedData[7].equals("yes") || parsedData[7].equals("y"))
                {
                    listSeparate = true;
                }
            }    
            // If the line that was just parsed is for a new question, we are done with the last question. Add the last question to allQuestions
            if (toQuestion.size() == 0 || (parsedData.length > 0 && !parsedData[1].equals(toQuestion.get(1)) || listSeparate))
            {
                String[] q = toQuestion.toArray(new String[toQuestion.size()]);
                if (i != 0 && i != 1)
                {
                    allQuestions.add(q);
                }
                toQuestion = Arrays.asList(parsedData);
            }
            // If we're not done with the last question, add the option to toQuestion.
            else
            {
                if (toQuestion.size() > 3 && parsedData.length > 3)
                {
                    toQuestion.set(3, toQuestion.get(3) + "," + parsedData[3]);
                }
                else
                {
                    System.out.println("Data in wrong format. Check line " + (i + 1) + " .");
                    System.exit(0);
                }
            }
        }
        // Add last question to allQuestions
        String[] q = toQuestion.toArray(new String[toQuestion.size()]);
        allQuestions.add(q);
        generator.generateSurvey(allQuestions, previewContents, numSurveys);
    }
}
