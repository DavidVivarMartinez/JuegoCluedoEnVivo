import { useState } from 'react'
import { api, FASES, hora } from './api.js'
import { useSondeo } from './useSondeo.js'
import Pestanas from './Pestanas.jsx'
import Avatar from './Avatar.jsx'
import { TablonMaster } from './Tablon.jsx'
import EnviosMaster from './EnviosMaster.jsx'
import RankingMaster from './RankingMaster.jsx'
import HojaQr from './HojaQr.jsx'
import ImportarMd from './ImportarMd.jsx'

const PESTANAS = [
  ['partida', 'Partida'],
  ['jugadores', 'Jugadores'],
  ['pistas', 'Pistas'],
  ['envios', 'Envíos'],
  ['ranking', 'Ranking'],
  ['dinero', 'Dinero'],
]

const CAMPOS_FICHA = [
  ['imagenUrl', 'Foto (enlace a una imagen)', false],
  ['profesion', 'Profesión', false],
  ['herramienta', 'Herramienta de su oficio (arma si es asesino)', false],
  ['pareja', 'Pareja', false],
  ['personalidad', 'Personalidad', true],
  ['relaciones', 'Relaciones', true],
  ['contexto', 'Contexto', true],
  ['loQueSabeMaster', 'Lo que sabe el Máster (privado)', true],
  ['motivoPotencial', 'Motivo potencial (privado)', true],
]

// Ficha pública del Máster: lo que los jugadores ven de él en el carrusel de personajes.
const CAMPOS_MASTER = [
  ['imagenUrl', 'Foto (enlace a una imagen)', false],
  ['profesion', 'Profesión', false],
  ['pareja', 'Pareja', false],
]

export default function PanelMaster({ sesion, alSalir }) {
  const { datos, error, recargar } = useSondeo('/master/estado', 6000, alSalir)
  const [pestana, setPestana] = useState('partida')
  const [aviso, setAviso] = useState(null)

  if (!datos) return <p className="centrado">{error || 'Cargando…'}</p>

  // Ejecuta una acción del Máster, muestra el error si falla y refresca el estado.
  async function hacer(metodo, ruta, cuerpo) {
    setAviso(null)
    try {
      await api(metodo, `/master${ruta}`, cuerpo)
      return true
    } catch (e) {
      setAviso(e.message)
      return false
    } finally {
      recargar()
    }
  }

  return (
    <div className="app master">
      <header className="cabecera">
        <div className="cabecera-fila">
          <div className="cabecera-texto">
            <h1>{datos.partida.nombre}</h1>
            <p className="tenue">
              Máster · {sesion.nombre}
              {datos.partida.casa ? ` · ${datos.partida.casa}` : ''}
            </p>
          </div>
          <span className="fase">{FASES[datos.partida.fase]}</span>
        </div>
        <Pestanas pestanas={PESTANAS} activa={pestana} alCambiar={setPestana} globos={{ partida: pendientes(datos), envios: (datos.envios ?? []).filter((e) => e.tipo === 'VISION' && !e.enviadoEn && !e.imagenUrl && e.programadaPara).length }} />
      </header>

      {(aviso || error) && <p className="error">{aviso || error}</p>}

      <main className="contenido">
        {pestana === 'partida' && <Partida datos={datos} hacer={hacer} alSalir={alSalir} recargar={recargar} />}
        {pestana === 'jugadores' && <Jugadores datos={datos} hacer={hacer} />}
        {pestana === 'pistas' && <Pistas datos={datos} hacer={hacer} />}
        {pestana === 'envios' && <EnviosMaster datos={datos} hacer={hacer} />}
        {pestana === 'ranking' && <RankingMaster datos={datos} />}
        {pestana === 'dinero' && <DineroMaster datos={datos} hacer={hacer} />}
      </main>
    </div>
  )
}

/** Lo que espera una respuesta del Máster: objetivos entregados, habilidades usadas y pistas falsas. */
function pendientes(datos) {
  const objetivos = datos.jugadores.reduce((n, j) => n + j.objetivos.filter((o) => o.estado === 'ENTREGADO').length, 0)
  const habilidades = datos.jugadores.reduce((n, j) => n + (j.habilidades ?? []).filter((h) => h.estado === 'SOLICITADA').length, 0)
  const falsas = datos.pistas.filter((p) => p.falsa && p.estadoFalsa === 'PENDIENTE').length
  return objetivos + habilidades + falsas
}

// ---------------------------------------------------------------- Partida

function Partida({ datos, hacer, alSalir, recargar }) {
  const [clave, setClave] = useState('')
  const [valor, setValor] = useState('')

  async function guardarVariable(evento) {
    evento.preventDefault()
    if (!clave.trim()) return
    if (await hacer('PUT', `/variables/${encodeURIComponent(clave.trim())}`, { valor })) {
      setClave('')
      setValor('')
    }
  }

  return (
    <section>
      <Pendientes datos={datos} hacer={hacer} />

      <h2>Fase</h2>
      <div className="botonera">
        {Object.entries(FASES).map(([fase, titulo]) => (
          <button
            key={fase}
            className={datos.partida.fase === fase ? 'primario' : ''}
            onClick={() => hacer('PUT', '/fase', { fase })}
          >
            {titulo}
          </button>
        ))}
      </div>

      <Asesinos datos={datos} hacer={hacer} />

      <TablonMaster entradas={datos.tablon ?? []} hacer={hacer} />

      <Reglas datos={datos} hacer={hacer} recargar={recargar} />

      <h2>Variables narrativas</h2>
      {datos.variables.map((v) => (
        <article key={v.clave} className="tarjeta fila">
          <p>
            <strong>{v.clave}</strong> = {v.valor}
          </p>
          <button className="enlace" onClick={() => hacer('DELETE', `/variables/${encodeURIComponent(v.clave)}`)}>
            Borrar
          </button>
        </article>
      ))}
      <form className="fila" onSubmit={guardarVariable}>
        <input value={clave} onChange={(e) => setClave(e.target.value)} placeholder="clave" aria-label="Clave" />
        <input value={valor} onChange={(e) => setValor(e.target.value)} placeholder="valor" aria-label="Valor" />
        <button>Guardar</button>
      </form>

      <h2>Registro</h2>
      {datos.eventos.map((e) => (
        <p key={e.id} className="registro">
          <span className="tenue">{hora(e.creadoEn)}</span> {e.descripcion}
        </p>
      ))}

      <button className="enlace salir" onClick={alSalir}>
        Salir
      </button>
    </section>
  )
}

/** Bandeja del Máster: lo que los jugadores esperan que contestes. */
function Pendientes({ datos, hacer }) {
  const habilidades = datos.jugadores.flatMap((j) => (j.habilidades ?? []).filter((h) => h.estado === 'SOLICITADA').map((h) => ({ ...h, jugador: j.nombre })))
  const falsas = datos.pistas.filter((p) => p.falsa && p.estadoFalsa === 'PENDIENTE')
  const objetivos = datos.jugadores.flatMap((j) => j.objetivos.filter((o) => o.estado === 'ENTREGADO').map((o) => ({ ...o, jugador: j.nombre })))
  if (habilidades.length + falsas.length + objetivos.length === 0) return null
  const nombre = new Map(datos.jugadores.map((j) => [j.id, j.nombre]))

  return (
    <>
      <h2>Te esperan ({habilidades.length + falsas.length + objetivos.length})</h2>
      {habilidades.map((h) => (
        <RespuestaHabilidad key={h.id} habilidad={h} hacer={hacer} />
      ))}
      {falsas.map((p) => (
        <RevisarFalsa key={p.id} pista={p} autor={nombre.get(p.autorFalsaId)} para={nombre.get(p.falsaParaId)} hacer={hacer} />
      ))}
      {objetivos.map((o) => (
        <article key={o.id} className="tarjeta pendiente">
          <p>
            <strong>{o.jugador}</strong> entrega: {o.texto}
          </p>
          {o.entregas.length > 0 && (
            <ul className="entregas">
              {o.entregas.map((e) => (
                <li key={e.id}>
                  <span>
                    {e.persona && <strong>{e.persona}</strong>}
                    {e.persona && e.texto ? ': ' : ''}
                    {e.texto || (!e.persona ? 'Marcado como conseguido' : '')}
                  </span>
                </li>
              ))}
            </ul>
          )}
          <div className="botonera">
            <button className="primario" onClick={() => hacer('PUT', `/objetivos/${o.id}`, { estado: 'CUMPLIDO' })}>
              Validar
            </button>
            <button onClick={() => hacer('PUT', `/objetivos/${o.id}`, { estado: 'ACTIVO' })}>Devolver</button>
          </div>
        </article>
      ))}
    </>
  )
}

function RespuestaHabilidad({ habilidad: h, hacer }) {
  const [texto, setTexto] = useState('')
  async function contestar(evento) {
    evento.preventDefault()
    if (await hacer('POST', `/habilidades/${h.id}/resolver`, { respuesta: texto })) setTexto('')
  }
  return (
    <form className="tarjeta pendiente" onSubmit={contestar}>
      <p>
        <strong>{h.jugador}</strong> usa «{h.nombre}»{h.persona ? <> sobre <strong>{h.persona}</strong></> : null}
      </p>
      {h.peticion && <p className="cita">«{h.peticion}»</p>}
      <p className="tenue">{h.descripcion}</p>
      <textarea rows={2} value={texto} onChange={(e) => setTexto(e.target.value)} placeholder="Tu respuesta (le llega al momento)" aria-label="Respuesta" />
      <button className="primario" disabled={!texto.trim()}>
        Contestar
      </button>
    </form>
  )
}

function RevisarFalsa({ pista, autor, para, hacer }) {
  const [titulo, setTitulo] = useState(pista.titulo)
  const [contenido, setContenido] = useState(pista.contenido)
  const [precio, setPrecio] = useState('400')
  return (
    <article className="tarjeta pendiente asesino">
      <h3>Pista falsa de {autor ?? 'un asesino'}</h3>
      <p className="tenue">
        Quiere colarla {para ? `en el móvil de ${para}` : 'en la tienda'}. Ya la ha pagado; si la rechazas, se le devuelve el dinero.
      </p>
      <input value={titulo} onChange={(e) => setTitulo(e.target.value)} aria-label="Título" />
      <textarea rows={3} value={contenido} onChange={(e) => setContenido(e.target.value)} aria-label="Contenido" />
      {!para && (
        <label className="campo-numero">
          Precio en la tienda
          <input type="number" inputMode="numeric" min="0" value={precio} onChange={(e) => setPrecio(e.target.value)} />
        </label>
      )}
      <div className="botonera">
        <button className="primario" onClick={() => hacer('POST', `/pistas/${pista.id}/falsa/aprobar`, { titulo, contenido, precio: para ? null : Number(precio) || 0 })}>
          Aprobar y colarla
        </button>
        <button onClick={() => hacer('POST', `/pistas/${pista.id}/falsa/rechazar`)}>Rechazar</button>
      </div>
    </article>
  )
}

/** Estancias de la casa (para el cuaderno), precio de la pista falsa e importación del personajes.md. */
function Reglas({ datos, hacer, recargar }) {
  const [lugares, setLugares] = useState(datos.lugares ?? '')
  const [guardado, setGuardado] = useState(false)

  async function guardar(evento) {
    evento.preventDefault()
    setGuardado(await hacer('PUT', '/reglas', { lugares }))
  }

  return (
    <>
      <h2>Reglas y datos</h2>
      <form className="tarjeta" onSubmit={guardar}>
        <label>
          Estancias de la casa (una por línea): salen en el cuaderno de los jugadores
          <textarea rows={5} value={lugares} onChange={(e) => { setLugares(e.target.value); setGuardado(false) }} />
        </label>
        <button className="primario">{guardado ? 'Guardado' : 'Guardar estancias'}</button>
        <div className="fila">
          <CampoNumero
            key={datos.precioPistaFalsa}
            inicial={datos.precioPistaFalsa}
            etiqueta="Lo que paga el asesino por una pista falsa"
            alGuardar={(n) => hacer('PUT', '/reglas', { precioPistaFalsa: n ?? 0 })}
          />
        </div>
      </form>
      <h3>Importar personajes.md</h3>
      <p className="tenue">Fichas, secretos, objetivos, habilidades, pistas, QR, sobres y visiones de un tirón. Se puede repetir: actualiza lo que haya cambiado.</p>
      <ImportarMd recargar={recargar} />
    </>
  )
}

// ---------------------------------------------------------------- Jugadores

function Jugadores({ datos, hacer }) {
  const [nombre, setNombre] = useState('')
  const [abierto, setAbierto] = useState(null)

  async function crear(evento) {
    evento.preventDefault()
    if (!nombre.trim()) return
    if (await hacer('POST', '/jugadores', { nombre })) setNombre('')
  }

  return (
    <section>
      <h2>Tu personaje</h2>
      <article className="tarjeta">
        <FichaMaster key={datos.master.id} master={datos.master} hacer={hacer} />
      </article>

      <h2>Jugadores ({datos.jugadores.length})</h2>
      {datos.jugadores.map((j) => (
        <article key={j.id} className="tarjeta">
          <button className="plegable" onClick={() => setAbierto(abierto === j.id ? null : j.id)}>
            <Avatar nombre={j.nombre} imagenUrl={j.imagenUrl} />
            <span className="plegable-texto">
              <span>
                {j.nombre}
                {j.asesino && <span className="etiqueta fallido">Asesino</span>}
              </span>
              <span className="tenue">
                {j.dinero} monedas · {j.objetivos.filter((o) => o.estado === 'CUMPLIDO').length}/{j.objetivos.length} objetivos
                {j.objetivos.some((o) => o.estado === 'ENTREGADO') && (
                  <span className="etiqueta entregado">{j.objetivos.filter((o) => o.estado === 'ENTREGADO').length} por validar</span>
                )}
              </span>
            </span>
          </button>
          {abierto === j.id && <Ficha key={j.id} jugador={j} pistas={datos.pistas} hacer={hacer} />}
        </article>
      ))}

      <form className="fila" onSubmit={crear}>
        <input value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Nombre del nuevo jugador" aria-label="Nombre del nuevo jugador" />
        <button>Añadir</button>
      </form>
    </section>
  )
}

function Ficha({ jugador, pistas, hacer }) {
  const [ficha, setFicha] = useState({
    nombre: jugador.nombre,
    edad: jugador.edad ?? '',
    ...Object.fromEntries(CAMPOS_FICHA.map(([campo]) => [campo, jugador[campo] ?? ''])),
  })
  const [guardado, setGuardado] = useState(false)
  const [copiado, setCopiado] = useState(false)
  const enlace = `${window.location.origin}/?codigo=${jugador.codigoAcceso}`

  function cambiar(campo, valor) {
    setFicha({ ...ficha, [campo]: valor })
    setGuardado(false)
  }

  async function guardar(evento) {
    evento.preventDefault()
    const ok = await hacer('PUT', `/jugadores/${jugador.id}`, {
      ...ficha,
      edad: ficha.edad === '' ? null : Number(ficha.edad),
    })
    setGuardado(ok)
  }

  async function copiar() {
    try {
      await navigator.clipboard.writeText(enlace)
      setCopiado(true)
    } catch {
      setCopiado(false)
    }
  }

  function borrar() {
    hacer('DELETE', `/jugadores/${jugador.id}`)
  }

  return (
    <div className="ficha">
      <p>
        Código: <code>{jugador.codigoAcceso}</code>{' '}
        <button className="enlace" onClick={copiar}>
          {copiado ? 'Enlace copiado' : 'Copiar enlace'}
        </button>
      </p>

      <form onSubmit={guardar}>
        <CamposFicha campos={CAMPOS_FICHA} ficha={ficha} cambiar={cambiar} />
        <button className="primario">{guardado ? 'Ficha guardada' : 'Guardar ficha'}</button>
      </form>

      <Lista
        titulo="Secretos"
        elementos={jugador.secretos}
        alCrear={(texto) => hacer('POST', `/jugadores/${jugador.id}/secretos`, { texto })}
        alBorrar={(s) => hacer('DELETE', `/secretos/${s.id}`)}
        acciones={(s) => (
          <button className="enlace" onClick={() => hacer('PUT', `/secretos/${s.id}`, { revelado: !s.revelado })}>
            {s.revelado ? 'Ocultar' : 'Revelar al grupo'}
          </button>
        )}
        marca={(s) => s.revelado && <span className="etiqueta fallido">Revelado</span>}
      />

      <ObjetivosMaster jugador={jugador} pistas={pistas} hacer={hacer} />

      <HabilidadesMaster jugador={jugador} hacer={hacer} />

      <Confirmar texto="Eliminar jugador" pregunta="¿Eliminar y borrar todo lo suyo?" alConfirmar={borrar} />
    </div>
  )
}

/** Campos de una ficha (nombre y edad siempre; el resto según "campos"). La foto se previsualiza. */
function CamposFicha({ campos, ficha, cambiar }) {
  return (
    <>
      <label>
        Nombre
        <input value={ficha.nombre} onChange={(e) => cambiar('nombre', e.target.value)} />
      </label>
      <label>
        Edad
        <input type="number" min="0" value={ficha.edad} onChange={(e) => cambiar('edad', e.target.value)} />
      </label>
      {campos.map(([campo, titulo, largo]) => (
        <label key={campo}>
          {titulo}
          {largo ? (
            <textarea rows={3} value={ficha[campo]} onChange={(e) => cambiar(campo, e.target.value)} />
          ) : campo === 'imagenUrl' ? (
            <span className="fila-imagen">
              <Avatar nombre={ficha.nombre} imagenUrl={ficha.imagenUrl.trim()} />
              <input
                value={ficha.imagenUrl}
                onChange={(e) => cambiar('imagenUrl', e.target.value)}
                placeholder="https://…"
                inputMode="url"
                autoCapitalize="none"
                autoCorrect="off"
                spellCheck={false}
              />
            </span>
          ) : (
            <input value={ficha[campo]} onChange={(e) => cambiar(campo, e.target.value)} />
          )}
        </label>
      ))}
    </>
  )
}

/** Ficha pública del propio Máster: aparece como un personaje más en el carrusel de los jugadores. */
function FichaMaster({ master, hacer }) {
  const [ficha, setFicha] = useState({
    nombre: master.nombre,
    edad: master.edad ?? '',
    ...Object.fromEntries(CAMPOS_MASTER.map(([campo]) => [campo, master[campo] ?? ''])),
  })
  const [guardado, setGuardado] = useState(false)

  function cambiar(campo, valor) {
    setFicha({ ...ficha, [campo]: valor })
    setGuardado(false)
  }

  async function guardar(evento) {
    evento.preventDefault()
    setGuardado(await hacer('PUT', '/ficha', { ...ficha, edad: ficha.edad === '' ? null : Number(ficha.edad) }))
  }

  return (
    <form onSubmit={guardar}>
      <p className="tenue">Así te ven los jugadores entre los personajes de la casa.</p>
      <CamposFicha campos={CAMPOS_MASTER} ficha={ficha} cambiar={cambiar} />
      <button className="primario">{guardado ? 'Guardado' : 'Guardar'}</button>
    </form>
  )
}

const TIPOS_OBJETIVO = { LOGRO: 'Marcar conseguido', TEXTO: 'Respuesta escrita', PERSONA: 'Personaje + texto' }
const ESTADOS_OBJETIVO = { ACTIVO: 'Activo', ENTREGADO: 'Pendiente de validar', CUMPLIDO: 'Cumplido', FALLIDO: 'Fallido' }

/** Objetivos de un jugador con sus pruebas: el Máster valida, elige la pista de recompensa y crea nuevos. */
function ObjetivosMaster({ jugador, pistas, hacer }) {
  const [texto, setTexto] = useState('')
  const [tipo, setTipo] = useState('PERSONA')
  const [cantidad, setCantidad] = useState('1')

  async function crear(evento) {
    evento.preventDefault()
    if (!texto.trim()) return
    if (await hacer('POST', `/jugadores/${jugador.id}/objetivos`, { texto, tipo, cantidad: Number(cantidad) || 1 })) setTexto('')
  }

  const porValidar = jugador.objetivos.filter((o) => o.estado === 'ENTREGADO').length

  return (
    <div className="lista">
      <h3>
        Objetivos
        {porValidar > 0 && <span className="etiqueta entregado">{porValidar} por validar</span>}
      </h3>
      {jugador.objetivos.map((o) => (
        <div key={o.id} className={`elemento ${o.estado === 'ENTREGADO' ? 'por-validar' : ''}`}>
          <p>
            {o.texto}
            {o.principal && <span className="etiqueta principal">Principal</span>}
          </p>
          <p className="tenue">
            {TIPOS_OBJETIVO[o.tipo]}
            {o.tipo !== 'LOGRO' && o.cantidad > 1 ? ` × ${o.cantidad}` : ''}
            {' · '}
            {ESTADOS_OBJETIVO[o.estado]}
            {o.recompensaDinero > 0 ? ` · paga ${o.recompensaDinero} monedas` : ''}
          </p>
          {o.entregas.length > 0 && (
            <ul className="entregas">
              {o.entregas.map((e) => (
                <li key={e.id}>
                  <span>
                    {e.persona && <strong>{e.persona}</strong>}
                    {e.persona && e.texto ? ': ' : ''}
                    {e.texto || (!e.persona ? 'Marcado como conseguido' : '')}
                  </span>
                  <span className="tenue">{hora(e.creadoEn)}</span>
                </li>
              ))}
            </ul>
          )}
          <div className="fila">
            {o.estado === 'ENTREGADO' && (
              <button className="primario" onClick={() => hacer('PUT', `/objetivos/${o.id}`, { estado: 'CUMPLIDO' })}>
                Validar
              </button>
            )}
            <select
              value={o.estado}
              onChange={(e) => hacer('PUT', `/objetivos/${o.id}`, { estado: e.target.value })}
              aria-label="Estado del objetivo"
            >
              {Object.entries(ESTADOS_OBJETIVO).map(([valor, titulo]) => (
                <option key={valor} value={valor}>
                  {titulo}
                </option>
              ))}
            </select>
            <select
              value={o.recompensaPistaId ?? ''}
              onChange={(e) => hacer('PUT', `/objetivos/${o.id}/recompensa`, { pistaId: e.target.value ? Number(e.target.value) : null })}
              aria-label="Pista que desbloquea al cumplirlo"
            >
              <option value="">Sin pista al cumplirlo</option>
              {pistas.map((p) => (
                <option key={p.id} value={p.id}>
                  Desbloquea: {p.titulo}
                </option>
              ))}
            </select>
            <CampoNumero
              key={`${o.id}-${o.recompensaDinero}`}
              inicial={o.recompensaDinero}
              etiqueta="Monedas al cumplirlo"
              alGuardar={(n) => hacer('PUT', `/objetivos/${o.id}`, { recompensaDinero: n ?? 0 })}
            />
            {!o.principal && (
              <button className="enlace" onClick={() => hacer('PUT', `/objetivos/${o.id}`, { principal: true })}>
                Hacer principal
              </button>
            )}
            <button className="enlace" onClick={() => hacer('DELETE', `/objetivos/${o.id}`)}>
              Borrar
            </button>
          </div>
        </div>
      ))}
      <form onSubmit={crear}>
        <input value={texto} onChange={(e) => setTexto(e.target.value)} placeholder="Nuevo objetivo" aria-label="Nuevo objetivo" />
        <div className="fila">
          <select value={tipo} onChange={(e) => setTipo(e.target.value)} aria-label="Tipo de prueba">
            {Object.entries(TIPOS_OBJETIVO).map(([valor, titulo]) => (
              <option key={valor} value={valor}>
                {titulo}
              </option>
            ))}
          </select>
          {tipo !== 'LOGRO' && (
            <input type="number" min="1" max="20" value={cantidad} onChange={(e) => setCantidad(e.target.value)} aria-label="Cuántas" />
          )}
          <button>Añadir</button>
        </div>
      </form>
    </div>
  )
}

const TIPOS_HABILIDAD = {
  MANUAL: 'La contesta el Máster',
  VER_PISTAS: 'Ver las pistas de alguien',
  VER_MOVIMIENTOS: 'Ver el dinero de alguien',
  RASTREAR_PAGOS: 'Ver los últimos pagos',
  PISTA_AL_AZAR: 'Pista gratis al azar',
  RUMOR_ANONIMO: 'Rumor anónimo',
}
const ESTADOS_HABILIDAD = { DISPONIBLE: 'Sin usar', SOLICITADA: 'Esperando tu respuesta', RESUELTA: 'Usada' }

function HabilidadesMaster({ jugador, hacer }) {
  const [nombre, setNombre] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [tipo, setTipo] = useState('MANUAL')
  const [pidePersona, setPidePersona] = useState(false)
  const [pideTexto, setPideTexto] = useState('')

  async function crear(evento) {
    evento.preventDefault()
    if (await hacer('POST', `/jugadores/${jugador.id}/habilidades`, { nombre, descripcion, tipo, pidePersona, pideTexto })) {
      setNombre('')
      setDescripcion('')
      setPideTexto('')
      setPidePersona(false)
    }
  }

  return (
    <div className="lista">
      <h3>Habilidades de oficio</h3>
      {(jugador.habilidades ?? []).map((h) => (
        <div key={h.id} className={`elemento ${h.estado === 'SOLICITADA' ? 'por-validar' : ''}`}>
          <p>
            <strong>{h.nombre}</strong>
            <span className="etiqueta">{ESTADOS_HABILIDAD[h.estado]}</span>
          </p>
          <p className="tenue">
            {TIPOS_HABILIDAD[h.tipo]} · {h.descripcion}
          </p>
          {h.estado !== 'DISPONIBLE' && (
            <p className="tenue">
              Usada {hora(h.usadaEn)}
              {h.persona ? ` sobre ${h.persona}` : ''}
              {h.peticion ? `: «${h.peticion}»` : ''}
              {h.respuesta ? ` → ${h.respuesta}` : ''}
            </p>
          )}
          <div className="fila">
            {h.estado !== 'DISPONIBLE' && (
              <button className="enlace" onClick={() => hacer('POST', `/habilidades/${h.id}/resolver`, { estado: 'DISPONIBLE' })}>
                Reponer (otro uso)
              </button>
            )}
            <button className="enlace" onClick={() => hacer('DELETE', `/habilidades/${h.id}`)}>
              Borrar
            </button>
          </div>
        </div>
      ))}
      <form onSubmit={crear}>
        <input value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Nombre de la habilidad" aria-label="Nombre de la habilidad" />
        <select value={tipo} onChange={(e) => setTipo(e.target.value)} aria-label="Qué hace">
          {Object.entries(TIPOS_HABILIDAD).map(([valor, titulo]) => (
            <option key={valor} value={valor}>
              {titulo}
            </option>
          ))}
        </select>
        <textarea rows={2} value={descripcion} onChange={(e) => setDescripcion(e.target.value)} placeholder="Qué le dice al jugador" aria-label="Descripción" />
        {tipo === 'MANUAL' && (
          <>
            <label className="casilla">
              <input type="checkbox" checked={pidePersona} onChange={(e) => setPidePersona(e.target.checked)} />
              Elige a un personaje
            </label>
            <input value={pideTexto} onChange={(e) => setPideTexto(e.target.value)} placeholder="Pregunta que escribe el jugador (opcional)" aria-label="Pregunta" />
          </>
        )}
        <button disabled={!nombre.trim() || !descripcion.trim()}>Añadir habilidad</button>
      </form>
    </div>
  )
}

function Lista({ titulo, elementos, alCrear, alBorrar, acciones, marca }) {
  const [texto, setTexto] = useState('')

  async function crear(evento) {
    evento.preventDefault()
    if (!texto.trim()) return
    if (await alCrear(texto)) setTexto('')
  }

  return (
    <div className="lista">
      <h3>{titulo}</h3>
      {elementos.map((e) => (
        <div key={e.id} className="elemento">
          <p>
            {e.texto} {marca && marca(e)}
          </p>
          <div className="fila">
            {acciones(e)}
            <button className="enlace" onClick={() => alBorrar(e)}>
              Borrar
            </button>
          </div>
        </div>
      ))}
      <form className="fila" onSubmit={crear}>
        <input value={texto} onChange={(e) => setTexto(e.target.value)} placeholder={`Añadir a ${titulo.toLowerCase()}`} aria-label={`Añadir a ${titulo.toLowerCase()}`} />
        <button>Añadir</button>
      </form>
    </div>
  )
}

function Confirmar({ texto, pregunta, alConfirmar, confirmacion = 'Sí, eliminar' }) {
  const [preguntando, setPreguntando] = useState(false)
  if (!preguntando) {
    return (
      <button className="enlace peligro" onClick={() => setPreguntando(true)}>
        {texto}
      </button>
    )
  }
  return (
    <p className="fila">
      <span>{pregunta}</span>
      <button className="enlace peligro" onClick={alConfirmar}>
        {confirmacion}
      </button>
      <button className="enlace" onClick={() => setPreguntando(false)}>
        Cancelar
      </button>
    </p>
  )
}

/** Número que se guarda al salir del campo o pulsar Intro; vacío = null si se permite. */
function CampoNumero({ inicial, etiqueta, alGuardar, vacioPermitido = false }) {
  const [valor, setValor] = useState(inicial == null ? '' : String(inicial))

  function guardar() {
    const limpio = valor.trim()
    const anterior = inicial == null ? '' : String(inicial)
    if (limpio === anterior) return
    if (limpio === '') {
      alGuardar(vacioPermitido ? null : 0)
      return
    }
    const n = Number(limpio)
    if (Number.isInteger(n) && n >= 0) alGuardar(n)
    else setValor(anterior)
  }

  return (
    <label className="campo-numero">
      {etiqueta}
      <input
        type="number"
        inputMode="numeric"
        min="0"
        value={valor}
        onChange={(e) => setValor(e.target.value)}
        onBlur={guardar}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            e.preventDefault()
            e.currentTarget.blur()
          }
        }}
      />
    </label>
  )
}

// ---------------------------------------------------------------- Asesinos

/** Puede haber varios asesinos. Cada uno elige su arma desde el móvil; aquí se ve y se genera la pista. */
function Asesinos({ datos, hacer }) {
  const asesinos = datos.jugadores.filter((j) => j.asesino)
  const porId = new Map(datos.jugadores.map((j) => [j.id, j]))
  const pistaArma = datos.pistas.find((p) => p.titulo === 'El arma')

  return (
    <>
      <h2>Asesinos</h2>
      <p className="tenue">
        El último (o los dos últimos) en quedarse contigo el viernes. Lo ven en su pestaña Pistas y, si son dos, saben quién es su cómplice.
      </p>
      <div className="casillas">
        {datos.jugadores.map((j) => (
          <label key={j.id}>
            <input type="checkbox" checked={j.asesino} onChange={() => hacer('PUT', `/jugadores/${j.id}/asesino`, { asesino: !j.asesino })} />
            {j.nombre}
          </label>
        ))}
      </div>
      {asesinos.length > 2 && <p className="error redondeado">Hay {asesinos.length} asesinos marcados. ¿Seguro?</p>}

      {asesinos.length > 0 && (
        <article className="tarjeta asesino">
          {asesinos.map((a) => {
            const duenio = a.armaDeId ? porId.get(a.armaDeId) : null
            return (
              <p key={a.id}>
                <strong>{a.nombre}</strong>:{' '}
                {duenio
                  ? `${duenio.herramienta}${duenio.id === a.id ? ' (la suya)' : `, la de ${duenio.nombre} (le incrimina)`}`
                  : 'todavía no ha elegido arma'}
              </p>
            )
          })}
          <div className="fila">
            <button onClick={() => hacer('POST', '/arma/pista')} disabled={!asesinos.some((a) => a.armaDeId)}>
              {pistaArma ? 'Actualizar la pista «El arma»' : 'Generar la pista «El arma»'}
            </button>
          </div>
          <p className="tenue">
            La pista dice el oficio, no el nombre, y se crea bloqueada: desbloquéala en Pistas cuando quieras. El arma no se puede cambiar desde que la fase pasa a Investigación.
          </p>
        </article>
      )}
    </>
  )
}

// ---------------------------------------------------------------- Dinero

function DineroMaster({ datos, hacer }) {
  const [cantidad, setCantidad] = useState('')
  const [concepto, setConcepto] = useState('')
  const [destinos, setDestinos] = useState([])
  const n = Number(cantidad)
  const valido = Number.isInteger(n) && n !== 0

  function alternar(id) {
    setDestinos(destinos.includes(id) ? destinos.filter((d) => d !== id) : [...destinos, id])
  }

  async function enviar(evento) {
    evento.preventDefault()
    if (!valido) return
    if (await hacer('POST', '/dinero', { cantidad: n, concepto, jugadorIds: destinos })) {
      setCantidad('')
      setConcepto('')
      setDestinos([])
    }
  }

  const total = datos.jugadores.reduce((suma, j) => suma + j.dinero, 0)
  const ranking = [...datos.jugadores].sort((a, b) => b.dinero - a.dinero)
  const enVenta = datos.pistas.filter((p) => p.precio != null).length

  return (
    <section>
      <h2>Saldos</h2>
      <p className="tenue">
        {total} monedas en juego · {enVenta} pistas en la tienda
      </p>
      <ul className="movimientos">
        {ranking.map((j) => (
          <li key={j.id}>
            <span className="importe entra">{j.dinero}</span>
            <span className="movimiento-texto">{j.nombre}</span>
            <span />
          </li>
        ))}
      </ul>

      <h2>Dar o quitar dinero</h2>
      <form onSubmit={enviar} className="tarjeta">
        <input
          type="number"
          inputMode="numeric"
          value={cantidad}
          onChange={(e) => setCantidad(e.target.value)}
          placeholder="Cantidad (negativa para quitar)"
          aria-label="Cantidad"
        />
        <input value={concepto} onChange={(e) => setConcepto(e.target.value)} placeholder="Concepto (lo verán los jugadores)" aria-label="Concepto" />
        <div className="casillas">
          {datos.jugadores.map((j) => (
            <label key={j.id}>
              <input type="checkbox" checked={destinos.includes(j.id)} onChange={() => alternar(j.id)} />
              {j.nombre}
            </label>
          ))}
        </div>
        <button className="primario" disabled={!valido}>
          {!valido
            ? 'Indica una cantidad'
            : `${n > 0 ? 'Dar' : 'Quitar'} ${Math.abs(n)} a ${destinos.length === 0 ? 'todos' : destinos.length}`}
        </button>
      </form>

      <h2>Movimientos</h2>
      {datos.transacciones.length === 0 && <p className="vacio">Todavía no se ha movido dinero.</p>}
      <ul className="movimientos">
        {datos.transacciones.map((t) => (
          <li key={t.id}>
            <span className="importe">{t.cantidad}</span>
            <span className="movimiento-texto">
              <span>
                {t.de ?? 'Banca'} → {t.para ?? 'Banca'}
              </span>
              {t.concepto && <span className="tenue">{t.concepto}</span>}
            </span>
            <span className="tenue">{hora(t.creadoEn)}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}

// ---------------------------------------------------------------- Pistas

function Pistas({ datos, hacer }) {
  const [titulo, setTitulo] = useState('')
  const [contenido, setContenido] = useState('')
  const [buscar, setBuscar] = useState('')
  const [imprimiendo, setImprimiendo] = useState(false)
  const nombre = new Map(datos.jugadores.map((j) => [j.id, j.nombre]))
  const normal = (t) => (t ?? '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
  const visibles = datos.pistas.filter((p) => !buscar.trim() || normal(p.titulo + ' ' + p.contenido).includes(normal(buscar)))
  const conQr = datos.pistas.filter((p) => p.codigoQr).length

  async function crear(evento) {
    evento.preventDefault()
    if (!titulo.trim() || !contenido.trim()) return
    if (await hacer('POST', '/pistas', { titulo, contenido })) {
      setTitulo('')
      setContenido('')
    }
  }

  function alternar(pista, jugadorId) {
    const ids = pista.jugadorIds.includes(jugadorId)
      ? pista.jugadorIds.filter((id) => id !== jugadorId)
      : [...pista.jugadorIds, jugadorId]
    hacer('PUT', `/pistas/${pista.id}/visibilidad`, { jugadorIds: ids })
  }

  const todos = datos.jugadores.map((j) => j.id)

  return (
    <section>
      <h2>Pistas ({datos.pistas.length})</h2>
      <div className="fila">
        <input value={buscar} onChange={(e) => setBuscar(e.target.value)} placeholder="Buscar pista" aria-label="Buscar pista" type="search" />
        <button onClick={() => setImprimiendo(true)}>Imprimir QR ({conQr})</button>
      </div>
      {imprimiendo && <HojaQr pistas={datos.pistas} alCerrar={() => setImprimiendo(false)} />}
      {visibles.map((p) => (
        <article key={p.id} className={`tarjeta ${p.falsa ? 'asesino' : ''}`}>
          <h3>
            {p.titulo}
            {p.falsa && (
              <span className="etiqueta fallido">
                Falsa · de {nombre.get(p.autorFalsaId) ?? '?'}
                {p.estadoFalsa !== 'APROBADA' ? ` (${p.estadoFalsa === 'PENDIENTE' ? 'pendiente' : 'rechazada'})` : ''}
              </span>
            )}
          </h3>
          <p>{p.contenido}</p>
          {p.nota && <p className="tenue">Nota: {p.nota}</p>}
          <p className="fila qr-fila">
            {p.codigoQr ? (
              <>
                <span>
                  QR <code>{p.codigoQr}</code>
                </span>
                <Confirmar texto="Quitar QR" pregunta="Los QR ya impresos dejarán de valer." alConfirmar={() => hacer('PUT', `/pistas/${p.id}/qr`, { activo: false })} confirmacion="Sí, quitar" />
              </>
            ) : (
              <button className="enlace" onClick={() => hacer('PUT', `/pistas/${p.id}/qr`, { activo: true })}>
                Crear QR para esconderla
              </button>
            )}
          </p>
          <p className="tenue">
            {p.jugadorIds.length === 0 ? 'Bloqueada: nadie la ve' : `La ven ${p.jugadorIds.length} de ${todos.length}`}
            {p.precio != null ? ` · en la tienda por ${p.precio} monedas` : ''}
          </p>
          <div className="fila">
            <CampoNumero
              key={`${p.id}-${p.precio}`}
              inicial={p.precio}
              etiqueta="Precio en la tienda (vacío = no se vende)"
              vacioPermitido
              alGuardar={(n) => hacer('PUT', `/pistas/${p.id}/precio`, { precio: n })}
            />
          </div>
          <div className="casillas">
            {datos.jugadores.map((j) => (
              <label key={j.id}>
                <input type="checkbox" checked={p.jugadorIds.includes(j.id)} onChange={() => alternar(p, j.id)} />
                {j.nombre}
              </label>
            ))}
          </div>
          <div className="fila">
            <button className="enlace" onClick={() => hacer('PUT', `/pistas/${p.id}/visibilidad`, { jugadorIds: todos })}>
              Desbloquear a todos
            </button>
            <button className="enlace" onClick={() => hacer('PUT', `/pistas/${p.id}/visibilidad`, { jugadorIds: [] })}>
              Bloquear
            </button>
            <Confirmar texto="Borrar" pregunta="¿Borrar la pista?" alConfirmar={() => hacer('DELETE', `/pistas/${p.id}`)} />
          </div>
        </article>
      ))}

      <h2>Nueva pista</h2>
      <form onSubmit={crear}>
        <input value={titulo} onChange={(e) => setTitulo(e.target.value)} placeholder="Título" aria-label="Título" />
        <textarea rows={3} value={contenido} onChange={(e) => setContenido(e.target.value)} placeholder="Contenido" aria-label="Contenido" />
        <button className="primario">Crear pista (bloqueada)</button>
      </form>
    </section>
  )
}

// La mensajería dentro de la app se retiró de la interfaz: el grupo habla por WhatsApp.
// La API de mensajes sigue en el backend por si hiciera falta recuperarla.
