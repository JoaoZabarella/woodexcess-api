CREATE TABLE addresses (
                           id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                           user_id UUID NOT NULL,
                           street VARCHAR(255) NOT NULL,
                           number VARCHAR(20) NOT NULL,
                           complement VARCHAR(255),
                           district VARCHAR(100) NOT NULL,
                           city VARCHAR(100) NOT NULL,
                           state VARCHAR(2) NOT NULL,
                           zip_code VARCHAR(10) NOT NULL,
                           country VARCHAR(50) NOT NULL DEFAULT 'Brasil',
                           active BOOLEAN NOT NULL DEFAULT true,
                           is_primary BOOLEAN NOT NULL DEFAULT false,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);
CREATE INDEX idx_addresses_zip_code ON addresses(zip_code);
CREATE INDEX idx_addresses_active ON addresses(active);
CREATE INDEX idx_addresses_is_primary ON addresses(is_primary);

COMMENT ON TABLE addresses IS 'Endereços dos usuários';
COMMENT ON COLUMN addresses.is_primary IS 'Endereço principal do usuário';
COMMENT ON COLUMN addresses.active IS 'Soft delete';
