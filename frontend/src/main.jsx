import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'
// Triggers the boot-time API_BASE_URL log; also keeps the constant in the bundle so the
// build-time-baked VITE_API_BASE_URL is visibly committed to the dist/ output.
import './lib/api.js'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
