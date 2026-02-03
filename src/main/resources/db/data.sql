-- =========================
-- 1. Services disponibles
-- =========================
CREATE TABLE IF NOT EXISTS ussd_service (
    id SERIAL PRIMARY KEY,
    code VARCHAR(10) UNIQUE,          -- "*500#"
    name VARCHAR(100),                -- "PicknDrop"
    automaton_config JSONB NOT NULL,  -- Définition complète
    api_config JSONB,                 -- Config API
    is_active BOOLEAN DEFAULT true
);

-- =========================
-- 2. Sessions USSD
-- =========================
CREATE TABLE IF NOT EXISTS ussd_sessions (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(15) NOT NULL,
    service_id INTEGER REFERENCES ussd_service(id),
    current_state VARCHAR(50),
    session_data JSONB,               -- Variables collectées
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ussd_sessions_phone
    ON ussd_sessions(phone_number);

CREATE INDEX IF NOT EXISTS idx_ussd_sessions_expires
    ON ussd_sessions(expires_at);

-- =========================
-- 3. Utilisateurs
-- =========================
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    phone_number VARCHAR(15) UNIQUE,
    pin_hash VARCHAR(255),
    name VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

-- =========================
-- 4. Stockage générique
-- =========================
CREATE TABLE IF NOT EXISTS generic_storage (
    id SERIAL PRIMARY KEY,
    phone_number VARCHAR(15) NOT NULL,
    service_code VARCHAR(10) NOT NULL,
    storage_key VARCHAR(100) NOT NULL,
    storage_value JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (phone_number, service_code, storage_key)
);

CREATE INDEX IF NOT EXISTS idx_generic_storage_lookup
    ON generic_storage(phone_number, service_code);
