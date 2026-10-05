import { useState } from 'react'

/**
 * Imagen del personaje. Si no tiene (o el enlace falla), muestra sus iniciales
 * sobre un color que depende del nombre, así cada persona se distingue igual.
 */
export default function Avatar({ nombre, imagenUrl, tamano = 'normal' }) {
  const [urlFallida, setUrlFallida] = useState(null)
  const url = imagenUrl && imagenUrl !== urlFallida ? imagenUrl : null

  return (
    <span className={`avatar ${tamano}`} style={url ? undefined : { background: colorDe(nombre) }} aria-hidden="true">
      {url ? (
        <img src={url} alt="" loading="lazy" referrerPolicy="no-referrer" onError={() => setUrlFallida(url)} />
      ) : (
        iniciales(nombre)
      )}
    </span>
  )
}

export function iniciales(nombre) {
  const partes = (nombre || '').trim().split(/\s+/).filter(Boolean)
  if (partes.length === 0) return '?'
  const letras = partes.length === 1 ? partes[0].slice(0, 2) : partes[0][0] + partes[partes.length - 1][0]
  return letras.toUpperCase()
}

export function colorDe(nombre) {
  let h = 0
  for (const c of nombre || '') h = (h * 31 + c.charCodeAt(0)) % 360
  return `hsl(${h} 32% 36%)`
}
