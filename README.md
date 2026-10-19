# CyberLaw

A Retrieval-Augmented Generation (RAG) chatbot for **cybersecurity law and compliance** questions, in Azerbaijani and English.
It answers **only from the indexed documents**, cites the exact article (e.g. *Maddə 5*, *Article 32*), and lets users
check their own policies against a law.

| | |
|---|---|
| Backend | Spring Boot 4.1 · Spring AI 2.0 · Java 21 |
| AI | Google Gemini (chat + `gemini-embedding-001` multilingual embeddings) |
| Database | PostgreSQL 17 + PGVector (Docker) |
| Frontend | React 19 · Vite · Tailwind CSS 4 · Motion |
| Security | Spring Security + JWT (roles `USER` / `ADMIN`) |

## Features

- **RAG chat** with live streaming answers, clickable citation chips and a source drawer showing the original passage
- **Two-layer knowledge base**
  - *Official Library*: laws curated by the admin (upload, or drop files into `docs/`)
  - *My Documents*: private uploads per user (max 5, auto-deleted after 7 days)
- **Search scope switch**: Official / Mine / Both
- **Article-aware chunking**: laws are split by `Maddə N` / `Article N`, so citations point to real articles
- **Compliance checker**: the admin generates a requirements checklist from a law once; users compare their policy and get
  a score, plus *Met / Partial / Missing* with evidence quotes
- **Bilingual UI** (AZ / EN, switchable in Settings), answers follow the language of the question
- **Free-tier friendly**: retries with back-off on Gemini rate limits, batched API calls, and an auto-retry countdown in the UI

## How it works

```
upload / docs/ ──► Tika text extraction ──► LegalTextSplitter (per article)
                                              │
                                              ▼
                         Gemini embeddings ──► PGVector (vector_store + metadata)
                                                         │ top-k similarity search
question ──► embed ──► filter (scope, owner) ────────────┘
                              │
                              ▼
            numbered sources + strict system prompt ──► Gemini ──► SSE stream ──► React UI
```

Pure small talk ("Salam", "sağ ol", "thanks", "bye") skips the search and gets a short reply without sources
(`SmallTalk.java`). Under each answer the UI lists only the sources the answer actually cites (`[1]`, `[2]`…).

## Run it locally

### 1. Prerequisites
Java 21, Docker Desktop, Node.js 20+. Maven is **not** needed (the project ships the Maven wrapper).

### 2. Configure
Copy `.env.example` to `.env` in the project root (`.env` is git-ignored and never committed), then fill in:

```
GEMINI_API_KEY=your-key-here          # free at https://aistudio.google.com/apikey
JWT_SECRET=a-long-random-string       # at least 32 characters
ADMIN_PASSWORD=your-admin-password
```

`JWT_SECRET` and `ADMIN_PASSWORD` are required: the backend does not start without them.
The admin account is created on the first start; changing `ADMIN_PASSWORD` later does not change an existing account's password.

### 3. Start the database
```bash
docker compose up -d
```

### 4. Start the backend (http://localhost:8080)
```bash
cd backend
./mvnw spring-boot:run        # Windows CMD/PowerShell: mvnw.cmd spring-boot:run
```

### 5. Start the frontend (http://localhost:5173)
```bash
cd frontend
npm install
npm run dev
```

Log in with `ADMIN_EMAIL` / `ADMIN_PASSWORD` from your `.env`.

### 6. Add laws
Put PDF/DOCX/TXT files into `docs/` and restart the backend, or upload them in **Admin**.
See `docs/README.md` for a suggested starter set. Then, in **Admin**, click **Generate checklist** on the laws
you want to use for compliance checks.

### Single-jar demo build
```bash
cd frontend && npm run build          # writes the UI into backend/src/main/resources/static
cd ../backend && ./mvnw package
java -jar target/cyberlaw-backend-0.1.0.jar   # everything on http://localhost:8080
```

## Configuration (`backend/src/main/resources/application.yml`)

| Property | Default | Meaning |
|---|---|---|
| `GEMINI_CHAT_MODEL` (.env) | `gemini-2.5-flash` | any Gemini chat model on your plan |
| `cyberlaw.rag.top-k` | 6 | passages sent to the model per question |
| `cyberlaw.rag.similarity-threshold` | 0.3 | minimum similarity for a passage |
| `cyberlaw.rag.chunk-max-chars` | 2000 | max chunk size (long articles are split) |
| `cyberlaw.uploads.max-private-documents` | 5 | per-user upload limit |
| `cyberlaw.uploads.private-ttl` | 7d | private documents are deleted after this |
| `cyberlaw.compliance.max-requirements` | 40 | checklist size per law |
| `cyberlaw.compliance.judge-batch-size` | 8 | requirements judged per Gemini call |

## REST API

| Method | Path | |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | returns a JWT |
| GET | `/api/auth/me` | current user |
| GET / POST / DELETE | `/api/documents` | list / upload (`scope=PRIVATE\|OFFICIAL`) / delete |
| POST | `/api/chat/stream` | SSE: `sources`, `token`, `done`, `error` |
| DELETE | `/api/chat/{conversationId}` | forget a conversation |
| GET | `/api/compliance/laws` | laws that have a checklist |
| GET | `/api/compliance/laws/{id}/requirements` | the checklist |
| POST / GET / DELETE | `/api/compliance/reports` | run / list / delete checks |
| POST | `/api/admin/documents/{id}/requirements` | admin: generate checklist |
| GET | `/api/health` | status and whether the Gemini key is set |

## Notes

- CyberLaw provides information, not legal advice.
- On the free Gemini tier Google may use prompts to improve its models: don't upload confidential documents.
- Scanned PDFs (images without a text layer) cannot be read.
