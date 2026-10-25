export type Role = 'USER' | 'ADMIN'

export interface User {
  id: string
  email: string
  displayName: string
  role: Role
}

export type DocStatus = 'PROCESSING' | 'READY' | 'FAILED'
export type RequirementsStatus = 'NONE' | 'EXTRACTING' | 'READY' | 'FAILED'
export type DocScope = 'OFFICIAL' | 'PRIVATE'

export interface LibraryDocument {
  id: string
  title: string
  filename: string
  scope: DocScope
  status: DocStatus
  errorCode: string | null
  errorMessage: string | null
  chunkCount: number
  language: string | null
  sizeBytes: number
  createdAt: string
  expiresAt: string | null
  requirementsStatus: RequirementsStatus
  mine: boolean
}

export type SearchScope = 'OFFICIAL' | 'MINE' | 'BOTH'

export interface Source {
  index: number
  documentId: string
  documentTitle: string
  scope: DocScope
  article: string
  articleTitle: string
  chapter: string
  text: string
  score: number
  /** In a saved chat: the private document was deleted, so its text is gone. */
  deleted?: boolean
}

/** A saved chat in the sidebar. */
export interface Conversation {
  id: string
  title: string
  scope: SearchScope
  updatedAt: string
}

export interface ConversationDetail {
  conversation: Conversation
  messages: { id: string; role: 'user' | 'assistant'; text: string; sources: Source[]; createdAt: string }[]
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  text: string
  sources?: Source[]
  status: 'searching' | 'streaming' | 'done' | 'error'
  /** The user stopped the answer before it was finished. */
  stopped?: boolean
  errorCode?: string
  errorMessage?: string
  errorParams?: Record<string, unknown>
  question?: string
}

export interface Law {
  id: string
  title: string
  language: string | null
  requirementCount: number
}

export interface Requirement {
  position: number
  article: string
  title: string
  description: string
}

export type Verdict = 'MET' | 'PARTIAL' | 'MISSING'

export interface Report {
  id: string
  policyDocumentId: string
  policyTitle: string | null
  /** The report is deleted together with its policy; null when the policy never expires. */
  policyExpiresAt: string | null
  lawDocumentId: string
  lawTitle: string | null
  status: 'RUNNING' | 'DONE' | 'FAILED'
  language: string
  total: number
  processed: number
  met: number
  partial: number
  missing: number
  score: number | null
  errorCode: string | null
  errorMessage: string | null
  createdAt: string
  finishedAt: string | null
}

export interface ReportResult {
  position: number
  article: string
  title: string
  description: string
  status: Verdict
  explanation: string
  evidence: string
}

export interface ReportDetail {
  report: Report
  results: ReportResult[]
}
