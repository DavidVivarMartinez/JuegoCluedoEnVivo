import { useEffect, useRef, useState } from 'react'

const ESPERA_GUARDADO = 800

/**
 * Cuaderno privado sobre un personaje. Guarda solo al dejar de escribir, al salir
 * del campo, al desmontarse (cambio de pestaña o de ficha) y al bloquear el móvil.
 * "alGuardar(jugadorId, texto)" debe devolver la respuesta del servidor (con actualizadoEn).
 */
export default function Nota({ personaje, alGuardar }) {
  const [texto, setTexto] = useState(personaje.anotacion ?? '')
  const [estado, setEstado] = useState('guardado') // guardado | pendiente | guardando | error
  const ref = useRef({
    texto: personaje.anotacion ?? '',
    guardadoEn: personaje.anotacionEn ?? null,
    sucio: false,
    temporizador: null,
  })

  // Si el servidor trae una versión más nueva (p. ej. escrita desde otro móvil) y
  // aquí no hay nada sin guardar, la adoptamos. Las respuestas atrasadas se ignoran.
  useEffect(() => {
    const r = ref.current
    const nuevaEn = personaje.anotacionEn
    if (r.sucio || !nuevaEn) return
    if (!r.guardadoEn || new Date(nuevaEn) > new Date(r.guardadoEn)) {
      r.guardadoEn = nuevaEn
      r.texto = personaje.anotacion ?? ''
      setTexto(r.texto)
    }
  }, [personaje.anotacion, personaje.anotacionEn])

  async function guardar() {
    const r = ref.current
    clearTimeout(r.temporizador)
    r.temporizador = null
    if (!r.sucio) return
    r.sucio = false
    setEstado('guardando')
    try {
      const respuesta = await alGuardar(personaje.id, r.texto)
      if (respuesta?.actualizadoEn) r.guardadoEn = respuesta.actualizadoEn
      // Si se siguió escribiendo durante el guardado, el temporizador volverá a guardar.
      setEstado(r.sucio ? 'pendiente' : 'guardado')
    } catch {
      r.sucio = true
      setEstado('error')
    }
  }

  function cambiar(evento) {
    const r = ref.current
    r.texto = evento.target.value
    r.sucio = true
    setTexto(r.texto)
    setEstado('pendiente')
    clearTimeout(r.temporizador)
    r.temporizador = setTimeout(guardar, ESPERA_GUARDADO)
  }

  // Al bloquear el móvil o cambiar de app no esperamos al temporizador.
  useEffect(() => {
    const alOcultar = () => {
      if (document.hidden && ref.current.sucio) guardar()
    }
    document.addEventListener('visibilitychange', alOcultar)
    return () => document.removeEventListener('visibilitychange', alOcultar)
  }, [])

  // Al desmontarse guardamos lo pendiente.
  useEffect(() => {
    return () => {
      if (ref.current.sucio) guardar()
    }
  }, [])

  return (
    <div className="anotacion">
      <textarea
        value={texto}
        onChange={cambiar}
        onBlur={guardar}
        rows={3}
        maxLength={4000}
        placeholder={`Lo que sé de ${personaje.nombre}…`}
        aria-label={`Anotaciones sobre ${personaje.nombre}`}
        autoCapitalize="sentences"
      />
      <div className={`estado ${estado === 'error' ? 'error' : ''}`} aria-live="polite">
        {estado === 'pendiente' && 'Sin guardar'}
        {estado === 'guardando' && 'Guardando…'}
        {estado === 'guardado' && texto !== '' && 'Guardado'}
        {estado === 'error' && (
          <>
            No se pudo guardar
            <button className="enlace" onClick={guardar}>
              Reintentar
            </button>
          </>
        )}
      </div>
    </div>
  )
}
