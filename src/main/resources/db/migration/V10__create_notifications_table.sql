CREATE TYPE notification_type AS ENUM (
    'NEW_OFFER',
    'OFFER_ACCEPTED',
    'OFFER_REJECTED',
    'COUNTER_OFFER',
    'OFFER_EXPIRED',
    'OFFER_CANCELLED'
);

CREATE TABLE notifications (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL,
                               type notification_type NOT NULL,
                               title VARCHAR(255) NOT NULL,
                               message TEXT NOT NULL,
                               link_url VARCHAR(500),
                               metadata JSONB,
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               read_at TIMESTAMP,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(user_id) WHERE is_read = FALSE;
