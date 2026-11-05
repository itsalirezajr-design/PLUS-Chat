# PLUS Chat

Android-first messenger prototype for the PLUS family.

## Current prototype
- Persian RTL login and registration screens.
- Username + password only; no phone, email, or Google sign-in.
- Username validation and minimum 8-character password validation.
- Dark navy / purple / cyan PLUS visual identity.
- Chat-list preview screen.

## Important status
This is a **UI prototype only**. The submit action currently opens the preview locally; it does not create a real account, authenticate against a server, or deliver messages. Do not use real passwords in this prototype.

## Planned backend
Cloudflare Workers API, Durable Objects for WebSocket connections, D1 for account/conversation/message metadata, and R2 for media. Production requires reviewed server-side password hashing (Argon2id or equivalent), rate limiting, secure sessions, recovery codes, and security tests. Never store plaintext passwords.
