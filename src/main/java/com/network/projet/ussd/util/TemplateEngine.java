package com.network.projet.ussd.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TemplateEngine - Advanced template rendering with filters
 * 
 * Supported syntax:
 * - Simple: {{variable}}
 * - With filter: {{amount | currency}}
 * - With default: {{email | default:"N/A"}}
 * - Nested objects: {{recipient.name}}
 * 
 * Available filters:
 * - currency: Format as FCFA (5000 → "5 000 FCFA")
 * - boolean: Convert 1/0 to Oui/Non
 * - default: Provide fallback value
 * - uppercase/lowercase: Text transformation
 * - number: Decimal formatting
 * - date: Date formatting (15/01/2026)
 * 
 * @author Magne Isabelle Christ
 * @since 2026-01-06
 * @version 1.0.0
 */
@Slf4j
@Component
public class TemplateEngine {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");
    private static final Pattern FILTER_PATTERN = 
        Pattern.compile("([^|]+)(?:\\s*\\|\\s*([^:]+)(?::(.+))?)?");
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    private static final DecimalFormat CURRENCY_FORMAT;
    private static final DecimalFormat NUMBER_FORMAT;
    
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        
        CURRENCY_FORMAT = new DecimalFormat("#,##0.00", symbols);
        NUMBER_FORMAT = new DecimalFormat("#,##0.##", symbols);
    }
    
    /**
     * Render template string with variables and filters
     * 
     * @param template Template string with {{placeholders}}
     * @param variables Map of variable name to value
     * @return Rendered string
     */
    public String render(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1).trim();
            String replacement = processPlaceholder(placeholder, variables);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Process single placeholder with optional filter
     * 
     * @param placeholder Placeholder string
     * @param variables Available variables
     * @return Processed value as string
     */
    private String processPlaceholder(String placeholder, Map<String, Object> variables) {
        Matcher filter_matcher = FILTER_PATTERN.matcher(placeholder);
        
        if (!filter_matcher.matches()) {
            return "";
        }
        
        String var_name = filter_matcher.group(1).trim();
        String filter_name = filter_matcher.group(2) != null 
            ? filter_matcher.group(2).trim() 
            : null;
        String filter_arg = filter_matcher.group(3) != null 
            ? filter_matcher.group(3).trim().replace("\"", "") 
            : null;
        
        Object value = getNestedValue(variables, var_name);
        
        if (filter_name != null) {
            value = applyFilter(value, filter_name, filter_arg);
        }
        
        return value != null ? value.toString() : "";
    }
    
    /**
     * Get nested value from map (e.g., "recipient.name")
     * 
     * @param variables Variables map
     * @param path Dot-separated path
     * @return Value at path or null
     */
    private Object getNestedValue(Map<String, Object> variables, String path) {
        String[] parts = path.split("\\.");
        Object current = variables;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Apply filter to value
     * 
     * @param value Value to filter
     * @param filter_name Filter name
     * @param filter_arg Optional filter argument
     * @return Filtered value
     */
    private Object applyFilter(Object value, String filter_name, String filter_arg) {
        return switch (filter_name.toLowerCase()) {
            case "currency" -> applyCurrencyFilter(value);
            case "boolean" -> applyBooleanFilter(value);
            case "default" -> value != null && !value.toString().isEmpty() 
                ? value 
                : filter_arg;
            case "uppercase" -> value != null ? value.toString().toUpperCase() : "";
            case "lowercase" -> value != null ? value.toString().toLowerCase() : "";
            case "number" -> applyNumberFilter(value, filter_arg);
            case "date" -> applyDateFilter(value);
            case "capitalize" -> applyCapitalizeFilter(value);
            case "truncate" -> applyTruncateFilter(value, filter_arg);
            default -> {
                log.warn("Unknown filter: {}", filter_name);
                yield value;
            }
        };
    }
    
    /**
     * Currency filter: 5000 → "5 000,00 FCFA"
     * 
     * @param value Numeric value
     * @return Formatted currency string
     */
    private String applyCurrencyFilter(Object value) {
        if (value == null) {
            return "0,00 FCFA";
        }
        
        try {
            double amount = value instanceof Number 
                ? ((Number) value).doubleValue()
                : Double.parseDouble(value.toString());
            
            return CURRENCY_FORMAT.format(amount) + " FCFA";
        } catch (NumberFormatException e) {
            log.warn("Cannot format '{}' as currency", value);
            return value.toString() + " FCFA";
        }
    }
    
    /**
     * Boolean filter: 1/true/"yes" → "Oui", 0/false/"no" → "Non"
     * 
     * @param value Boolean-like value
     * @return "Oui" or "Non"
     */
    private String applyBooleanFilter(Object value) {
        if (value == null) {
            return "Non";
        }
        
        if (value instanceof Boolean) {
            return (Boolean) value ? "Oui" : "Non";
        }
        
        String str_value = value.toString().toLowerCase().trim();
        return switch (str_value) {
            case "1", "true", "yes", "oui" -> "Oui";
            case "0", "false", "no", "non" -> "Non";
            default -> str_value;
        };
    }
    
    /**
     * Number filter: 2.5 → "2,5" or "2,50" (with precision)
     * 
     * @param value Numeric value
     * @param precision Number of decimal places
     * @return Formatted number string
     */
    private String applyNumberFilter(Object value, String precision) {
        if (value == null) {
            return "0";
        }
        
        try {
            double number = value instanceof Number
                ? ((Number) value).doubleValue()
                : Double.parseDouble(value.toString());
            
            if (precision != null) {
                int decimals = Integer.parseInt(precision);
                String zeros = "0".repeat(decimals);
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
                DecimalFormat custom_format = 
                    new DecimalFormat("#,##0." + zeros, symbols);
                return custom_format.format(number);
            }
            
            return NUMBER_FORMAT.format(number);
        } catch (Exception e) {
            log.warn("Cannot format '{}' as number", value);
            return value.toString();
        }
    }
    
    /**
     * Date filter: "2026-01-15" → "15/01/2026"
     * 
     * @param value Date value
     * @return Formatted date string
     */
    private String applyDateFilter(Object value) {
        if (value == null) {
            return "";
        }
        
        try {
            String date_str = value.toString();
            
            if (date_str.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                LocalDate date = LocalDate.parse(date_str.substring(0, 10));
                return date.format(DATE_FORMATTER);
            }
            
            return date_str;
        } catch (Exception e) {
            log.warn("Cannot parse date: {}", value);
            return value.toString();
        }
    }
    
    /**
     * Capitalize filter: "isa magne" → "Isa Magne"
     * 
     * @param value Text value
     * @return Capitalized text
     */
    private String applyCapitalizeFilter(Object value) {
        if (value == null) {
            return "";
        }
        
        String str = value.toString();
        String[] words = str.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * Truncate filter: "Long text..." → "Long te..." (max length)
     * 
     * @param value Text value
     * @param max_length Maximum length
     * @return Truncated text
     */
    private String applyTruncateFilter(Object value, String max_length) {
        if (value == null) {
            return "";
        }
        
        String str = value.toString();
        try {
            int length = max_length != null ? Integer.parseInt(max_length) : 50;
            if (str.length() <= length) {
                return str;
            }
            return str.substring(0, length - 3) + "...";
        } catch (NumberFormatException e) {
            return str;
        }
    }
    
    /**
     * Render entire map (for API body templates)
     * 
     * @param template Template map
     * @param variables Variables map
     * @return Rendered map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> renderMap(
        Map<String, Object> template, 
        Map<String, Object> variables
    ) {
        if (template == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> rendered = new HashMap<>();
        
        template.forEach((key, value) -> {
            if (value instanceof String) {
                rendered.put(key, render((String) value, variables));
            } else if (value instanceof Map) {
                rendered.put(key, renderMap((Map<String, Object>) value, variables));
            } else {
                rendered.put(key, value);
            }
        });
        
        return rendered;
    }
    
    /**
     * Validate template syntax (check for unclosed placeholders)
     * 
     * @param template Template string
     * @return true if valid
     */
    public boolean isValidTemplate(String template) {
        if (template == null) {
            return true;
        }
        
        long open_count = template.chars().filter(ch -> ch == '{').count();
        long close_count = template.chars().filter(ch -> ch == '}').count();
        
        return open_count == close_count && open_count % 2 == 0;
    }
    
    /**
     * Extract all variable names from template
     * 
     * @param template Template string
     * @return Set of variable names
     */
    public Set<String> extractVariableNames(String template) {
        Set<String> variables = new HashSet<>();
        
        if (template == null) {
            return variables;
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String placeholder = matcher.group(1).trim();
            Matcher filter_matcher = FILTER_PATTERN.matcher(placeholder);
            if (filter_matcher.matches()) {
                String var_name = filter_matcher.group(1).trim();
                variables.add(var_name);
            }
        }
        
        return variables;
    }
}