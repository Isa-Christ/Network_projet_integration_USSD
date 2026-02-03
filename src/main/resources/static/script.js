/** * CONFIGURATION (Ta configuration originale)
 */
const CONFIG = { 
    API_URL: 'http://localhost:8080/api/ussd', // J'ai remis ton port 8080
    TIMEOUT: 15000, // Un peu plus long pour être sûr
    PHONE_NUMBER: '237690123456' // Ton numéro de test
};

/** * ÉTAT GLOBAL 
 */
let state = { 
    sessionId: null, 
    isWaiting: false, 
    ussdCode: '',
    currentInput: '' // Stocke ce qu'on tape sur l'écran d'accueil
};

/** * ÉLÉMENTS DU DOM (Mappage vers le nouveau HTML de ton ami) 
 */
const UI = {
    // Écran principal
    phoneInput: document.getElementById('phone-input'),
    addContactLabel: document.getElementById('add-contact'),
    toast: document.getElementById('toast'),
    
    // Modale USSD
    modal: document.getElementById('ussd-modal'),
    loading: document.getElementById('ussd-loading'),
    result: document.getElementById('ussd-result'),
    message: document.getElementById('ussd-message'),
    
    // Inputs et Boutons USSD
    responseInput: document.getElementById('ussd-response-input'), // L'input dans la modale
    inputContainer: document.getElementById('input-container'),
    actionButtons: document.getElementById('ussd-buttons'),
    okButton: document.getElementById('ussd-ok-btn')
};

/**
 * INITIALISATION & ÉCOUTEURS
 */
document.addEventListener('DOMContentLoaded', () => {
    // Écoute du clavier physique (Ta fonctionnalité clé)
    document.body.addEventListener('keydown', handlePhysicalKeyboard);
    
    // Focus initial
    UI.responseInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') sendResponse();
    });
});

/**
 * GESTION DU CLAVIER PHYSIQUE
 */
function handlePhysicalKeyboard(e) {
    const key = e.key;

    // Si la modale est ouverte, on laisse l'input de la modale gérer
    if (UI.modal.classList.contains('active')) {
        if (key === 'Escape') closeUssd();
        return;
    }

    // Sinon, on gère le composeur (écran principal)
    if (/^[0-9*#]$/.test(key)) {
        typeNumber(key);
        highlightKey(key); // Petit effet visuel bonus
    } else if (key === 'Backspace') {
        deleteChar();
    } else if (key === 'Enter') {
        initiateUSSD();
    }
}

// Petit effet visuel quand on tape au clavier physique (Bonus)
function highlightKey(char) {
    // Recherche le bouton qui contient ce chiffre (logique approximative pour l'effet)
    const buttons = document.querySelectorAll('.key-btn');
    buttons.forEach(btn => {
        if(btn.innerText.includes(char)) {
            btn.style.backgroundColor = 'rgba(0,0,0,0.1)';
            setTimeout(() => btn.style.backgroundColor = '', 150);
        }
    });
}

/**
 * FONCTIONS EXPOSÉES (Liées aux onclick="" du HTML de ton ami)
 */

// 1. Taper un numéro
window.typeNumber = function(char) {
    if (state.currentInput.length < 20) {
        state.currentInput += char;
        updateDialerUI();
    }
};

// 2. Effacer
window.deleteChar = function() {
    state.currentInput = state.currentInput.slice(0, -1);
    updateDialerUI();
};

// 3. Lancer l'USSD (Le bouton vert)
window.initiateUSSD = async function() {
    const code = state.currentInput;

    // Validation basique
    if (!code.startsWith('*') || !code.endsWith('#')) {
        showToast("Code USSD invalide (doit commencer par * et finir par #)");
        return;
    }

    // Démarrage session (Ta logique)
    state.ussdCode = code;
    state.sessionId = 'sess_' + Date.now(); // ID unique basé sur le temps
    
    openModal(true); // Ouvre en mode chargement
    
    // Premier appel (texte vide)
    await sendToBackend("");
};

// 4. Envoyer une réponse (Dans le menu)
window.sendResponse = async function() {
    const userInput = UI.responseInput.value;
    
    // Passage en mode chargement
    setLoadingState(true);
    UI.responseInput.value = ''; // Clean input
    
    await sendToBackend(userInput);
};

// 5. Fermer / Annuler
window.closeUssd = function() {
    UI.modal.classList.remove('active');
    
    // Reset complet de l'état USSD
    state.sessionId = null;
    state.isWaiting = false;
    
    // On garde le numéro composé sur l'écran principal (comme un vrai tel)
    UI.responseInput.value = '';
};

/**
 * COEUR DU SYSTÈME (Ta logique Backend API)
 */
async function sendToBackend(text) {
    if (state.isWaiting) return;
    state.isWaiting = true;

    const payload = {
        sessionId: state.sessionId,
        phoneNumber: CONFIG.PHONE_NUMBER,
        ussdCode: state.ussdCode,
        text: text
    };

    try {
        const response = await fetch(CONFIG.API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) throw new Error(`Erreur HTTP: ${response.status}`);

        const data = await response.json();
        
        // Affichage du résultat
        updateUssdDisplay(data);

    } catch (error) {
        console.error("Erreur API:", error);
        UI.message.innerText = "Problème de connexion.\nVeuillez réessayer.";
        // En cas d'erreur, on montre le bouton OK pour fermer
        UI.inputContainer.style.display = 'none';
        UI.actionButtons.style.display = 'none';
        UI.okButton.style.display = 'flex';
        setLoadingState(false);
    } finally {
        state.isWaiting = false;
    }
}

/**
 * GESTION DE L'INTERFACE (UI)
 */

// Met à jour l'écran principal (Dialer)
function updateDialerUI() {
    UI.phoneInput.value = state.currentInput;
    // Affiche/Cache "Ajouter au contact"
    if (state.currentInput.length > 0) {
        UI.addContactLabel.classList.add('visible');
    } else {
        UI.addContactLabel.classList.remove('visible');
    }
}

// Met à jour l'écran USSD (La modale grise)
function updateUssdDisplay(data) {
    setLoadingState(false);
    
    // 1. Mettre le texte (avec formatage des sauts de ligne)
    UI.message.innerText = data.message;

    // 2. Gérer la suite de la session
    // NOTE: Ton ancienne logique voulait que ce soit TOUJOURS interactif si possible.
    // Ici, j'adapte au design : 
    // Si continueSession = true -> Input + Bouton Envoyer
    // Si continueSession = false -> Bouton OK (Fermer)
    
    if (data.continueSession) {
        UI.inputContainer.style.display = 'block';
        UI.actionButtons.style.display = 'flex'; // Montre Envoyer/Annuler
        UI.okButton.style.display = 'none';      // Cache OK
        
        // Focus automatique sur l'input
        setTimeout(() => UI.responseInput.focus(), 100);
    } else {
        UI.inputContainer.style.display = 'none'; // Cache l'input
        UI.actionButtons.style.display = 'none';  // Cache Envoyer
        UI.okButton.style.display = 'flex';       // Montre OK
    }
}

// Bascule entre le Spinner et le Résultat
function setLoadingState(isLoading) {
    if (isLoading) {
        UI.loading.style.display = 'block';
        UI.result.style.display = 'none';
        UI.actionButtons.style.display = 'none';
        UI.okButton.style.display = 'none';
    } else {
        UI.loading.style.display = 'none';
        UI.result.style.display = 'block';
    }
}

// Ouvre la modale
function openModal(loading = false) {
    UI.modal.classList.add('active');
    setLoadingState(loading);
}

// Affiche un toast (notification en bas)
function showToast(msg) {
    UI.toast.innerText = msg;
    UI.toast.classList.add('show');
    setTimeout(() => UI.toast.classList.remove('show'), 3000);
}
