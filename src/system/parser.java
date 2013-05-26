import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;

public class parser
{
    String regex = "\"([^\"]*)\"|\\[\\[([^\\[\\]]*)\\]\\]|\\[([^\\[\\]]*)\\]|(?<=,|^)([^,]*)(?:,|$)";
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
    
    public static void main(String[] args)
    {
        //String lineinput = "1,\"Would you describe the following image as 'cute'?\",/path/to/cherublic_child.jpg,[Yes, No, I don't understand the question],yes,no,yes";
        if (args.length == 0)
        {
            System.out.println("Usage: java parser.java [file path]");
            System.exit(0);
        }
        
        String lineinput = "2,\"What year \nwere you born?\",,[[1920-1993]],yes,yes,no";

        parser csvParser = new parser();
        try
        {
            System.out.println(csvParser.readFile(args[0]));
        }
        catch (java.io.FileNotFoundException e)
        {
            System.out.println("Invalid filename");
        }
        System.out.println("Testing parser with: \n " + lineinput);
        for (String s : csvParser.parse(lineinput))
        {
            System.out.println("'"+s+"'");
        }
    }
}
