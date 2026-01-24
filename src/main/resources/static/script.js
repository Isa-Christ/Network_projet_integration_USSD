/** CONSTANTES & CONFIG */
const CONFIG = { 
  API_URL: 'http://localhost:8080/api/ussd', 
  TIMEOUT: 8000,
  PHONE_NUMBER: '237690123456' // Numéro par défaut (tu peux le rendre configurable plus tard)
};
const ALLOWED_KEYS = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '#'];

/** ÉTAT GLOBALE */
let state = { 
  currentDisplay: '', 
  sessionId: null, 
  isWaiting: false, 
  isUssdActive: false,
  ussdCode: '' // Pour stocker le code initial
};

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

// --- GESTION CLAVIER PHYSIQUE ---
document.addEventListener('keydown', (e) => {
  if (state.isUssdActive && document.activeElement === elements.input) {
    if (e.key === 'Enter') handleResponse();
    return;
  }

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
  state.ussdCode = code;
  state.isUssdActive = true;
  showOverlay(true);
  await sendToBackend('', true);
}

async function handleResponse() {
  const val = elements.input.value;
  elements.input.value = '';
  await sendToBackend(val);
}

async function sendToBackend(userInput, isInitial = false) {
  if (state.isWaiting) return;

  try {
    toggleLoading(true);

    const payload = {
      sessionId: state.sessionId,
      phoneNumber: CONFIG.PHONE_NUMBER,
      ussdCode: state.ussdCode,
      text: userInput
    };

    const response = await fetch(CONFIG.API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    
    updateUssdUI({
      message: data.message,
      should_close: !data.continueSession
    });

  } catch (error) {
    console.error('USSD Error:', error);
    updateUssdUI({ 
      message: "Erreur de connexion.\nRéessayez plus tard.", 
      should_close: true 
    });
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
    setTimeout(() => elements.input.focus(), 300);
  } else {
    setTimeout(closeUssd, 3500);
  }
}

function showOverlay(show) {
  if (show) {
    elements.overlay.style.display = 'flex';
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
  state.ussdCode = '';
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