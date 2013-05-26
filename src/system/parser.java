import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Scanner;
import java.io.File;

public class parser
{
    String regex = "\\s*\"([^\"]*)\"|\\s*\\[\\[([^\\[\\]]*)\\]\\]\\s*|\\s*\\[([^\\[\\]]*)\\]|(?<=, ?|^)([^,]*)(?:, ?|$)";
    private final Pattern csvPattern = Pattern.compile(regex);
    private ArrayList<String> allMatches = null;
    private Matcher matcher = null;
    private String match = null;
    private int size;

    public parser()
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

    static void process(String[] parsedData)
    {
        int block = -1;
        String questionText = "";
        String resource = "";
        List<String> options = new ArrayList<String>();
        boolean exclusive=true, ordered=true, perturb=true;
        if (parsedData.length < 1)
        {
            return;
        }
        if (parsedData.length > 0 && !parsedData[0].equals(""))
        {
            try
            {
                block = Integer.parseInt(parsedData[0]);
            }
            catch (java.lang.NumberFormatException e)
            {
                block = -1;
            }
        }
        if (parsedData.length > 1 && !parsedData[1].equals(""))
        {
            questionText = parsedData[1];
        }
        if (parsedData.length > 2 && !parsedData[2].equals(""))
        {
            resource = parsedData[2];
        }
        if (parsedData.length > 3 && !parsedData[3].equals(""))
        {
            options = Arrays.asList(parsedData[3].split(",| ,"));
        }
        if (parsedData.length > 4 && !parsedData[4].equals(""))
        {
            if (parsedData[4].equalsIgnoreCase("no") || parsedData[4].equalsIgnoreCase("n"))
            {
                exclusive = false;
            }
        }
        if (parsedData.length > 5 && !parsedData[5].equals(""))
        {
            if (parsedData[5].equalsIgnoreCase("no") || parsedData[5].equalsIgnoreCase("n"))
            {
                ordered = false;
            }
        }
        if (parsedData.length > 6 && !parsedData[6].equals(""))
        {
            if (parsedData[6].equalsIgnoreCase("no") || parsedData[6].equalsIgnoreCase("n"))
            {
                perturb = false;
            }
        }
        System.out.println("Block: " + block);
        System.out.println("Question text: " + questionText);
        System.out.println("Resource: " + resource);
        System.out.println("Options:");
        System.out.println(options);
        System.out.println("Exclusive? " + exclusive);
        System.out.println("Ordered? " + ordered);
        System.out.println("Perturb? " + perturb);
        System.out.println();
        return;
    }
    
    public static void main(String[] args)
    {
        String fileContents = "";
        String[] lines;
        List<String> toQuestion = new ArrayList<String>();
        String[] parsedData;
        if (args.length == 0)
        {
            System.out.println("Usage: java parser.java [file path]");
            System.exit(0);
        }
        
        parser csvParser = new parser();
        try
        {
            fileContents = csvParser.readFile(args[0]);
        }
        catch (java.io.FileNotFoundException e)
        {
            System.out.println("Invalid filename");
        }
        lines = csvParser.split(fileContents);
        for (int i = 0; i < lines.length; i++)
        {
            parsedData = csvParser.parse(lines[i]);
            if (toQuestion.size() == 0 || (parsedData.length > 0 && !parsedData[1].equals(toQuestion.get(1))))
            {
                String[] q = toQuestion.toArray(new String[toQuestion.size()]);
                System.out.println(toQuestion);
                process(q);
                toQuestion = Arrays.asList(parsedData);
            }
            else
            {
                if (toQuestion.size() > 3 && parsedData.length > 3)
                {
                    toQuestion.set(3, toQuestion.get(3) + "," + parsedData[3]);
                }
                else
                {
                    System.out.println("Data in wrong format. Check line " + i + 1 + " .");
                    System.exit(0);
                }
            }
        }
    }
}
