package com.network.projet.ussd.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ConditionalEvaluator - Evaluates conditional expressions in transitions
 * Supports: {{var != null}}, {{var == null}}, {{var == 'value'}}, etc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionalEvaluator {

    private static final Pattern CONDITION_PATTERN = Pattern.compile("\\{\\{\\s*(.+?)\\s*}}");
    
    /**
     * Evaluate a condition string against session data
     * 
     * @param condition Condition to evaluate (e.g., "{{authToken != null}}")
     * @param sessionData Current session data
     * @return true if condition is met, false otherwise
     */
    public boolean evaluate(String condition, Map<String, Object> sessionData) {
        if (condition == null || condition.isEmpty()) {
            return true; // No condition = always true
        }

        // Extract expression from {{...}}
        Matcher matcher = CONDITION_PATTERN.matcher(condition);
        if (!matcher.find()) {
            // Not a conditional expression, treat as literal match
            return true;
        }

        String expression = matcher.group(1).trim();
        log.debug("Evaluating condition: {}", expression);

        try {
            return evaluateExpression(expression, sessionData);
        } catch (Exception e) {
            log.error("Failed to evaluate condition: {}", expression, e);
            return false;
        }
    }

    private boolean evaluateExpression(String expression, Map<String, Object> sessionData) {
        // Handle != null
        if (expression.contains("!=")) {
            String[] parts = expression.split("!=");
            String varName = parts[0].trim();
            String expectedValue = parts[1].trim();
            
            Object actualValue = sessionData.get(varName);
            
            if ("null".equals(expectedValue)) {
                return actualValue != null;
            }
            
            return !valueEquals(actualValue, parseValue(expectedValue));
        }

        // Handle == null or == 'value'
        if (expression.contains("==")) {
            String[] parts = expression.split("==");
            String varName = parts[0].trim();
            String expectedValue = parts[1].trim();
            
            Object actualValue = sessionData.get(varName);
            
            if ("null".equals(expectedValue)) {
                return actualValue == null;
            }
            
            return valueEquals(actualValue, parseValue(expectedValue));
        }

        // Handle simple existence check
        return sessionData.containsKey(expression) && sessionData.get(expression) != null;
    }

    private Object parseValue(String value) {
        value = value.trim();
        
        // Remove quotes
        if ((value.startsWith("'") && value.endsWith("'")) || 
            (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }
        
        // Parse numbers
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Not a number, return as string
        }
        
        // Parse booleans
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        
        return value;
    }

    private boolean valueEquals(Object actual, Object expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        
        // Convert to strings for comparison
        return actual.toString().equals(expected.toString());
    }
}