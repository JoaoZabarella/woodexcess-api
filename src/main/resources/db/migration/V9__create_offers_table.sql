CREATE TYPE offer_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'COUNTER_OFFERED',
    'EXPIRED',
    'CANCELLED'
);

CREATE TABLE offers (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        listing_id UUID NOT NULL,
                        buyer_id UUID NOT NULL,
                        seller_id UUID NOT NULL,
                        offered_price DECIMAL(10, 2) NOT NULL,
                        quantity INTEGER NOT NULL DEFAULT 1,
                        message TEXT,
                        status offer_status NOT NULL DEFAULT 'PENDING',
                        expires_at TIMESTAMP NOT NULL,
                        parent_offer_id UUID,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_offers_listing FOREIGN KEY (listing_id) REFERENCES material_listings(id) ON DELETE CASCADE,
                        CONSTRAINT fk_offers_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_offers_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_offers_parent FOREIGN KEY (parent_offer_id) REFERENCES offers(id) ON DELETE SET NULL,
                        CONSTRAINT check_positive_price CHECK (offered_price > 0),
                        CONSTRAINT check_positive_quantity CHECK (quantity > 0),
                        CONSTRAINT check_expires_future CHECK (expires_at > created_at)
);

CREATE INDEX idx_offers_buyer_status ON offers(buyer_id, status);
CREATE INDEX idx_offers_seller_status ON offers(seller_id, status);
CREATE INDEX idx_offers_listing_status ON offers(listing_id, status);
CREATE INDEX idx_offers_expires ON offers(expires_at) WHERE status = 'PENDING';
CREATE INDEX idx_offers_created ON offers(created_at DESC);

CREATE TRIGGER update_offers_updated_at BEFORE UPDATE ON offers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
