#!/usr/bin/env node
// Carga o actualiza una partida entera desde su personajes.md (fichas, secretos, objetivos,
// habilidades, pistas, QR, sobres programados y visiones). Es lo mismo que el botón
// "Importar personajes.md" del panel del Máster, pero desde la terminal.
//
//   node scripts/cargar-fichas.mjs <codigoMaster> <personajes.md> [urlBase] [--reemplazar-objetivos] [--reemplazar-secretos]
//
// El formato del .md está explicado al principio de frontend/src/partidaMd.js y al final
// del propio fichero de la partida. Todo se empareja por nombre o título: se puede
// relanzar cuantas veces haga falta. Con --reemplazar-objetivos se borran antes los
// objetivos de cada personaje del fichero; con --reemplazar-secretos, los secretos que
// ya no estén en él. También acepta el JSON antiguo (personajes.json).
// Guarda el fichero en contenido-privado/ (ignorado por git): son datos reales.

import { readFile } from 'node:fs/promises'
import { dirname, extname, resolve } from 'node:path'
import { leerPartidaMd } from '../frontend/src/partidaMd.js'
import { importarPartida } from '../frontend/src/importarPartida.js'

const argumentos = process.argv.slice(2)
const opciones = {
  reemplazarObjetivos: argumentos.includes('--reemplazar-objetivos'),
  reemplazarSecretos: argumentos.includes('--reemplazar-secretos'),
}
const [codigo, fichero, base = 'http://localhost:8080'] = argumentos.filter((a) => !a.startsWith('--'))
if (!codigo || !fichero) {
  console.error('Uso: node scripts/cargar-fichas.mjs <codigoMaster> <personajes.md> [urlBase] [--reemplazar-objetivos] [--reemplazar-secretos]')
  process.exit(1)
}

async function api(metodo, ruta, cuerpo) {
  const respuesta = await fetch(`${base}/api/master${ruta}`, {
    method: metodo,
    headers: { Authorization: `Bearer ${codigo}`, 'Content-Type': 'application/json' },
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  })
  const texto = await respuesta.text()
  if (!respuesta.ok) throw new Error(`${metodo} ${ruta} → ${respuesta.status} ${texto}`)
  return texto ? JSON.parse(texto) : null
}

// Imágenes locales del .md (rutas relativas a él): se suben a la partida al importar.
const TIPOS = { '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.png': 'image/png', '.webp': 'image/webp', '.gif': 'image/gif' }
async function leerImagen(ruta) {
  try {
    const datos = await readFile(resolve(dirname(fichero), ruta))
    return new Blob([datos], { type: TIPOS[extname(ruta).toLowerCase()] ?? 'application/octet-stream' })
  } catch {
    return null
  }
}
async function subirImagen(nombre, blob) {
  const formulario = new FormData()
  formulario.append('archivo', blob, nombre)
  const respuesta = await fetch(`${base}/api/master/imagenes`, { method: 'POST', headers: { Authorization: `Bearer ${codigo}` }, body: formulario })
  const texto = await respuesta.text()
  if (!respuesta.ok) throw new Error(`subir ${nombre} → ${respuesta.status} ${texto}`)
  return JSON.parse(texto).url
}

const texto = await readFile(fichero, 'utf8')
let datos
if (fichero.toLowerCase().endsWith('.json')) {
  // Formato antiguo: mismas claves, sin habilidades, sobres ni visiones.
  const json = JSON.parse(texto)
  datos = {
    partida: { lugares: null, precioPistaFalsa: null },
    personajes: (json.personajes ?? []).map((p) => ({
      habilidades: [], visiones: [], sobres: [], secretos: [], objetivos: [],
      ...p,
      objetivos: (p.objetivos ?? []).map((o) => (typeof o === 'string' ? { texto: o, principal: false, tipo: 'LOGRO', cantidad: 1, dinero: 0 } : o)),
    })),
    pistas: (json.pistas ?? []).map((p) => ({ qr: false, nota: null, precio: null, ...p })),
    visionesReserva: [],
    avisos: [],
  }
} else {
  datos = leerPartidaMd(texto)
}

for (const aviso of datos.avisos) console.warn(`aviso: ${aviso}`)
if (datos.personajes.length === 0) {
  console.error('El fichero no tiene personajes.')
  process.exit(1)
}

await importarPartida(datos, api, (linea) => console.log(linea), { ...opciones, leerImagen, subirImagen })
console.log('\nListo. Los códigos de acceso están en el panel del Máster.')
