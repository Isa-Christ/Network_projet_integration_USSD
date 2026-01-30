package com.network.projet.ussd.domain.enums;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = StateTypeDeserializer.class)
public enum StateType {
    MENU,     // Display menu with options
    INPUT,    // Collect user input
    DISPLAY,  // Just show info
    PROCESSING, // Perform some backend processing
    FINAL     // Terminal state
}