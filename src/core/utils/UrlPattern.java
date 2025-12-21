package core.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

public class UrlPattern {
    private String originalPattern;
    private Pattern regex;
    private List<String> paramNames;

    public UrlPattern(String pattern) {
        this.originalPattern = pattern;
        this.paramNames = new java.util.ArrayList<>();
        this.regex = compilePattern(pattern);
    }

    private Pattern compilePattern(String pattern) {
        StringBuilder regexBuilder = new StringBuilder("^");
        int lastIndex = 0;
        
        Pattern paramPattern = Pattern.compile("\\{([^/]+)\\}");
        Matcher matcher = paramPattern.matcher(pattern);
        
        while (matcher.find()) {
            regexBuilder.append(Pattern.quote(pattern.substring(lastIndex, matcher.start())));
            regexBuilder.append("([^/]+)");
            paramNames.add(matcher.group(1));
            lastIndex = matcher.end();
        }
        
        regexBuilder.append(Pattern.quote(pattern.substring(lastIndex)));
        regexBuilder.append("$");
        
        return Pattern.compile(regexBuilder.toString());
    }

    public boolean matches(String url) {
        return regex.matcher(url).matches();
    }

    public Map<String, String> extractParams(String url) {
        Map<String, String> params = new HashMap<>();
        Matcher matcher = regex.matcher(url);
        
        if (matcher.matches()) {
            for (int i = 0; i < paramNames.size(); i++) {
                params.put(paramNames.get(i), matcher.group(i + 1));
            }
        }
        
        return params;
    }

    public String getOriginalPattern() {
        return originalPattern;
    }
}