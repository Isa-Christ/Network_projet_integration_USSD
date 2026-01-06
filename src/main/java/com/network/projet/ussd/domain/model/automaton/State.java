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
    
    public boolean isFinal() {
        return type == StateType.FINAL;
    }

    public Boolean getIsInitial() {
        return isInitial;
    }

    public void setIsInitial(Boolean isInitial) {
        this.isInitial = isInitial;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ValidationRule getValidation() {
        return validation;
    }

    public void setValidation(ValidationRule validation) {
        this.validation = validation;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getStoreAs() {
        return storeAs;
    }

    public void setStoreAs(String storeAs) {
        this.storeAs = storeAs;
    }
}