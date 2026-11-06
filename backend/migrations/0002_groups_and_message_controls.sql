PRAGMA foreign_keys = ON;
ALTER TABLE conversations ADD COLUMN kind TEXT NOT NULL DEFAULT 'direct';
ALTER TABLE conversations ADD COLUMN title TEXT;
ALTER TABLE conversations ADD COLUMN created_by TEXT;
ALTER TABLE messages ADD COLUMN edited_at INTEGER;
ALTER TABLE messages ADD COLUMN deleted_at INTEGER;
CREATE INDEX IF NOT EXISTS idx_conversations_kind ON conversations(kind);
CREATE TABLE IF NOT EXISTS message_reactions(
  message_id TEXT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  emoji TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  PRIMARY KEY(message_id,user_id,emoji)
);
CREATE INDEX IF NOT EXISTS idx_reactions_message ON message_reactions(message_id);
