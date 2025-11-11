CREATE TABLE users(
                      id UUID PRIMARY KEY,
                      name VARCHAR(50) NOT NULL,
                      email VARCHAR(100) NOT NULL UNIQUE,
                      password_hash VARCHAR(255) NOT NULL,
                      role VARCHAR(20) NOT NULL DEFAULT 'USER',
                      phone VARCHAR(20),
                      active BOOLEAN NOT NULL DEFAULT TRUE,
                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
