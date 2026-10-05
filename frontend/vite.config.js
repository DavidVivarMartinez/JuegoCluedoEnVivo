import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En desarrollo, /api se redirige al backend: el local por defecto o, si se define
// API_URL, el desplegado (así no hace falta tener el backend arrancado en el PC).
//   PowerShell:  $env:API_URL = 'https://misterio-en-vivo.onrender.com'; npm run dev
const api = (process.env.API_URL || 'http://localhost:8080').replace(/\/+$/, '')

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    proxy: { '/api': { target: api, changeOrigin: true, secure: true } },
  },
})
