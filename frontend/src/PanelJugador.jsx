import { useState } from 'react'
import { api, FASES, hora } from './api.js'
import { useSondeo } from './useSondeo.js'

const PESTANAS = [
  ['personaje', 'Personaje'],
  ['objetivos', 'Objetivos'],
  ['pistas', 'Pistas'],
  ['mensajes', 'Mensajes'],
  ['secretos', 'Secretos'],
]

const ESTADOS = { ACTIVO: 'Activo', CUMPLIDO: 'Cumplido', FALLIDO: 'Fallido' }

export default function PanelJugador({ alSalir }) {
  const { datos, error, recargar } = useSondeo('/yo', 8000, alSalir)
  const [pestana, setPestana] = useState('personaje')

  if (!datos) return <p className="centrado">{error || 'Cargando…'}</p>

  const sinLeer = datos.mensajes.filter((m) => !m.leido).length

  async function marcarLeido(id) {
    try {
      await api('POST', `/yo/mensajes/${id}/leido`)
    } finally {
      recargar()
    }
  }

  return (
    <div className="app">
      <header className="cabecera">
        <div>
          <h1>{datos.partida.nombre}</h1>
          <p className="tenue">
            {datos.partida.casa ? `${datos.partida.casa} · ` : ''}
            {datos.personaje.nombre}
          </p>
        </div>
        <span className="fase">{FASES[datos.partida.fase]}</span>
      </header>

      {error && <p className="error">{error}</p>}

      <main className="contenido">
        {pestana === 'personaje' && <Personaje datos={datos} alSalir={alSalir} />}

        {pestana === 'objetivos' && (
          <section>
            <h2>Objetivos</h2>
            {datos.objetivos.length === 0 && <Vacio>Todavía no tienes objetivos.</Vacio>}
            {datos.objetivos.map((o) => (
              <article key={o.id} className="tarjeta">
                <p>{o.texto}</p>
                <span className={`etiqueta ${o.estado.toLowerCase()}`}>{ESTADOS[o.estado]}</span>
              </article>
            ))}
          </section>
        )}

        {pestana === 'pistas' && (
          <section>
            <h2>Pistas descubiertas</h2>
            {datos.pistas.length === 0 && <Vacio>Aún no has descubierto ninguna pista.</Vacio>}
            {datos.pistas.map((p) => (
              <article key={p.id} className="tarjeta">
                <h3>{p.titulo}</h3>
                <p>{p.contenido}</p>
              </article>
            ))}
          </section>
        )}

        {pestana === 'mensajes' && (
          <section>
            <h2>Mensajes del Máster</h2>
            {datos.mensajes.length === 0 && <Vacio>No tienes mensajes.</Vacio>}
            {datos.mensajes.map((m) => (
              <article key={m.id} className={`tarjeta ${m.leido ? '' : 'nuevo'}`}>
                <p>{m.texto}</p>
                <div className="fila">
                  <span className="tenue">{hora(m.enviadoEn)}</span>
                  {!m.leido && (
                    <button className="enlace" onClick={() => marcarLeido(m.id)}>
                      Marcar como leído
                    </button>
                  )}
                </div>
              </article>
            ))}
          </section>
        )}

        {pestana === 'secretos' && (
          <section>
            <h2>Tus secretos</h2>
            {datos.esAsesino && (
              <article className="tarjeta asesino">
                <h3>Eres el asesino</h3>
                <p>Solo tú y el Máster lo sabéis.</p>
              </article>
            )}
            {datos.secretos.length === 0 && !datos.esAsesino && <Vacio>No tienes secretos asignados.</Vacio>}
            {datos.secretos.map((s) => (
              <article key={s.id} className="tarjeta">
                <p>{s.texto}</p>
                {s.revelado && <span className="etiqueta fallido">Revelado al grupo</span>}
              </article>
            ))}

            <h2>Secretos revelados</h2>
            {datos.secretosRevelados.length === 0 && <Vacio>Nadie ha quedado al descubierto todavía.</Vacio>}
            {datos.secretosRevelados.map((s, i) => (
              <article key={i} className="tarjeta">
                <h3>{s.jugador}</h3>
                <p>{s.texto}</p>
              </article>
            ))}
          </section>
        )}
      </main>

      <nav className="pestanas" aria-label="Secciones">
        {PESTANAS.map(([clave, titulo]) => (
          <button key={clave} className={pestana === clave ? 'activa' : ''} onClick={() => setPestana(clave)}>
            {titulo}
            {clave === 'mensajes' && sinLeer > 0 && <span className="globo">{sinLeer}</span>}
          </button>
        ))}
      </nav>
    </div>
  )
}

function Personaje({ datos, alSalir }) {
  const p = datos.personaje
  const basicos = [p.edad && `${p.edad} años`, p.profesion, p.pareja && `Pareja: ${p.pareja}`].filter(Boolean)
  return (
    <section>
      <h2>{p.nombre}</h2>
      {basicos.length > 0 && <p className="tenue">{basicos.join(' · ')}</p>}
      <Bloque titulo="Personalidad" texto={p.personalidad} />
      <Bloque titulo="Relaciones" texto={p.relaciones} />
      <Bloque titulo="Contexto" texto={p.contexto} />
      {!p.personalidad && !p.relaciones && !p.contexto && <Vacio>Tu ficha todavía está en construcción.</Vacio>}

      <h2>En la casa</h2>
      <p>{datos.participantes.join(' · ')}</p>

      <button className="enlace salir" onClick={alSalir}>
        Salir de la partida
      </button>
    </section>
  )
}

function Bloque({ titulo, texto }) {
  if (!texto) return null
  return (
    <article className="tarjeta">
      <h3>{titulo}</h3>
      <p>{texto}</p>
    </article>
  )
}

function Vacio({ children }) {
  return <p className="vacio">{children}</p>
}
