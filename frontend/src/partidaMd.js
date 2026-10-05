// Lee el fichero personajes.md de una partida y lo convierte en datos.
// Sin dependencias ni JSX: lo usan el panel del Máster (en el navegador) y
// scripts/cargar-fichas.mjs (en Node).
//
// Estructura (los títulos # son fijos; el orden da igual):
//
//   # Partida                       - Precio pista falsa: 200
//                                   - Lugares:  (y debajo, una estancia por línea "  - Despacho")
//   # Personajes
//   ## Nombre                       - Edad / Profesión / Pareja / Foto / Herramienta: valor
//   ### Personalidad | Relaciones | Contexto | Lo que sabe el Máster | Motivo potencial   (texto libre)
//   ### Secretos                    - un secreto por línea
//   ### Objetivos                   1. texto   (debajo: "   - principal: sí", tipo, cantidad, pista, dinero)
//   ### Habilidad: Nombre           - Tipo / Pide persona / Pregunta: valor, y la descripción como texto
//   ### Visiones                    1. Momento (debajo: hora, imagen, nota)
//   ### Sobres                      1. Título  (debajo: hora, anunciar, texto)
//   # Pistas
//   ## Título                       - Precio / QR / Dónde: valor, y el contenido como texto
//   # Visiones de reserva           1. Título  (debajo: imagen, nota)
//
// Las líneas que empiezan por ">" y los comentarios <!-- --> se ignoran.

/** "Lo que sabe el Máster" → "lo que sabe el master": así da igual tildes y mayúsculas. */
export function normalizar(texto) {
  return (texto ?? '')
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/\s+/g, ' ')
}

const SECCIONES_TEXTO = {
  personalidad: 'personalidad',
  relaciones: 'relaciones',
  contexto: 'contexto',
  'lo que sabe el master': 'loQueSabeMaster',
  'motivo potencial': 'motivoPotencial',
}

const CAMPOS_FICHA = {
  edad: 'edad',
  profesion: 'profesion',
  pareja: 'pareja',
  foto: 'imagenUrl',
  imagen: 'imagenUrl',
  herramienta: 'herramienta',
}

const TIPOS_HABILIDAD = ['MANUAL', 'VER_PISTAS', 'VER_MOVIMIENTOS', 'RASTREAR_PAGOS', 'PISTA_AL_AZAR', 'RUMOR_ANONIMO']
const TIPOS_OBJETIVO = ['LOGRO', 'TEXTO', 'PERSONA']

/**
 * Devuelve { partida, personajes, pistas, visionesReserva, avisos }.
 * "avisos" recoge lo que no se ha entendido, con su número de línea, para enseñárselo al Máster.
 */
export function leerPartidaMd(texto) {
  const avisos = []
  const arbol = arbolDe(texto)
  const resultado = { partida: { lugares: null, precioPistaFalsa: null }, personajes: [], pistas: [], visionesReserva: [], avisos }

  for (const h1 of arbol) {
    const seccion = normalizar(h1.titulo)
    if (seccion === 'partida') {
      const campos = camposDe(h1.cuerpo)
      if (campos.lugares) resultado.partida.lugares = campos.lugares.lista
      if (campos['precio pista falsa']) resultado.partida.precioPistaFalsa = numero(campos['precio pista falsa'], avisos)
    } else if (seccion === 'personajes') {
      for (const h2 of h1.hijos) resultado.personajes.push(personajeDe(h2, avisos))
    } else if (seccion === 'pistas') {
      for (const h2 of h1.hijos) resultado.pistas.push(pistaDe(h2, avisos))
    } else if (seccion === 'visiones de reserva') {
      resultado.visionesReserva = numeradosDe(h1.cuerpo).map((v) => ({
        titulo: v.titulo,
        imagenUrl: v.campos.imagen?.valor || null,
        nota: v.campos.nota?.valor || null,
      }))
    } else if (h1.titulo && h1 !== arbol[0] && !seccion.startsWith('como ')) {
      // El primer # es el título del documento y "# Cómo se rellena…" son instrucciones: no avisan.
      avisos.push(`Línea ${h1.linea}: sección «${h1.titulo}» desconocida (se ignora)`)
    }
  }
  return resultado
}

function personajeDe(h2, avisos) {
  const campos = camposDe(h2.cuerpo)
  const p = { nombre: h2.titulo.trim(), secretos: [], objetivos: [], habilidades: [], visiones: [], sobres: [] }
  for (const [clave, campo] of Object.entries(campos)) {
    const destino = CAMPOS_FICHA[clave]
    if (!destino) {
      avisos.push(`Línea ${campo.linea}: «${clave}» no es un dato de ficha (${p.nombre})`)
      continue
    }
    p[destino] = destino === 'edad' ? (campo.valor ? numero(campo, avisos) : null) : campo.valor || null
  }
  for (const h3 of h2.hijos) {
    const titulo = normalizar(h3.titulo)
    if (SECCIONES_TEXTO[titulo]) {
      p[SECCIONES_TEXTO[titulo]] = textoDe(h3.cuerpo) || null
    } else if (titulo === 'secretos') {
      p.secretos = elementosDe(h3.cuerpo)
    } else if (titulo === 'objetivos') {
      p.objetivos = numeradosDe(h3.cuerpo).map((o) => {
        const tipo = (o.campos.tipo?.valor || 'LOGRO').toUpperCase()
        if (!TIPOS_OBJETIVO.includes(tipo)) avisos.push(`Línea ${o.linea}: tipo de objetivo «${tipo}» desconocido`)
        return {
          texto: o.titulo,
          principal: si(o.campos.principal?.valor),
          tipo,
          cantidad: o.campos.cantidad ? numero(o.campos.cantidad, avisos) : 1,
          pista: o.campos.pista?.valor || null,
          dinero: o.campos.dinero ? numero(o.campos.dinero, avisos) : 0,
        }
      })
    } else if (titulo.startsWith('habilidad')) {
      const c = camposDe(h3.cuerpo)
      const nombre = h3.titulo.includes(':') ? h3.titulo.slice(h3.titulo.indexOf(':') + 1).trim() : c.nombre?.valor
      const tipo = (c.tipo?.valor || 'MANUAL').toUpperCase()
      if (!TIPOS_HABILIDAD.includes(tipo)) avisos.push(`Línea ${h3.linea}: tipo de habilidad «${tipo}» desconocido`)
      p.habilidades.push({
        nombre,
        tipo,
        pidePersona: si(c['pide persona']?.valor),
        pideTexto: c.pregunta?.valor || null,
        descripcion: textoDe(h3.cuerpo),
      })
    } else if (titulo === 'visiones') {
      p.visiones = numeradosDe(h3.cuerpo).map((v) => ({
        momento: v.titulo,
        hora: fecha(v.campos.hora, avisos),
        imagenUrl: v.campos.imagen?.valor || null,
        nota: v.campos.nota?.valor || null,
      }))
    } else if (titulo === 'sobres') {
      p.sobres = numeradosDe(h3.cuerpo).map((s) => ({
        titulo: s.titulo,
        hora: fecha(s.campos.hora, avisos),
        anunciar: s.campos.anunciar ? si(s.campos.anunciar.valor) : true,
        contenido: s.campos.texto?.valor || '',
      }))
    } else {
      avisos.push(`Línea ${h3.linea}: apartado «${h3.titulo}» desconocido en ${p.nombre}`)
    }
  }
  return p
}

function pistaDe(h2, avisos) {
  const c = camposDe(h2.cuerpo)
  const precio = c.precio?.valor
  return {
    titulo: h2.titulo.trim(),
    contenido: textoDe(h2.cuerpo),
    precio: precio && !['no', '-', 'ninguno'].includes(normalizar(precio)) ? numero(c.precio, avisos) : null,
    qr: si(c.qr?.valor),
    nota: c.donde?.valor || c.nota?.valor || null,
  }
}

// ---------------------------------------------------------------- piezas

/** Agrupa las líneas por encabezados: # → ## → ###. */
function arbolDe(texto) {
  const raiz = []
  let h1 = null
  let h2 = null
  let h3 = null
  let enComentario = false
  const lineas = (texto ?? '').replace(/^﻿/, '').replace(/\r\n?/g, '\n').split('\n')
  lineas.forEach((original, i) => {
    let linea = original
    if (enComentario) {
      if (!linea.includes('-->')) return
      linea = linea.slice(linea.indexOf('-->') + 3)
      enComentario = false
    }
    linea = linea.replace(/<!--.*?-->/g, '')
    if (linea.includes('<!--')) {
      linea = linea.slice(0, linea.indexOf('<!--'))
      enComentario = true
    }
    if (/^\s*>/.test(linea)) return
    const encabezado = /^(#{1,3})\s+(.*?)\s*#*\s*$/.exec(linea)
    if (encabezado) {
      const nodo = { titulo: encabezado[2], linea: i + 1, cuerpo: [], hijos: [] }
      if (encabezado[1].length === 1) {
        h1 = nodo
        h2 = h3 = null
        raiz.push(nodo)
      } else if (encabezado[1].length === 2) {
        if (!h1) {
          h1 = { titulo: '', linea: 0, cuerpo: [], hijos: [] }
          raiz.push(h1)
        }
        h2 = nodo
        h3 = null
        h1.hijos.push(nodo)
      } else if (h2) {
        h3 = nodo
        h2.hijos.push(nodo)
      }
      return
    }
    const destino = h3 ?? h2 ?? h1
    if (destino) destino.cuerpo.push({ texto: linea, linea: i + 1 })
  })
  return raiz
}

const CAMPO = /^-\s+([^:]{1,40}):\s*(.*)$/
const SUBELEMENTO = /^\s{2,}[-*]\s+(.*)$/

/** Campos "- Clave: valor" sin sangría. Si el valor va vacío y debajo hay una lista sangrada, la recoge. */
function camposDe(cuerpo) {
  const campos = {}
  let ultimo = null
  for (const { texto, linea } of cuerpo) {
    const campo = CAMPO.exec(texto)
    if (campo) {
      ultimo = { valor: campo[2].trim(), linea, lista: [] }
      campos[normalizar(campo[1])] = ultimo
      continue
    }
    const sub = SUBELEMENTO.exec(texto)
    if (sub && ultimo) {
      ultimo.lista.push(sub[1].trim())
      continue
    }
    if (texto.trim() !== '') ultimo = null
  }
  return campos
}

/** Elementos de una lista simple "- texto" (los secretos): el texto entero, aunque lleve dos puntos. */
function elementosDe(cuerpo) {
  const elementos = []
  for (const { texto } of cuerpo) {
    const m = /^[-*]\s+(.*)$/.exec(texto)
    if (m) elementos.push(m[1].trim())
    else if (/^\s{2,}\S/.test(texto) && elementos.length > 0) elementos[elementos.length - 1] += ' ' + texto.trim()
  }
  return elementos
}

/** Lista numerada "1. Título" con campos sangrados debajo ("   - clave: valor"). */
function numeradosDe(cuerpo) {
  const items = []
  for (const { texto, linea } of cuerpo) {
    const item = /^\d+[.)]\s+(.*)$/.exec(texto)
    if (item) {
      items.push({ titulo: item[1].trim(), linea, campos: {} })
      continue
    }
    const actual = items[items.length - 1]
    const campo = /^\s+[-*]\s+([^:]{1,40}):\s*(.*)$/.exec(texto)
    if (actual && campo) {
      actual.campos[normalizar(campo[1])] = { valor: campo[2].trim(), linea }
    } else if (actual && /^\s{2,}\S/.test(texto)) {
      // Continuación del valor anterior en otra línea
      const claves = Object.keys(actual.campos)
      if (claves.length > 0) actual.campos[claves[claves.length - 1]].valor += ' ' + texto.trim()
    }
  }
  return items
}

/** El texto libre de un bloque (sin campos ni listas): párrafos separados por una línea en blanco. */
function textoDe(cuerpo) {
  const parrafos = []
  let actual = []
  let enLista = false
  for (const { texto } of cuerpo) {
    if (texto.trim() === '') {
      if (actual.length) parrafos.push(actual.join('\n'))
      actual = []
      enLista = false
      continue
    }
    if (/^[-*]\s/.test(texto) || /^\d+[.)]\s/.test(texto)) {
      enLista = true
      continue
    }
    if (enLista && /^\s/.test(texto)) continue
    enLista = false
    actual.push(texto.trim())
  }
  if (actual.length) parrafos.push(actual.join('\n'))
  return parrafos.join('\n\n')
}

function si(valor) {
  return ['si', 'yes', 'true', 'x', '1'].includes(normalizar(valor))
}

function numero(campo, avisos) {
  const n = Number(String(campo.valor).replace(/[^\d-]/g, ''))
  if (!Number.isFinite(n) || String(campo.valor).trim() === '') {
    avisos.push(`Línea ${campo.linea}: «${campo.valor}» no es un número`)
    return null
  }
  return n
}

/** "2026-10-24 14:30" en hora de la casa (la del dispositivo que importa) → ISO con zona. */
function fecha(campo, avisos) {
  if (!campo?.valor) return null
  const m = /^(\d{4})-(\d{2})-(\d{2})[ T](\d{1,2}):(\d{2})/.exec(campo.valor)
  if (!m) {
    avisos.push(`Línea ${campo.linea}: hora «${campo.valor}» no válida (formato 2026-10-24 14:30)`)
    return null
  }
  return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]), Number(m[4]), Number(m[5])).toISOString()
}
