CREATE TABLE favorites(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    listing_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id)
                      REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_listing FOREIGN KEY  (listing_id)
                      REFERENCES material_listings(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_listing UNIQUE (user_id, listing_id)
);

CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_listing_id ON favorites(listing_id);
CREATE INDEX idx_favorites_created_at ON favorites(created_at DESC);

COMMENT ON TABLE favorites IS 'User favorites/watchlist for material listings';
COMMENT ON COLUMN favorites.user_id IS 'User who favorited the listing';
COMMENT ON COLUMN favorites.listing_id IS 'Favorited material listing';
COMMENT ON CONSTRAINT uk_user_listing ON favorites IS 'Prevents duplicate favorites';
