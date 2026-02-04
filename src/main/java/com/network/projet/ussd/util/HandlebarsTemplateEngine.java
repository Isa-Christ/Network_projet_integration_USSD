package com.network.projet.ussd.util;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * HandlebarsTemplateEngine - Template rendering with Handlebars
 * 
 * Supported syntax:
 * - Simple: {{variable}}
 * - Nested: {{recipient.name}}
 * - Loops: {{#each items}}{{name}}{{/each}}
 * - Conditionals: {{#if condition}}...{{/if}}
 * - Custom helpers: {{currency amount}}, {{boolean value}}
 * 
 * @author Network Projet Team
 * @since 2026-01-24
 * @version 2.0.0
 */
@Slf4j
@Component
public class HandlebarsTemplateEngine {
    
    private final Handlebars handlebars;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    private static final DecimalFormat CURRENCY_FORMAT;
    private static final DecimalFormat NUMBER_FORMAT;
    
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        
        CURRENCY_FORMAT = new DecimalFormat("#,##0.00", symbols);
        NUMBER_FORMAT = new DecimalFormat("#,##0.##", symbols);
    }
    
    public HandlebarsTemplateEngine() {
        this.handlebars = new Handlebars();
        registerCustomHelpers();
    }
    
    /**
     * Render template string with variables
     * 
     * @param templateString Template string with {{placeholders}}
     * @param variables Map of variable name to value
     * @return Rendered string
     */
    public String render(String templateString, Map<String, Object> variables) {
        if (templateString == null) {
            return "";
        }
        
        if (variables == null) {
            variables = new HashMap<>();
        }
        
        try {
            Template template = handlebars.compileInline(templateString);
            return template.apply(variables);
        } catch (IOException e) {
            log.error("Failed to render template: {}", templateString, e);
            return templateString; // Return original if rendering fails
        }
    }
    
    /**
     * Render entire map (for API body templates)
     * 
     * @param templateMap Template map
     * @param variables Variables map
     * @return Rendered map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> renderMap(
        Map<String, Object> templateMap, 
        Map<String, Object> variables
    ) {
        if (templateMap == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> rendered = new HashMap<>();
        
        templateMap.forEach((key, value) -> {
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
     * Register custom Handlebars helpers
     */
    private void registerCustomHelpers() {
        // Currency helper: {{currency amount}} → "5 000,00 FCFA"
        handlebars.registerHelper("currency", (Helper<Object>) (context, options) -> {
            if (context == null) {
                return "0,00 FCFA";
            }
            
            try {
                double amount = context instanceof Number 
                    ? ((Number) context).doubleValue()
                    : Double.parseDouble(context.toString());
                
                return CURRENCY_FORMAT.format(amount) + " FCFA";
            } catch (NumberFormatException e) {
                log.warn("Cannot format '{}' as currency", context);
                return context.toString() + " FCFA";
            }
        });
        
        // Boolean helper: {{boolean value}} → "Oui" or "Non"
        handlebars.registerHelper("boolean", (Helper<Object>) (context, options) -> {
            if (context == null) {
                return "Non";
            }
            
            if (context instanceof Boolean) {
                return (Boolean) context ? "Oui" : "Non";
            }
            
            String strValue = context.toString().toLowerCase().trim();
            return switch (strValue) {
                case "1", "true", "yes", "oui" -> "Oui";
                case "0", "false", "no", "non" -> "Non";
                default -> strValue;
            };
        });
        
        // Number helper: {{number value}} → "2,5"
        handlebars.registerHelper("number", (Helper<Object>) (context, options) -> {
            if (context == null) {
                return "0";
            }
            
            try {
                double number = context instanceof Number
                    ? ((Number) context).doubleValue()
                    : Double.parseDouble(context.toString());
                
                return NUMBER_FORMAT.format(number);
            } catch (Exception e) {
                log.warn("Cannot format '{}' as number", context);
                return context.toString();
            }
        });
        
        // Date helper: {{date value}} → "15/01/2026"
        handlebars.registerHelper("date", (Helper<Object>) (context, options) -> {
            if (context == null) {
                return "";
            }
            
            try {
                String dateStr = context.toString();
                
                if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                    LocalDate date = LocalDate.parse(dateStr.substring(0, 10));
                    return date.format(DATE_FORMATTER);
                }
                
                return dateStr;
            } catch (Exception e) {
                log.warn("Cannot parse date: {}", context);
                return context.toString();
            }
        });
        
        // Uppercase helper: {{uppercase text}} → "TEXT"
        handlebars.registerHelper("uppercase", (Helper<Object>) (context, options) -> {
            return context != null ? context.toString().toUpperCase() : "";
        });
        
        // Lowercase helper: {{lowercase text}} → "text"
        handlebars.registerHelper("lowercase", (Helper<Object>) (context, options) -> {
            return context != null ? context.toString().toLowerCase() : "";
        });
        
        // Capitalize helper: {{capitalize text}} → "Text"
        handlebars.registerHelper("capitalize", (Helper<Object>) (context, options) -> {
            if (context == null) {
                return "";
            }
            
            String str = context.toString();
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
        });
        
        // Default helper: {{default value "fallback"}}
        handlebars.registerHelper("default", (Helper<Object>) (context, options) -> {
            if (context != null && !context.toString().isEmpty()) {
                return context;
            }
            return options.param(0, "");
        });
        
        // Truncate helper: {{truncate text 50}}
        handlebars.registerHelper("truncate", (Helper<Object>) (context, options) -> {
            if (context == null) {
                return "";
            }
            
            String str = context.toString();
            int length = options.param(0, 50);
            
            if (str.length() <= length) {
                return str;
            }
            return str.substring(0, length - 3) + "...";
        });

        handlebars.registerHelper("add", (context, options) -> {
    return ((Number) context).intValue() + ((Number) options.param(0)).intValue();
});
    }
}