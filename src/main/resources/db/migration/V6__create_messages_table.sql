CREATE TABLE messages (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          sender_id UUID NOT NULL,
                          recipient_id UUID NOT NULL,
                          listing_id UUID NOT NULL,
                          content TEXT NOT NULL,
                          is_read BOOLEAN DEFAULT FALSE,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id)
                              REFERENCES users(id) ON DELETE CASCADE,
                          CONSTRAINT fk_messages_recipient FOREIGN KEY (recipient_id)
                              REFERENCES users(id) ON DELETE CASCADE,
                          CONSTRAINT fk_messages_listing FOREIGN KEY (listing_id)
                              REFERENCES material_listings(id) ON DELETE CASCADE,
                          CONSTRAINT chk_sender_not_recipient CHECK (sender_id != recipient_id)
);

CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_recipient ON messages(recipient_id);
CREATE INDEX idx_messages_listing ON messages(listing_id);
CREATE INDEX idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX idx_messages_is_read ON messages (is_read) WHERE is_read = FALSE;

CREATE INDEX idx_messages_conversation ON messages(sender_id, recipient_id, listing_id, created_at);

CREATE INDEX idx_messages_unread ON messages(recipient_id, is_read) WHERE is_read = FALSE;

CREATE OR REPLACE FUNCTION update_messages_updated_at()
       RETURNS TRIGGER AS $$
       BEGIN
            NEW.updated_at = CURRENT_TIMESTAMP;
            RETURN NEW;
END;

$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_messages_updated_at
    BEFORE UPDATE ON messages
    FOR EACH ROW
    EXECUTE FUNCTION update_messages_updated_at();

COMMENT ON TABLE messages IS 'Stores chat messages between users about material listings';
COMMENT ON COLUMN messages.sender_id IS 'User who sent the message';
COMMENT ON COLUMN messages.recipient_id IS 'User who receives the message';
COMMENT ON COLUMN messages.listing_id IS 'Material listing the conversation is about';
COMMENT ON COLUMN messages.content IS 'Message text content (max 5000 chars)';
COMMENT ON COLUMN messages.is_read IS 'Whether the recipient has read the message';