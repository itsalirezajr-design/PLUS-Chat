# PLUS Chat backend (Cloudflare Workers)
Implements username/password register and login, user lookup, 1:1 conversations, persistent text messages in D1, and Durable Object WebSocket room fan-out.

## Deploy
1. Create a D1 database named plus-chat-db in Cloudflare.
2. Put its ID in wrangler.toml.
3. From backend/: `npx wrangler d1 execute plus-chat-db --remote --file=migrations/0001_init.sql`
4. Set secret: `npx wrangler secret put JWT_SECRET` (use a long random value).
5. `npx wrangler deploy`
6. Configure the Android app API URL with the resulting Worker URL.

Passwords are PBKDF2-HMAC-SHA256 hashed with 210,000 iterations and per-account salts. Never store plaintext passwords or commit secrets.

This is an early 1:1 text-chat backend, not production-audited. Add rate limiting, abuse controls, session revocation/refresh, account recovery, monitoring and security review before public launch. No E2EE, media, push notifications, group chats, voice/video calls, or password recovery yet.
