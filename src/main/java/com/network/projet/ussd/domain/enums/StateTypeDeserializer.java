package com.network.projet.ussd.domain.enums;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Custom deserializer for StateType enum that handles invalid values with fallback.
 * Maps unknown state types to sensible defaults based on naming patterns.
 * 
 * @author Network Project Team
 * @since 2025-01-28
 */
@Slf4j
public class StateTypeDeserializer extends JsonDeserializer<StateType> {
    
    @Override
    public StateType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        
        if (value == null) {
            return StateType.DISPLAY;
        }
        
        String normalized = value.toUpperCase().trim();
        
        // Try exact match first
        try {
            return StateType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Fallback logic for common LLM mistakes
            log.warn("Invalid StateType value '{}', attempting to map to valid type", value);
            
            if (normalized.contains("MENU") || normalized.contains("CHOICE") || normalized.contains("OPTION")) {
                return StateType.MENU;
            } else if (normalized.contains("INPUT") || normalized.contains("ENTRY") || normalized.contains("FORM")) {
                return StateType.INPUT;
            } else if (normalized.contains("PROCESS") || normalized.contains("ACTION") || normalized.contains("EXECUTE")) {
                return StateType.PROCESSING;
            } else if (normalized.contains("FINAL") || normalized.contains("END") || normalized.contains("EXIT")) {
                return StateType.FINAL;
            } else if (normalized.contains("DISPLAY") || normalized.contains("SHOW") || normalized.contains("VIEW")) {
                return StateType.DISPLAY;
            }
            
            // Default fallback
            log.warn("Could not map '{}' to any StateType, using DISPLAY as default", value);
            return StateType.DISPLAY;
        }
    }
}
