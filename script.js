/** CONSTANTES & CONFIG */
const CONFIG = { API_URL: 'https://api-mock.com/ussd', TIMEOUT: 8000 };
const ALLOWED_KEYS = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '#'];

/** ÉTAT GLOBALE */
let state = { currentDisplay: '', sessionId: null, isWaiting: false, isUssdActive: false };

/** ÉLÉMENTS DOM */
const elements = {
  display: document.getElementById('display-container'),
  overlay: document.getElementById('ussd-overlay'),
  dialog: document.getElementById('ussd-dialog'),
  content: document.getElementById('ussd-content'),
  input: document.getElementById('ussd-input'),
  sendBtn: document.getElementById('send-btn'),
  keypad: document.getElementById('keypad')
};

// --- GESTION CLAVIER PHYSIQUE (NOUVEAU) ---
document.addEventListener('keydown', (e) => {
  // Si la dialog USSD est ouverte, on laisse l'input gérer le clavier
  if (state.isUssdActive && document.activeElement === elements.input) {
    if (e.key === 'Enter') handleResponse();
    return;
  }

  // Gestion du composeur principal
  if (ALLOWED_KEYS.includes(e.key)) {
    press(e.key);
    simulateKeyPressHighlight(e.key);
  } else if (e.key === 'Backspace') {
    handleBackspace();
  } else if (e.key === 'Enter') {
    startUssd();
  }
});

function simulateKeyPressHighlight(keyChar) {
  const keyEl = elements.keypad.querySelector(`[data-key="${keyChar}"]`);
  if (keyEl) {
    keyEl.classList.add('active-press');
    setTimeout(() => keyEl.classList.remove('active-press'), 150);
  }
}

// --- LOGIQUE UI COMPOSEUR ---
function press(key) {
  if (state.currentDisplay.length < 20) {
    state.currentDisplay += key;
    updateDisplay();
  }
}

function handleBackspace() {
  state.currentDisplay = state.currentDisplay.slice(0, -1);
  updateDisplay();
}

function updateDisplay() {
  elements.display.innerText = state.currentDisplay;
}

// --- CŒUR USSD ---
async function startUssd() {
  if (!state.currentDisplay.includes('*') || !state.currentDisplay.endsWith('#')) return;

  const code = state.currentDisplay;
  state.currentDisplay = '';
  updateDisplay();

  state.sessionId = 'sess_' + Date.now();
  state.isUssdActive = true;
  showOverlay(true);
  await sendToBackend(code, true);
}

async function handleResponse() {
  const val = elements.input.value;
  // On autorise l'envoi vide si le backend le gère, sinon ajouter: if (!val) return;
  elements.input.value = '';
  await sendToBackend(val);
}

async function sendToBackend(userInput, isInitial = false) {
  if (state.isWaiting) return;

  try {
    toggleLoading(true);

    // --> REMPLACER PAR VOTRE VRAI FETCH ICI <--
    // const response = await fetch(...)

    // Simulation pour le test (à retirer quand le backend est prêt)
    await new Promise(r => setTimeout(r, 800)); // Latence réseau fictive
    mockBackendResponse(userInput, isInitial);

  } catch (error) {
    updateUssdUI({ message: "Erreur de connexion.\nRéessayez plus tard.", should_close: true });
  } finally {
    toggleLoading(false);
  }
}

// --- GESTION UI USSD ---
function updateUssdUI(data) {
  elements.content.innerText = data.message;

  const isInteractive = !data.should_close;
  elements.input.style.display = isInteractive ? 'block' : 'none';
  elements.sendBtn.style.display = isInteractive ? 'block' : 'none';

  if (isInteractive) {
    // Petit délai pour que le focus fonctionne après l'animation
    setTimeout(() => elements.input.focus(), 300);
  } else {
    setTimeout(closeUssd, 3500); // Fermeture auto après message final
  }
}

function showOverlay(show) {
  if (show) {
    elements.overlay.style.display = 'flex';
    // Force repaint pour l'animation CSS
    elements.dialog.getBoundingClientRect();
    elements.dialog.classList.add('show');
  } else {
    elements.dialog.classList.remove('show');
    setTimeout(() => {
      elements.overlay.style.display = 'none';
      elements.content.innerText = "Chargement...";
      elements.input.value = '';
    }, 250);
  }
}

function closeUssd() {
  showOverlay(false);
  state.sessionId = null;
  state.isUssdActive = false;
}

function toggleLoading(isLoading) {
  state.isWaiting = isLoading;
  elements.content.style.opacity = isLoading ? '0.6' : '1';
  if (isLoading) {
    elements.sendBtn.disabled = true;
  } else {
    elements.sendBtn.disabled = false;
    elements.content.style.opacity = '1';
  }
}

/** MOCK BACKEND (POUR TEST LOCAL) */
function mockBackendResponse(input, isInitial) {
  let response = { message: "Action non reconnue.", should_close: true };

  if (isInitial && input === '*126#') {
    response = { message: "Menu Orange:\n1. Solde & Conso\n2. Offres mobiles\n3. Transfert d'argent\n0. Quitter", should_close: false };
  } else if (input === '1') {
    response = { message: "Votre solde principal est de 1250 FCFA, valable jusqu'au 30/01.\n\nTapez 1 pour détails\nTapez 0 pour retour", should_close: false };
  } else if (input === '2') {
    response = { message: "Offres:\n1) Jour (500F)\n2) Semaine (1500F)\n3) Mois\n0) Retour", should_close: false };
  } else if (input === '0') {
    mockBackendResponse('*126#', true); return;
  } else if (input !== '*126#' && !isInitial) {
    response = { message: "Merci d'avoir utilisé ce service. Au revoir.", should_close: true };
  }

  updateUssdUI(response);
}