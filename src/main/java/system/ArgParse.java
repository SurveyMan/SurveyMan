package system;

import java.util.*;

public class ArgParse {
    public static enum ArgType {
        KEYVALUE,   // options of the type --foo=bar
        KEY         // options of the type --foo
    }

    private static int PADDING = 2;
    private static String DEBUG = "debug-arg-parser";
    private String _program_name;
    private List<String> _m_args;
    private HashSet<String> _kv_options;
    private HashSet<String> _k_options;
    private HashSet<String> _h_options; // these options will be omitted from usage
    private HashMap<String,String> _arg_usage;
    private HashMap<String,String> _defaults;
    private String _custom_text;

    public ArgParse(String program_name,
                    List<String> mandatory_args,
                    HashMap<String,ArgType> optional_flags,
                    HashMap<String,String> arg_usage,
                    HashMap<String,String> defaults,
                    String custom_text,
                    HashSet<String> hidden_args) {
        this(program_name, mandatory_args, optional_flags, arg_usage, defaults, custom_text);
        _h_options.addAll(hidden_args);
    }

    public ArgParse(String program_name,
                    List<String> mandatory_args,
                    HashMap<String,ArgType> optional_flags,
                    HashMap<String,String> arg_usage,
                    HashMap<String,String> defaults,
                    String custom_text) {
        _kv_options = new HashSet<String>();
        _k_options = new HashSet<String>() {{
            add(DEBUG);
        }};
        _h_options = new HashSet<String>() {{
            add(DEBUG);
        }};

        _program_name = program_name;
        _m_args = mandatory_args;
        _arg_usage = arg_usage;
        _defaults = defaults;

        for (Map.Entry<String,ArgType> pair : optional_flags.entrySet()) {
            String argname = pair.getKey();
            ArgType at = pair.getValue();
            if (at == ArgType.KEYVALUE) {
                _kv_options.add(argname);
            } else {
                _k_options.add(argname);
            }
        }

        _custom_text = custom_text;
    }

    private int getIndentWidth(Collection<String> names, int padding) {
        int max = 0;
        for(String name : names) {
            if (name.length() > max) {
                max = name.length();
            }
        }
        return max + padding;
    }

    private String makeNSpaces(int n) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) {
            s.append(" ");
        }
        return s.toString();
    }

    private String argWrap(String prefix, String postfix) {
        int max_width = 80;
        int prefix_width = prefix.length();
        int postfix_width = max_width - prefix_width;
        assert(postfix_width > 0);

        String[] words = postfix.split("\\s+");

        StringBuilder sb = new StringBuilder();

        // append prefix
        sb.append(prefix);

        // append words, one at a time
        int w = 0;
        int line_len = 0;
        while(w < words.length) {
            String word = words[w];

            // insert a newline and padding, if necessary
            if (line_len + word.length() + 1 > postfix_width) {
                line_len = 0;
                sb.append(String.format("%n%s", makeNSpaces(prefix_width)));
            }

            // we may need a preceding space
            if (line_len > 0) {
                word = " " + word;
            }

            // append a word
            sb.append(word);

            // adjust counts
            line_len += word.length();
            w++;
        }
        return sb.toString();
    }

    private String argFormatter(List<String> args, String prefix, String postfix) {
        return argFormatter(args, prefix, postfix, getIndentWidth(args, PADDING));
    }

    private String argFormatter(List<String> args, String prefix, String postfix, int width) {
        StringBuilder str = new StringBuilder();

        for (String arg: args) {
            String leftside = String.format("%s%s%s%s", prefix, arg, postfix, makeNSpaces(width - arg.length()));

            if (_arg_usage.containsKey(arg)) {
                str.append(String.format("%s%n", argWrap(leftside, _arg_usage.get(arg))));
            } else {
                str.append(String.format("%s%n", argWrap(leftside, "It does something!")));
            }
        }

        return str.toString();
    }

    public void usage(String... err_reasons) {
        for (String err_reason: err_reasons) {
            System.err.printf("ERROR: %s%n", err_reason);
        }
        if (err_reasons.length > 0 ) { System.err.println(); }

        StringBuilder usage = new StringBuilder();

        // top banner
        usage.append(String.format("USAGE: %s ", _program_name));
        if (_k_options.size() > 0 || _kv_options.size() > 0) {
            usage.append("[OPTIONS...] ");
        }
        for (String marg : _m_args) {
            usage.append(String.format("<%s> ", marg));
        }
        usage.append(String.format("%n%n"));

        // mandatory argument descriptions
        if (_m_args.size() > 0) {
            usage.append(String.format("Mandatory arguments:%n"));
            usage.append(argFormatter(_m_args, "<", ">"));
            usage.append(String.format("%n"));
        }

        // optional argument descriptions
        if (_k_options.size() > 0 || _kv_options.size() > 0) {
            // key-value arguments
            List<String> kvopts_sorted = new ArrayList<String>();
            kvopts_sorted.addAll(_kv_options);    // add all key-val opts
            kvopts_sorted.removeAll(_h_options);  // remove all hidden opts
            Collections.sort(kvopts_sorted);

            // single-key arguments
            List<String> kopts_sorted = new ArrayList<String>();
            kopts_sorted.addAll(_k_options);     // add all single-key opts
            kopts_sorted.removeAll(_h_options);
            Collections.sort(kopts_sorted);

            // find widest argument
            int kvwidth = getIndentWidth(kvopts_sorted, PADDING);
            int kwidth = getIndentWidth(kopts_sorted, PADDING);
            int width = kvwidth > kwidth ? kvwidth : kwidth;

            usage.append(String.format("Optional arguments:%n"));
            usage.append(argFormatter(kvopts_sorted, "--", "=<arg>", width));
            usage.append(argFormatter(kopts_sorted, "--", "      ", width));
            usage.append(String.format("%n"));
        }

        // custom text
        usage.append(_custom_text);

        System.err.println(usage.toString());
        System.exit(-1);
    }

    public HashMap<String,String> processArgs(String[] argarray) {
        List<String> errors = new ArrayList<String>();

        String args = Join(argarray, " ");

        Scanner sc = new Scanner(args);
        HashMap<String,String> argmap = new HashMap<String, String>();

        // get key-value pairs
        String s;
        while((s = sc.findInLine("(--[a-z_-]+\\s*=\\s*\\S+)|(--[a-z_-]+)")) != null) {
            String[] pair = s.split("=");
            String key = pair[0].substring(2);

            if (pair.length == 1) {
                // single-key args
                if (_k_options.contains(key)) {
                    if (!ArgMapAdd(argmap, key, "true")) {
                        errors.add(String.format("Duplicate option: \"%s\".", key));
                    }
                } else {
                    errors.add(String.format("Unrecognized option: %s", key));
                }
            } else {
                // key-value args
                String value = pair[1];

                if (_kv_options.contains(key)) {
                    if (!ArgMapAdd(argmap, key, value)) {
                        errors.add(String.format("Duplicate option: \"%s\".", key));
                    }
                } else {
                    errors.add(String.format("Unrecognized option: %s", key));
                }
            }
        }

        // get mandatory arguments
        for(String marg : _m_args) {
            String argtxt = sc.findInLine("\\S+");
            if (argtxt == null) {
                errors.add(String.format("Missing <%s>. Note that options must precede mandatory arguments.", marg));
            }
            argmap.put(marg, argtxt);
        }

        // check for anything remaining
        if (sc.hasNext()) { errors.add(String.format("Unrecognized arguments: %s", sc.nextLine())); }

        sc.close();

        // add option defaults
        for(Map.Entry<String,String> pair : _defaults.entrySet()) {
            DefArgMapAdd(argmap, pair.getKey(), pair.getValue());
        }

        // if the user wants to debug the arg parser, spit out the
        // HashMap and quit
        if (argmap.containsKey(DEBUG)) {
            System.err.println(argmap.toString());
            System.exit(0);
        }

        // check for errors
        if (errors.size() > 0) {
            usage(errors.toArray(new String[errors.size()]));
        }

        return argmap;
    }

    // Utility methods
    private static String Join(String[] strs, String delim) {
        return Arrays.toString(strs).replace(", ", delim).replaceAll("[\\[\\]]", "");
    }

    // Returns false if the HashMap already contains the key
    private Boolean ArgMapAdd(HashMap<String,String> argmap, String key, String value) {
        if (argmap.containsKey(key)) {
            return false;
        }
        argmap.put(key, value);
        return true;
    }

    private void DefArgMapAdd(HashMap<String,String> argmap, String key, String value) {
        if (!argmap.containsKey(key)) {
            argmap.put(key, value);
        }
    }

}
