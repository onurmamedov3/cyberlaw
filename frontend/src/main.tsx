import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import {BrowserRouter} from 'react-router-dom'
import {SettingsProvider} from './lib/settings'
import {AuthProvider} from './lib/auth'
import {App} from './App'
import {ErrorBoundary} from './components/ErrorBoundary'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <SettingsProvider>
        <ErrorBoundary>
          <AuthProvider>
            <App />
          </AuthProvider>
        </ErrorBoundary>
      </SettingsProvider>
    </BrowserRouter>
  </StrictMode>,
)
