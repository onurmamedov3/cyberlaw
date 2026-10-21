import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import {BrowserRouter} from 'react-router-dom'
import {SettingsProvider} from './lib/settings'
import {AuthProvider} from './lib/auth'
import {App} from './App'
import {ConfirmProvider} from './components/ConfirmDialog'
import {ErrorBoundary} from './components/ErrorBoundary'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <SettingsProvider>
        <ErrorBoundary>
          <ConfirmProvider>
            <AuthProvider>
              <App />
            </AuthProvider>
          </ConfirmProvider>
        </ErrorBoundary>
      </SettingsProvider>
    </BrowserRouter>
  </StrictMode>,
)
