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
    <main className="acceso">
      <h1>Misterio en Vivo</h1>
      <p className="tenue">Introduce el código que te ha dado el Máster.</p>
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
    </main>
  )
}
