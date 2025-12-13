ALTER TABLE addresses
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;

COMMENT ON COLUMN addresses.is_active IS 'Indicates if the address is active';

CREATE INDEX idx_addresses_is_active ON addresses(is_active);

UPDATE addresses SET is_active = true WHERE is_active IS NULL;
