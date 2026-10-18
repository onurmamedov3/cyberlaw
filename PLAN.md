# CyberLaw: project plan

A small "e-qanun.ai"-style RAG chatbot for cybersecurity law and compliance questions.

## Decisions
- **Name:** CyberLaw
- **LLM:** Google Gemini (free tier): chat + multilingual embeddings with one API key
- **Storage:** PostgreSQL + PGVector in Docker
- **Languages:** documents and questions in Azerbaijani + English; UI switchable AZ/EN in Settings
- **Frontend:** React + Vite + Tailwind, dark cyber/neon theme
- **Accounts:** register/login (JWT), roles USER and ADMIN
- **Hosting:** local (laptop), Docker for the database
- **Extra feature:** compliance checker
- **Goal:** a working web app (not a thesis)

## Knowledge base
| | Official Library | My Documents |
|---|---|---|
| Who uploads | admin (or `docs/` folder) | any user |
| Visible to | everyone | only the owner |
| Lifetime | permanent | 7 days, max 5 files, 10 MB each |

Chat scope switch: Official / Mine / Both.

## Phases
1. ✅ Skeleton: Spring Boot + Spring AI + Docker PostgreSQL/PGVector + Flyway schema
2. ✅ Auth: register/login, JWT, admin seeded from `.env`
3. ✅ Ingestion: Tika parsing, article-aware splitter (unit-tested), PGVector, `docs/` loader, expiry job
4. ✅ RAG chat: scoped retrieval, grounded prompt, SSE streaming, citations, rate-limit retry
5. ✅ React UI: login, chat, documents, admin, settings, AZ/EN
6. ✅ Compliance checker: checklist extraction (admin), batched judging, report UI with score
7. ⏳ First run: install dependencies, start Docker, add the Gemini key, load starter laws, end-to-end test
8. ⏳ Polish after the first real test (prompt tuning, chunk size, UI details)

## Risks and mitigations
- **Gemini free-tier limits:** sequential indexing, batched calls, retry with back-off, pre-index before the demo
- **Prompt injection in uploads:** sources are delimited and marked untrusted in every prompt
- **Privacy:** UI banner: don't upload confidential documents
- **Scanned PDFs:** clear error message
