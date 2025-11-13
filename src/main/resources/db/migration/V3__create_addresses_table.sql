CREATE TABLE addresses (
                           id UUID PRIMARY KEY,
                           user_id UUID NOT NULL,
                           street VARCHAR(255),
                           number VARCHAR(20),
                           complement VARCHAR(100),
                           district VARCHAR(100),
                           city VARCHAR(100),
                           state VARCHAR(50),
                           zip_code VARCHAR(20),
                           country VARCHAR(50),
                           active BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP,
                           CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);
CREATE INDEX idx_addresses_active ON addresses(active);