import { useState } from 'react'
import { api, hora } from './api.js'
import Avatar from './Avatar.jsx'
import CarruselPersonajes from './CarruselPersonajes.jsx'
import Nota from './Nota.jsx'

const CATEGORIAS = [
  ['SOSPECHOSO', 'Sospechosos'],
  ['ARMA', 'Armas'],
  ['LUGAR', 'Lugares'],
]

// Orden al tocar: vacío → descartado → dudoso → encaja → vacío. Cada botón también se puede tocar directamente.
const MARCAS = [
  ['NO', '✕', 'Descartado'],
  ['DUDA', '?', 'Dudoso'],
  ['SI', '✓', 'Encaja'],
]

/**
 * Cuaderno del detective: tablero de deducción (sospechosos × armas × lugares, como la hoja
 * del Cluedo), notas por personaje y la acusación final. Todo privado salvo la acusación,
 * que el Máster ve al momento y el grupo al terminar la partida.
 */
export default function Cuaderno({ datos, otros, alAbrir, alGuardarNota, recargar }) {
  const sospechosos = otros.filter((o) => !o.esMaster)
  const yo = datos.personaje
  const armas = [
    yo.herramienta && { id: yo.id, nombre: yo.nombre, herramienta: yo.herramienta, imagenUrl: yo.imagenUrl, mia: true },
    ...sospechosos.filter((o) => o.herramienta),
  ].filter(Boolean)

  return (
    <section>
      <Tablero datos={datos} sospechosos={sospechosos} armas={armas} recargar={recargar} />
      <Notas otros={otros} marcas={datos.deduccion ?? []} alAbrir={alAbrir} alGuardar={alGuardarNota} />
      <AcusacionFinal datos={datos} sospechosos={sospechosos} armas={armas} recargar={recargar} />
    </section>
  )
}

// ---------------------------------------------------------------- Tablero

function Tablero({ datos, sospechosos, armas, recargar }) {
  const [categoria, setCategoria] = useState('SOSPECHOSO')
  const [locales, setLocales] = useState({}) // cambios aún sin confirmar por el servidor
  const [error, setError] = useState(null)

  const servidor = Object.fromEntries((datos.deduccion ?? []).map((m) => [`${m.categoria}|${m.clave}`, m.marca]))
  const marcaDe = (cat, clave) => {
    const k = `${cat}|${clave}`
    return k in locales ? locales[k] : (servidor[k] ?? null)
  }

  async function marcar(cat, clave, marca) {
    const k = `${cat}|${clave}`
    const nueva = marcaDe(cat, clave) === marca ? null : marca
    setLocales((l) => ({ ...l, [k]: nueva }))
    setError(null)
    try {
      await api('PUT', '/yo/deduccion', { categoria: cat, clave: String(clave), marca: nueva })
      await recargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setLocales((l) => {
        const copia = { ...l }
        delete copia[k]
        return copia
      })
    }
  }

  const filas = {
    SOSPECHOSO: sospechosos.map((o) => ({ clave: o.id, titulo: o.nombre, detalle: o.profesion, persona: o })),
    ARMA: armas.map((o) => ({ clave: o.id, titulo: o.herramienta, detalle: o.mia ? 'La tuya' : `De ${o.nombre}`, persona: o })),
    LUGAR: (datos.lugares ?? []).map((l) => ({ clave: l, titulo: l })),
  }
  const lista = filas[categoria]
  const cuenta = (cat) => filas[cat].filter((f) => marcaDe(cat, f.clave) === 'NO').length

  return (
    <>
      <h2>Tablero de deducción</h2>
      <p className="tenue">Como la hoja del Cluedo: tacha lo que vayas descartando. Solo lo ves tú.</p>
      <div className="segmentos" role="tablist" aria-label="Qué deduces">
        {CATEGORIAS.map(([clave, titulo]) => (
          <button
            key={clave}
            type="button"
            role="tab"
            aria-selected={categoria === clave}
            className={categoria === clave ? 'activa' : ''}
            onClick={() => setCategoria(clave)}
          >
            {titulo}
            <span className="segmento-cuenta">
              {cuenta(clave)}/{filas[clave].length}
            </span>
          </button>
        ))}
      </div>
      {error && <p className="error redondeado">{error}</p>}
      {lista.length === 0 ? (
        <p className="vacio">{categoria === 'LUGAR' ? 'El Máster todavía no ha apuntado las estancias de la casa.' : 'Todavía no hay nada que deducir aquí.'}</p>
      ) : (
        <ul className="tablero">
          {lista.map((f) => {
            const marca = marcaDe(categoria, f.clave)
            return (
              <li key={f.clave} className={`deduccion ${marca ? `marca-${marca.toLowerCase()}` : ''}`}>
                {f.persona ? <Avatar nombre={f.persona.nombre} imagenUrl={f.persona.imagenUrl} tamano="mini" /> : <span className="punto-lugar" aria-hidden="true" />}
                <span className="deduccion-texto">
                  <span className="deduccion-titulo">{f.titulo}</span>
                  {f.detalle && <span className="tenue">{f.detalle}</span>}
                </span>
                <span className="tri" role="group" aria-label={`Marca para ${f.titulo}`}>
                  {MARCAS.map(([valor, simbolo, nombre]) => (
                    <button
                      key={valor}
                      type="button"
                      className={`tri-${valor.toLowerCase()}`}
                      aria-pressed={marca === valor}
                      aria-label={nombre}
                      title={nombre}
                      onClick={() => marcar(categoria, f.clave, valor)}
                    >
                      {simbolo}
                    </button>
                  ))}
                </span>
              </li>
            )
          })}
        </ul>
      )}
    </>
  )
}

// ---------------------------------------------------------------- Notas por personaje

const ETIQUETA_MARCA = { NO: 'Descartado', DUDA: 'Dudoso', SI: 'Encaja' }

function Notas({ otros, marcas, alAbrir, alGuardar }) {
  const [elegido, setElegido] = useState(0)
  if (otros.length === 0) return null
  // Si el Máster borra a alguien, el índice no debe quedarse fuera de la lista.
  const indice = Math.min(elegido, otros.length - 1)
  const personaje = otros[indice]
  const marca = marcas.find((m) => m.categoria === 'SOSPECHOSO' && m.clave === String(personaje.id))?.marca

  return (
    <>
      <h2>Notas por personaje</h2>
      <p className="tenue">Desliza para elegir a quién y apunta lo que vayas descubriendo. Se guarda solo.</p>
      <CarruselPersonajes personajes={otros} indice={indice} alCambiar={setElegido} alAbrir={alAbrir} />
      {/* La clave fuerza un cuaderno nuevo por personaje; el anterior guarda lo pendiente al desmontarse. */}
      <article key={personaje.id} className="tarjeta">
        <h3>
          Sobre {personaje.nombre}
          {marca && <span className={`etiqueta marca-${marca.toLowerCase()}`}>{ETIQUETA_MARCA[marca]}</span>}
        </h3>
        <Nota personaje={personaje} alGuardar={alGuardar} />
      </article>
    </>
  )
}

// ---------------------------------------------------------------- Acusación final

function AcusacionFinal({ datos, sospechosos, armas, recargar }) {
  const actual = datos.acusacionFinal
  const [sospechosoId, setSospechosoId] = useState(actual?.sospechosoId ? String(actual.sospechosoId) : '')
  const [armaDeId, setArmaDeId] = useState(actual?.armaDeId ? String(actual.armaDeId) : '')
  const [razon, setRazon] = useState(actual?.razon ?? '')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const cerrada = datos.partida.fase === 'FINALIZADA'
  const cambiada = (actual?.sospechosoId ?? '') + '' !== sospechosoId || (actual?.armaDeId ?? '') + '' !== armaDeId
    || (actual?.razon ?? '') !== razon

  async function guardar(cuerpo) {
    setError(null)
    setEnviando(true)
    try {
      await api('PUT', '/yo/acusacion-final', cuerpo)
      await recargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  function entregar(evento) {
    evento.preventDefault()
    guardar({ sospechosoId: sospechosoId ? Number(sospechosoId) : null, armaDeId: armaDeId ? Number(armaDeId) : null, razon })
  }

  function retirar() {
    setSospechosoId('')
    setArmaDeId('')
    setRazon('')
    guardar({ sospechosoId: null, armaDeId: null, razon: null })
  }

  return (
    <>
      <h2>Tu acusación final</h2>
      <form className={`tarjeta acusacion-final ${actual ? 'entregada' : ''}`} onSubmit={entregar}>
        <p className="tenue">
          Quién lo hizo y con qué. El Máster la ve en cuanto la entregas; el resto, cuando termine la partida. Puedes cambiarla hasta entonces.
        </p>
        {error && <p className="error redondeado">{error}</p>}
        <label>
          El asesino
          <select value={sospechosoId} onChange={(e) => setSospechosoId(e.target.value)} disabled={cerrada}>
            <option value="">Sin decidir</option>
            {sospechosos.map((o) => (
              <option key={o.id} value={o.id}>
                {o.nombre}
              </option>
            ))}
          </select>
        </label>
        <label>
          El arma
          <select value={armaDeId} onChange={(e) => setArmaDeId(e.target.value)} disabled={cerrada}>
            <option value="">Sin decidir</option>
            {armas.map((o) => (
              <option key={o.id} value={o.id}>
                {o.herramienta} ({o.mia ? 'la tuya' : `de ${o.nombre}`})
              </option>
            ))}
          </select>
        </label>
        <label>
          Por qué
          <textarea rows={3} value={razon} onChange={(e) => setRazon(e.target.value)} maxLength={2000} disabled={cerrada} placeholder="Las pruebas que lo demuestran" />
        </label>
        {actual && <p className="tenue">Entregada {hora(actual.en)}.</p>}
        {!cerrada && (
          <div className="botonera">
            <button className="primario" disabled={enviando || !cambiada || (!sospechosoId && !armaDeId && !razon.trim())}>
              {actual ? 'Actualizar acusación' : 'Entregar acusación'}
            </button>
            {actual && (
              <button type="button" className="enlace" onClick={retirar} disabled={enviando}>
                Retirarla
              </button>
            )}
          </div>
        )}
        {cerrada && <p className="tenue">La partida ha terminado: las acusaciones de todos están en el Tablón.</p>}
      </form>
    </>
  )
}
