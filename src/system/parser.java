import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;

public class parser
{
    String regex = "\\s*\"([^\"]*)\"|\\s*\\[\\[([^\\[\\]]*)\\]\\]\\s*|\\s*\\[([^\\[\\]]*)\\]|\\s*(?<=,|^)([^,]*)(?:,|$)";
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
    
    public static void main(String[] args)
    {
        String fileContents = "";
        String[] lines;
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
            System.out.println("Testing parser with " + lines[i]);
            for (String s : csvParser.parse(lines[i]))
            {
                System.out.println(s);
            }
        }
    }
}
