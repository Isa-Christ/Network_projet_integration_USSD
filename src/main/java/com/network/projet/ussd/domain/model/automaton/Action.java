package com.network.projet.ussd.domain.model.automaton;

import com.network.projet.ussd.domain.enums.ActionType;
import com.network.projet.ussd.domain.enums.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Action {
    private ActionType type;
    private HttpMethod method;
    private String endpoint;
    private Map<String, String> headers;
    private Map<String, Object> body;

    /**
     * Mapping pour la requête (utilisé dans ApiInvoker ligne 214-215)
     * Permet de mapper les variables du contexte vers les paramètres de la requête
     */
    private Map<String, String> requestMapping;

    private ActionResult onSuccess;
    private ActionResult onError;
    private String storageKey; // Clé de stockage
    private String storeAs; // Variable où stocker le résultat
    private String operation; // "SAVE", "APPEND", "DELETE"
    private Object value; // Valeur à stocker (peut contenir des templates)
}