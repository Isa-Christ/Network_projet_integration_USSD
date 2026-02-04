-- =========================
-- 1. Services disponibles
-- =========================
CREATE TABLE IF NOT EXISTS ussd_service (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    short_code VARCHAR(50),
    json_config TEXT,
    api_base_url VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ussd_service_code ON ussd_service (code);
-- =========================
-- 2. Sessions USSD
-- =========================
CREATE TABLE IF NOT EXISTS ussd_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL,
    service_code VARCHAR(50),
    current_state_id VARCHAR(50) NOT NULL,
    session_data TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ussd_sessions_phone ON ussd_sessions(phone_number);
CREATE INDEX IF NOT EXISTS idx_ussd_sessions_expires ON ussd_sessions(expires_at);
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
-- ============================
-- 4. Configurations générées
-- ============================
CREATE TABLE IF NOT EXISTS generated_configs (
    config_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type VARCHAR(50) NOT NULL,
    source_url VARCHAR(500),
    api_structure TEXT,
    selected_proposal_index INTEGER,
    generated_config TEXT NOT NULL,
    validation_report TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE generated_configs
ADD CONSTRAINT chk_source_type CHECK (
        source_type IN ('SWAGGER_URL', 'SWAGGER_FILE', 'POSTMAN')
    );
ALTER TABLE generated_configs
ADD CONSTRAINT chk_status CHECK (
        status IN ('ANALYZING', 'GENERATING', 'COMPLETED', 'FAILED')
    );
CREATE INDEX IF NOT EXISTS idx_generated_configs_status ON generated_configs (status);
CREATE INDEX IF NOT EXISTS idx_generated_configs_created ON generated_configs (created_at DESC);
-- ============================
-- 5. Historique des générations
-- ============================
CREATE TABLE IF NOT EXISTS generation_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_id UUID,
    admin_user VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    processing_time_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gen_history_config FOREIGN KEY (config_id) REFERENCES generated_configs (config_id) ON DELETE CASCADE
);
ALTER TABLE generation_history
ADD CONSTRAINT chk_action CHECK (
        action IN (
            'ANALYZE',
            'GENERATE_PROPOSALS',
            'GENERATE_CONFIG',
            'VALIDATE'
        )
    );
CREATE INDEX IF NOT EXISTS idx_gen_history_config ON generation_history (config_id);
CREATE INDEX IF NOT EXISTS idx_gen_history_created ON generation_history (created_at DESC);
-- =========================
-- 6. Stockage générique
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
CREATE INDEX IF NOT EXISTS idx_generic_storage_lookup ON generic_storage(phone_number, service_code);