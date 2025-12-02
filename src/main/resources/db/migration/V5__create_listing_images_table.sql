CREATE TABLE listing_images
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id     UUID NOT NULL REFERENCES material_listings (id) ON DELETE CASCADE,
    image_url      VARCHAR(500) NOT NULL,
    thumbnail_url  VARCHAR(500) NOT NULL,
    storage_key    VARCHAR(255) NOT NULL UNIQUE,
    display_order  INTEGER NOT NULL,
    file_size      BIGINT NOT NULL,
    file_extension VARCHAR(10) NOT NULL,
    is_primary     BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_listing_images_listing FOREIGN KEY (listing_id) REFERENCES material_listings (id),
    CONSTRAINT chk_display_order CHECK (display_order BETWEEN 1 AND 5),
    CONSTRAINT chk_file_size CHECK (file_size > 0 AND file_size <= 5242880), --5MB
    CONSTRAINT chk_file_extension CHECK (file_extension IN ('jpg', 'jpeg', 'png'))
);

--Index para buscar imagens de um listing
CREATE INDEX idx_listing_images_listing_id ON listing_images(listing_id);

--Index para buscar imagem primária
CREATE INDEX idx_listing_images_primary ON listing_images(listing_id, is_primary) WHERE is_primary = true;

--Garantir apenas uma imagem primária por listing
CREATE UNIQUE INDEX idx_listing_images_unique_primary ON listing_images(listing_id) WHERE is_primary = true;

--Comentários
COMMENT ON TABLE listing_images IS 'Stores uploaded images for material listings';
COMMENT ON COLUMN listing_images.storage_key IS 'Unique key used in S3 (e.g., listings/abc123/img1.jpg)';
COMMENT ON COLUMN listing_images.display_order IS 'Order in which images are displayed (1 = first)';