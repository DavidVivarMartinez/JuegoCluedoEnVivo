import { useEffect, useRef } from 'react'

// Iconos de trazo, sin dependencias, para que cada sección se reconozca de un vistazo.
const ICONOS = {
  personaje: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
    </>
  ),
  objetivos: (
    <>
      <circle cx="12" cy="12" r="9" />
      <circle cx="12" cy="12" r="5" />
      <circle cx="12" cy="12" r="1.2" fill="currentColor" />
    </>
  ),
  pistas: (
    <>
      <circle cx="11" cy="11" r="6.5" />
      <path d="m20 20-4.3-4.3" />
    </>
  ),
  dinero: (
    <>
      <ellipse cx="12" cy="6.5" rx="7" ry="3" />
      <path d="M5 6.5v5c0 1.7 3.1 3 7 3s7-1.3 7-3v-5" />
      <path d="M5 11.5v5c0 1.7 3.1 3 7 3s7-1.3 7-3v-5" />
    </>
  ),
  cuaderno: (
    <>
      <rect x="5" y="3" width="14" height="18" rx="2" />
      <path d="m8.5 8 1.2 1.2L12 7M8.5 13l1.2 1.2L12 12M14 8.2h2M14 13.2h2M8.5 17.5h7" />
    </>
  ),
  tablon: (
    <>
      <path d="M4 10v4a1 1 0 0 0 1 1h2l5 4V5L7 9H5a1 1 0 0 0-1 1Z" />
      <path d="M16 8.5a5 5 0 0 1 0 7M18.8 6a8.5 8.5 0 0 1 0 12" />
    </>
  ),
  envios: (
    <>
      <rect x="3" y="5.5" width="18" height="13" rx="2" />
      <path d="m3.5 7 8.5 6.5L20.5 7" />
    </>
  ),
  ranking: (
    <>
      <path d="M8 4h8v5a4 4 0 0 1-8 0V4Z" />
      <path d="M8 6H5a3 3 0 0 0 3 4M16 6h3a3 3 0 0 1-3 4M12 13v4M8.5 20h7M10 17h4" />
    </>
  ),
  partida: (
    <>
      <path d="m3 11 9-7 9 7" />
      <path d="M5 10v10h14V10" />
    </>
  ),
  jugadores: (
    <>
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6" />
      <path d="M16 4.5a3.5 3.5 0 0 1 0 7" />
      <path d="M18.5 14.5c2 .8 3 2.5 3 5.5" />
    </>
  ),
}

/**
 * Barra de secciones, fija en la parte superior. Cada pestaña lleva icono y texto.
 * "globos" permite mostrar un contador junto a una pestaña (p. ej. mensajes sin leer).
 */
export default function Pestanas({ pestanas, activa, alCambiar, globos = {} }) {
  const barra = useRef(null)

  // Si la barra no cabe entera (letra muy grande), la pestaña activa se trae a la vista.
  useEffect(() => {
    const boton = barra.current?.querySelector('[aria-selected="true"]')
    boton?.scrollIntoView?.({ block: 'nearest', inline: 'center' })
  }, [activa])

  return (
    <nav className="pestanas" aria-label="Secciones" role="tablist" ref={barra}>
      {pestanas.map(([clave, titulo]) => {
        const globo = globos[clave]
        return (
          <button
            key={clave}
            type="button"
            role="tab"
            aria-selected={activa === clave}
            className={activa === clave ? 'activa' : ''}
            onClick={() => alCambiar(clave)}
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              {ICONOS[clave]}
            </svg>
            <span className="pestana-titulo">{titulo}</span>
            {globo > 0 && <span className="globo">{globo > 99 ? '99+' : globo}</span>}
          </button>
        )
      })}
    </nav>
  )
}
