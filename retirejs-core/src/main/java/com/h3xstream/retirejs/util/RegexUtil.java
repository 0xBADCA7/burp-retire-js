package com.h3xstream.retirejs.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtil {

    private static Pattern PATTERN_REPLACE = Pattern.compile("^\\/(.*[^\\\\])\\/([^\\/]+)\\/$");

    /**
     *
     * @param pattern Pattern to find containing a single group to match. The group is mark in parentheses.
     * @param data The source of data to process (URI, filename, js content, ..)
     * @return Match of the first group extract
     */
    public static String simpleMatch(Pattern pattern, String data) {
        Matcher m = pattern.matcher(data);
        validateRegexResult(m);
        return m.find()? m.group(1) : null;
    }

    /**
     *
     * @param replacePattern The format expected is /(FIND_SOMETHING)/(REPLACE_BY_SOMETHING)/
     * @param data The source of data to process (URI, filename, js content, ..)
     * @return Match of the first group extract
     */
    public static String replaceMatch(String replacePattern, String data) {
        Matcher mRP = PATTERN_REPLACE.matcher(replacePattern);
        if(mRP.find() || mRP.groupCount() != 3) { //Extract the replace pattern /(FIND_SOMETHING)/(REPLACE_BY_SOMETHING)/
            String patternToFind = mRP.group(1);
            String replaceBy = mRP.group(2);

            Matcher m = Pattern.compile(patternToFind).matcher(data);
            validateRegexResult(m);
            if(m.find()) { //Do the replacement
                return m.group(1).replaceAll(patternToFind,replaceBy);
            }
            else {
                return null;
            }
        }
        else {
            throw new RuntimeException("Invalid replace pattern.");
        }
    }

    private static void validateRegexResult(Matcher m) {
        if(m.find() && m.groupCount() == 0) throw new IllegalArgumentException("The regex is expected to contain at least one group.");
        m.reset(); //Needed to use find() again
    }

    public static String replaceVersion(String regex) {
        return regex.replace("§§version§§","[0-9][0-9.a-z_\\\\\\\\-]+");
    }
}
