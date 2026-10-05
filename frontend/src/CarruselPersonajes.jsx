import { useRef, useState } from 'react'
import { colorDe, iniciales } from './Avatar.jsx'

const PASO = 0.62 // separación entre cartas, en anchos de carta

/**
 * Carrusel en perspectiva: la carta central es el personaje elegido y las demás
 * asoman a los lados. Se desliza con el dedo, se toca una carta lateral para
 * traerla al centro y la central se toca para abrir su ficha (alAbrir).
 * Con teclado: flechas para moverse, Enter para abrir.
 */
export default function CarruselPersonajes({ personajes, indice, alCambiar, alAbrir }) {
  const contenedor = useRef(null)
  const gesto = useRef({ activo: false, movido: false, x0: 0, dx: 0, t0: 0, paso: 120 })
  const [arrastre, setArrastre] = useState(0)
  const [arrastrando, setArrastrando] = useState(false)
  const [fallos, setFallos] = useState({})

  function ir(nuevo) {
    alCambiar(Math.max(0, Math.min(personajes.length - 1, nuevo)))
  }

  function alBajar(evento) {
    if (evento.pointerType === 'mouse' && evento.button !== 0) return
    const carta = contenedor.current?.querySelector('.carta')
    const g = gesto.current
    g.activo = true
    g.movido = false
    g.x0 = evento.clientX
    g.dx = 0
    g.t0 = performance.now()
    g.paso = (carta?.offsetWidth || 200) * PASO
  }

  function alMover(evento) {
    const g = gesto.current
    if (!g.activo) return
    let dx = evento.clientX - g.x0
    if (Math.abs(dx) > 6) g.movido = true
    // En los extremos la fila ofrece resistencia en vez de seguir al dedo.
    if ((indice === 0 && dx > 0) || (indice === personajes.length - 1 && dx < 0)) dx *= 0.35
    g.dx = dx
    if (g.movido) {
      setArrastrando(true)
      setArrastre(dx)
    }
  }

  function alSoltar() {
    const g = gesto.current
    if (!g.activo) return
    g.activo = false
    const velocidad = g.dx / Math.max(1, performance.now() - g.t0) // px por ms
    if (Math.abs(g.dx) > g.paso * 0.3 || Math.abs(velocidad) > 0.5) ir(indice + (g.dx < 0 ? 1 : -1))
    setArrastrando(false)
    setArrastre(0)
  }

  function alCancelar() {
    gesto.current.activo = false
    setArrastrando(false)
    setArrastre(0)
  }

  // Tras deslizar, el "click" que suelta el navegador no debe elegir ni abrir una carta.
  function alClick(evento) {
    if (gesto.current.movido) {
      evento.stopPropagation()
      evento.preventDefault()
      gesto.current.movido = false
    }
  }

  function alTecla(evento) {
    if (evento.key === 'ArrowRight') {
      evento.preventDefault()
      ir(indice + 1)
    } else if (evento.key === 'ArrowLeft') {
      evento.preventDefault()
      ir(indice - 1)
    } else if (evento.key === 'Enter' && alAbrir && personajes[indice]) {
      evento.preventDefault()
      alAbrir(personajes[indice])
    }
  }

  const desplazamiento = arrastrando ? arrastre / gesto.current.paso : 0

  return (
    <>
      <div
        className={`carrusel ${arrastrando ? 'arrastrando' : ''}`}
        ref={contenedor}
        role="group"
        aria-roledescription="carrusel"
        aria-label="Personajes"
        tabIndex={0}
        onPointerDown={alBajar}
        onPointerMove={alMover}
        onPointerUp={alSoltar}
        onPointerLeave={alSoltar}
        onPointerCancel={alCancelar}
        onClickCapture={alClick}
        onKeyDown={alTecla}
      >
        {personajes.map((p, i) => {
          const d = Math.max(-2.5, Math.min(2.5, i - indice + desplazamiento))
          const lejania = Math.abs(d)
          const estilo = {
            transform: `translateX(calc(-50% + ${d * PASO * 100}%)) rotateY(${-d * 14}deg) scale(${Math.max(0.55, 1 - lejania * 0.13)})`,
            opacity: Math.max(0, 1 - Math.max(0, lejania - 0.2) * 0.42),
            filter: `brightness(${1 - Math.min(1, lejania) * 0.35})`,
            zIndex: 10 - Math.round(lejania),
            visibility: lejania > 2.4 ? 'hidden' : 'visible',
          }
          const activa = i === indice
          const basicos = [p.profesion, p.pareja && `Pareja: ${p.pareja}`].filter(Boolean)
          return (
            <button
              key={p.id}
              type="button"
              className={`carta ${activa ? 'activa' : ''}`}
              style={estilo}
              onClick={() => (activa && alAbrir ? alAbrir(p) : ir(i))}
              aria-label={activa ? `Abrir la ficha de ${p.nombre}` : `Ver a ${p.nombre}`}
              aria-current={activa ? 'true' : undefined}
              tabIndex={-1}
            >
              {p.imagenUrl && !fallos[p.id] ? (
                <img
                  src={p.imagenUrl}
                  alt=""
                  draggable={false}
                  referrerPolicy="no-referrer"
                  onError={() => setFallos((f) => ({ ...f, [p.id]: true }))}
                />
              ) : (
                <span className="carta-iniciales" style={{ backgroundColor: colorDe(p.nombre) }}>
                  {iniciales(p.nombre)}
                </span>
              )}
              <span className="carta-texto">
                <strong>{p.nombre}</strong>
                {basicos.length > 0 && <span>{basicos.join(' · ')}</span>}
                {activa && alAbrir && <span className="carta-abrir">Ver ficha ›</span>}
              </span>
            </button>
          )
        })}

        <button type="button" className="carrusel-flecha izq" onClick={() => ir(indice - 1)} disabled={indice === 0} aria-label="Personaje anterior">
          ‹
        </button>
        <button
          type="button"
          className="carrusel-flecha der"
          onClick={() => ir(indice + 1)}
          disabled={indice === personajes.length - 1}
          aria-label="Personaje siguiente"
        >
          ›
        </button>
      </div>

      <div className="carrusel-puntos">
        {personajes.map((p, i) => (
          <button key={p.id} type="button" className={i === indice ? 'activa' : ''} onClick={() => ir(i)} aria-label={`Ir a ${p.nombre}`} />
        ))}
      </div>
    </>
  )
}
