-- Create material_listings table
CREATE TABLE material_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    material_type VARCHAR(50) NOT NULL,
    price DECIMAL(10, 2) NOT NULL CHECK (price > 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    condition VARCHAR(20) NOT NULL,
    address_id UUID,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_listing_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_listing_address FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE SET NULL,
    CONSTRAINT chk_material_type CHECK (material_type IN ('WOOD', 'MDF', 'PLYWOOD', 'VENEER', 'PARTICLE_BOARD', 'OTHER')),
    CONSTRAINT chk_condition CHECK (condition IN ('NEW', 'USED', 'SCRAP')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'RESERVED', 'SOLD'))
);

-- Create indexes for performance
CREATE INDEX idx_listing_owner ON material_listings(owner_id);
CREATE INDEX idx_listing_status ON material_listings(status);
CREATE INDEX idx_listing_material_type ON material_listings(material_type);
CREATE INDEX idx_listing_city ON material_listings(city);
CREATE INDEX idx_listing_state ON material_listings(state);
CREATE INDEX idx_listing_price ON material_listings(price);
CREATE INDEX idx_listing_created_at ON material_listings(created_at DESC);

-- Composite index for common filter combinations
CREATE INDEX idx_listing_status_material_city ON material_listings(status, material_type, city);
CREATE INDEX idx_listing_status_price ON material_listings(status, price);

-- Comments for documentation
COMMENT ON TABLE material_listings IS 'Anúncios de sobras de materiais de madeira';
COMMENT ON COLUMN material_listings.owner_id IS 'Usuário que criou o anúncio';
COMMENT ON COLUMN material_listings.material_type IS 'Tipo de material (WOOD, MDF, PLYWOOD, etc)';
COMMENT ON COLUMN material_listings.condition IS 'Condição do material (NEW, USED, SCRAP)';
COMMENT ON COLUMN material_listings.status IS 'Status do anúncio (ACTIVE, INACTIVE, RESERVED, SOLD)';
COMMENT ON COLUMN material_listings.city IS 'Cidade (denormalizado para filtros rápidos)';
COMMENT ON COLUMN material_listings.state IS 'Estado (denormalizado para filtros rápidos)';
