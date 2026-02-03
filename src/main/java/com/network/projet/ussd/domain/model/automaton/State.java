package com.network.projet.ussd.domain.model.automaton;

import com.network.projet.ussd.domain.enums.StateType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class State {
    private String id;
    private String name;
    private StateType type;
    private Boolean isInitial;
    private String message;
    private ValidationRule validation;
    private String storeAs;
    private Action action;
    private List<Transition> transitions;
    private List<Action> preActions;   // Actions avant l'état
    private List<Action> postActions;  // Actions après l'état

}