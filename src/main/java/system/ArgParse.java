package system;

import java.util.*;

public class ArgParse {
    public static enum ArgType {
        KEYVALUE,   // options of the type --foo=bar
        KEY         // options of the type --foo
    }

    private static int PADDING = 2;
    private String _program_name;
    private List<String> _m_args;
    private HashSet<String> _kv_options;
    private HashSet<String> _k_options;
    private HashMap<String,String> _arg_usage;
    private HashMap<String,String> _defaults;

    public ArgParse(String program_name,
                    List<String> mandatory_args,
                    HashMap<String,ArgType> optional_flags,
                    HashMap<String,String> arg_usage,
                    HashMap<String,String> defaults) {
        _kv_options = new HashSet<String>();
        _k_options = new HashSet<String>();

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

    private String argFormatter(List<String> args, String prefix, String postfix) {
        StringBuilder str = new StringBuilder();

        int width = getIndentWidth(args, PADDING);
        for (String arg: args) {
            if (_arg_usage.containsKey(arg)) {
                str.append(String.format("%s%s%s%s%s%n", prefix, arg, postfix, makeNSpaces(width - arg.length()), _arg_usage.get(arg)));
            } else {
                str.append(String.format("%s%s%s%s%s%n", prefix, arg, postfix, makeNSpaces(width - arg.length()), "It does something!"));
            }
        }
        str.append(String.format("%n"));

        return str.toString();
    }

    public void Usage(String err_reason) {
        System.err.printf("ERROR: %s%n%n", err_reason);

        StringBuilder usage = new StringBuilder();

        // top banner
        usage.append(String.format("USAGE: %s %n%n", _program_name));
        if (_k_options.size() > 0 || _kv_options.size() > 0) {
            usage.append("[OPTIONS...]");
        }
        for (String marg : _m_args) {
            usage.append(String.format("<%s> ", marg));
        }
        usage.append(String.format("%n%n"));

        // mandatory argument descriptions
        if (_m_args.size() > 0) {
            usage.append("Mandatory arguments:%n");
            usage.append(argFormatter(_m_args, "<", ">"));
        }

        // optional argument descriptions
        if (_k_options.size() > 0 || _kv_options.size() > 0) {
            List<String> sorted_opts = new ArrayList<>();
            sorted_opts.addAll(_k_options);
            sorted_opts.addAll(_kv_options);
            Collections.sort(sorted_opts);
            usage.append(argFormatter(sorted_opts, "--", ""));
        }

        System.err.println(usage.toString());
        System.exit(-1);
    }

    public HashMap<String,String> processArgs(String[] argarray) {
        String args = Join(argarray, " ");
        if (argarray.length == 0) { Usage("Missing arguments."); }

        Scanner sc = new Scanner(args);
        HashMap<String,String> argmap = new HashMap<String, String>();

        // get key-value pairs
        String s;
        while((s = sc.findInLine("(--[a-z]+\\s*=\\s*\\S+)|(--[a-z]+)")) != null) {
            String[] pair = s.split("=");
            String key = pair[0].substring(2);

            if (pair.length == 1) {
                // single-key args
                if (_k_options.contains(key)) {
                    ArgMapAdd(argmap, key, "true");
                } else {
                    Usage("Unrecognized option.");
                }
            } else {
                // key-value args
                String value = pair[1];

                if (_kv_options.contains(key)) {
                    ArgMapAdd(argmap, key, value);
                } else {
                    Usage("Unrecognized option.");
                }
            }
        }

        // get mandatory arguments
        for(String marg : _m_args) {
            String argtxt = sc.findInLine("\\S+");
            if (argtxt == null) {
                Usage(String.format("Missing <%s>. Note that options must precede mandatory arguments.", marg));
            }
            argmap.put(marg, argtxt);
        }

        // check for anything remaining
        if (sc.hasNext()) { Usage(String.format("Unrecognized arguments: %s", sc.nextLine())); }

        sc.close();

        // add option defaults
        for(Map.Entry<String,String> pair : _defaults.entrySet()) {
            DefArgMapAdd(argmap, pair.getKey(), pair.getValue());
        }

        return argmap;
    }

    // Utility methods
    private static String Join(String[] strs, String delim) {
        return Arrays.toString(strs).replace(", ", delim).replaceAll("[\\[\\]]", "");
    }

    private void ArgMapAdd(HashMap<String,String> argmap, String key, String value) {
        if (argmap.containsKey(key)) {
            Usage(String.format("Duplicate option: \"%s\".", key));
        }
        argmap.put(key, value);
    }

    private void DefArgMapAdd(HashMap<String,String> argmap, String key, String value) {
        if (!argmap.containsKey(key)) {
            argmap.put(key, value);
        }
    }

}
