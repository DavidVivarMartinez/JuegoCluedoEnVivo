import { useState } from 'react'

export default function Acceso({ alEntrar, error }) {
  const [codigo, setCodigo] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento) {
    evento.preventDefault()
    if (!codigo.trim()) return
    setEnviando(true)
    await alEntrar(codigo)
    setEnviando(false)
  }

  return (
    <div className="escena">
      <EscenaDelCrimen />
      <main className="acceso">
        <div className="acceso-tarjeta">
          <h1>Misterio en Vivo</h1>
          <form onSubmit={enviar}>
            <input
              className="codigo"
              value={codigo}
              onChange={(e) => setCodigo(e.target.value)}
              placeholder="CÓDIGO"
              aria-label="Código de acceso"
              autoCapitalize="characters"
              autoCorrect="off"
              autoComplete="off"
              spellCheck={false}
              maxLength={20}
            />
            <button className="primario" disabled={enviando}>
              {enviando ? 'Entrando…' : 'Entrar'}
            </button>
          </form>
          {error && <p className="error">{error}</p>}
        </div>
      </main>
    </div>
  )
}

// Dos mitades idénticas: al desplazarse media cinta, el bucle no se nota.
const CINTA = 'ESCENA DEL CRIMEN · NO PASAR · '.repeat(16)

/**
 * Fondo decorativo: silueta de tiza que se dibuja sola, cintas policiales en movimiento,
 * marcadores de pruebas, una linterna que barre la escena y destellos de sirena.
 */
function EscenaDelCrimen() {
  return (
    <div className="escena-fondo" aria-hidden="true">
      <span className="sirena roja" />
      <span className="sirena azul" />
      <span className="linterna" />
      <svg className="silueta" viewBox="0 0 200 300">
        <defs>
          {/* Borde irregular, como de tiza sobre baldosa */}
          <filter id="tiza" x="-10%" y="-10%" width="120%" height="120%">
            <feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" seed="7" />
            <feDisplacementMap in="SourceGraphic" scale="3" />
          </filter>
        </defs>
        <g filter="url(#tiza)">
          <circle cx="100" cy="38" r="19" pathLength="1" />
          <path pathLength="1" d="M92 60 L70 68 L40 56 L22 32 L12 40 L32 72 L62 90 L66 140 L60 152 L40 232 L30 272 L16 284 L44 288 L58 242 L88 172 L100 164 L112 172 L140 240 L160 280 L186 278 L168 262 L150 226 L138 152 L134 140 L136 92 L158 104 L176 146 L186 140 L168 96 L130 68 L108 60 Z" />
        </g>
      </svg>
      <span className="marcador uno">1</span>
      <span className="marcador dos">2</span>
      <span className="marcador tres">3</span>
      <div className="cinta arriba">
        <span>{CINTA}</span>
      </div>
      <div className="cinta abajo">
        <span>{CINTA}</span>
      </div>
    </div>
  )
}
