PRAGMA foreign_keys = ON;
CREATE TABLE users(id TEXT PRIMARY KEY, username TEXT NOT NULL UNIQUE COLLATE NOCASE, display_name TEXT NOT NULL, password_hash TEXT NOT NULL, created_at INTEGER NOT NULL);
CREATE TABLE conversations(id TEXT PRIMARY KEY, created_at INTEGER NOT NULL);
CREATE TABLE conversation_members(conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, PRIMARY KEY(conversation_id,user_id));
CREATE TABLE messages(id TEXT PRIMARY KEY, conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE, sender_id TEXT NOT NULL REFERENCES users(id), body TEXT NOT NULL, created_at INTEGER NOT NULL);
CREATE INDEX idx_messages_conversation_time ON messages(conversation_id,created_at);
CREATE INDEX idx_members_user ON conversation_members(user_id);
