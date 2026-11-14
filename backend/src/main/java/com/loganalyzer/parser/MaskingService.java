package com.loganalyzer.parser;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MaskingService {

    // Patterns for sensitive data masking
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(password|passwd|pwd|secret|token|apikey|api_key|auth_token|authorization)\\s*[:=]\\s*['\"]?([^'\"\\s,;]+)['\"]?");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)Bearer\\s+([A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*)");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b([a-zA-Z0-9_.+-]+)@([a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+)\\b");
    private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[a-zA-Z0-9-_=]+\\.eyJ[a-zA-Z0-9-_=]+\\.[a-zA-Z0-9-_.+/=]+");

    public String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Mask JWT tokens
        String result = JWT_PATTERN.matcher(input).replaceAll("[MASKED_JWT_TOKEN]");

        // Mask Bearer tokens
        result = BEARER_PATTERN.matcher(result).replaceAll("Bearer [MASKED_AUTH_TOKEN]");

        // Mask password fields
        Matcher pwdMatcher = PASSWORD_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (pwdMatcher.find()) {
            String key = pwdMatcher.group(1);
            pwdMatcher.appendReplacement(sb, key + "=***MASKED***");
        }
        pwdMatcher.appendTail(sb);
        result = sb.toString();

        // Mask credit cards
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("[MASKED_CC_NUMBER]");

        // Mask emails partially: e.g. j***n@example.com
        Matcher emailMatcher = EMAIL_PATTERN.matcher(result);
        StringBuffer emailSb = new StringBuffer();
        while (emailMatcher.find()) {
            String name = emailMatcher.group(1);
            String domain = emailMatcher.group(2);
            String maskedName = name.length() > 2 ? name.charAt(0) + "***" + name.charAt(name.length() - 1) : "***";
            emailMatcher.appendReplacement(emailSb, maskedName + "@" + domain);
        }
        emailMatcher.appendTail(emailSb);
        result = emailSb.toString();

        return result;
    }
}
