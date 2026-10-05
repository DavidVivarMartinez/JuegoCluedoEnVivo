// Vuelca en la partida lo leído de personajes.md (ver partidaMd.js). Lo usan el panel
// del Máster y scripts/cargar-fichas.mjs. Todo se empareja por nombre o título, así que
// se puede importar las veces que haga falta: crea lo que falta y actualiza lo que cambió.
// Lo que ya pasó (sobres abiertos, visiones enviadas, habilidades usadas) no se toca.
//
// "api(metodo, ruta, cuerpo)" llama a /api/master<ruta> con el código del Máster.
// Las imágenes (fotos y visiones) pueden ser enlaces https o ficheros locales
// ("visiones/fer-1.jpg"): para estos hacen falta en "opciones" leerImagen(ruta) → Blob|null
// y subirImagen(nombre, blob) → enlace. Una imagen ya subida con el mismo nombre y tamaño no se vuelve a subir.

import { normalizar } from './partidaMd.js'

const CAMPOS_FICHA = ['nombre', 'edad', 'profesion', 'pareja', 'imagenUrl', 'herramienta', 'personalidad', 'relaciones',
  'contexto', 'loQueSabeMaster', 'motivoPotencial']

export async function importarPartida(datos, api, log = () => {}, opciones = {}) {
  const { reemplazarObjetivos = false, reemplazarSecretos = false, leerImagen, subirImagen } = opciones
  const cuenta = { fichas: 0, secretos: 0, pistas: 0, objetivos: 0, habilidades: 0, envios: 0, imagenes: 0 }

  // ---------- Imágenes locales → enlaces de la partida ----------
  const subidas = new Map((await api('GET', '/imagenes').catch(() => [])).map((i) => [i.nombre, i]))
  const faltan = new Set()
  async function enlace(valor) {
    if (!valor) return null
    if (/^https?:\/\//i.test(valor) || valor.startsWith('/api/imagenes/')) return valor
    const nombre = valor.replace(/\\/g, '/').split('/').pop()
    const blob = leerImagen ? await leerImagen(valor) : null
    if (!blob) {
      // Sin el fichero a mano vale la ya subida con ese nombre
      if (subidas.has(nombre)) return subidas.get(nombre).url
      faltan.add(valor)
      return null
    }
    const previa = subidas.get(nombre)
    if (previa && previa.tamano === blob.size) return previa.url
    const url = await subirImagen(nombre, blob)
    subidas.set(nombre, { nombre, url, tamano: blob.size })
    cuenta.imagenes++
    return url
  }

  // ---------- Reglas ----------
  const reglas = {}
  if (datos.partida.lugares) reglas.lugares = datos.partida.lugares.join('\n')
  if (datos.partida.precioPistaFalsa != null) reglas.precioPistaFalsa = datos.partida.precioPistaFalsa
  if (Object.keys(reglas).length > 0) {
    await api('PUT', '/reglas', reglas)
    log(`Reglas: ${datos.partida.lugares?.length ?? 0} estancias, pista falsa a ${datos.partida.precioPistaFalsa ?? '—'}`)
  }

  // ---------- Fichas ----------
  let estado = await api('GET', '/estado')
  const previos = new Map(estado.jugadores.map((j) => [normalizar(j.nombre), j]))
  for (const p of datos.personajes) {
    const previo = previos.get(normalizar(p.nombre))
    const ficha = Object.fromEntries(CAMPOS_FICHA.map((c) => [c, p[c] ?? null]))
    ficha.imagenUrl = await enlace(ficha.imagenUrl)
    // Una foto puesta desde el panel no se borra porque el fichero la deje vacía.
    if (!ficha.imagenUrl && previo?.imagenUrl) ficha.imagenUrl = previo.imagenUrl
    if (previo) await api('PUT', `/jugadores/${previo.id}`, ficha)
    else await api('POST', '/jugadores', ficha)
    cuenta.fichas++
  }
  log(`Fichas: ${cuenta.fichas}`)

  estado = await api('GET', '/estado')
  const jugador = new Map(estado.jugadores.map((j) => [normalizar(j.nombre), j]))
  const de = (p) => jugador.get(normalizar(p.nombre))

  // ---------- Secretos (por texto) ----------
  for (const p of datos.personajes) {
    const j = de(p)
    if (!j) continue
    if (reemplazarSecretos) {
      for (const s of j.secretos) {
        if (!p.secretos.some((t) => t.trim() === s.texto.trim())) await api('DELETE', `/secretos/${s.id}`)
      }
    }
    for (const texto of p.secretos) {
      if (!j.secretos.some((s) => s.texto.trim() === texto.trim())) {
        await api('POST', `/jugadores/${j.id}/secretos`, { texto })
        cuenta.secretos++
      }
    }
  }
  log(`Secretos nuevos: ${cuenta.secretos}`)

  // ---------- Pistas (por título); los sobres de cada personaje también son pistas ----------
  const todas = [
    ...datos.pistas,
    ...datos.personajes.flatMap((p) => p.sobres.map((s) => ({ titulo: s.titulo, contenido: s.contenido, precio: null, qr: false, nota: `Sobre programado para ${p.nombre}` }))),
  ]
  const pistaPrevia = new Map(estado.pistas.map((p) => [normalizar(p.titulo), p]))
  for (const pista of todas) {
    if (!pista.contenido) {
      log(`Aviso: la pista «${pista.titulo}» no tiene contenido y se salta`)
      continue
    }
    let previa = pistaPrevia.get(normalizar(pista.titulo))
    if (!previa) {
      const { id } = await api('POST', '/pistas', { titulo: pista.titulo, contenido: pista.contenido, nota: pista.nota })
      previa = { id, precio: null, codigoQr: null }
      cuenta.pistas++
    } else if (previa.contenido !== pista.contenido || (previa.nota ?? null) !== (pista.nota ?? null)) {
      await api('PUT', `/pistas/${previa.id}`, { titulo: previa.titulo, contenido: pista.contenido, nota: pista.nota ?? '' })
    }
    if ((previa.precio ?? null) !== (pista.precio ?? null)) await api('PUT', `/pistas/${previa.id}/precio`, { precio: pista.precio })
    // El QR solo se activa: quitarlo invalidaría los códigos ya impresos, así que eso se hace a mano en el panel.
    if (pista.qr && !previa.codigoQr) await api('PUT', `/pistas/${previa.id}/qr`, { activo: true })
  }
  log(`Pistas nuevas: ${cuenta.pistas} (de ${todas.length})`)

  // ---------- Objetivos (por texto) y su pista de recompensa ----------
  for (const p of datos.personajes) {
    const j = de(p)
    if (!j || p.objetivos.length === 0) continue
    let existentes = j.objetivos
    if (reemplazarObjetivos) {
      for (const o of existentes) await api('DELETE', `/objetivos/${o.id}`)
      existentes = []
    }
    for (const o of p.objetivos) {
      const previo = existentes.find((x) => x.texto.trim() === o.texto.trim())
      const cuerpo = { texto: o.texto, principal: o.principal, tipo: o.tipo, cantidad: o.cantidad ?? 1, recompensaDinero: o.dinero ?? 0 }
      if (!previo) {
        await api('POST', `/jugadores/${j.id}/objetivos`, cuerpo)
        cuenta.objetivos++
      } else if (previo.principal !== o.principal || previo.tipo !== o.tipo || previo.cantidad !== cuerpo.cantidad
          || previo.recompensaDinero !== cuerpo.recompensaDinero) {
        await api('PUT', `/objetivos/${previo.id}`, { principal: o.principal, tipo: o.tipo, cantidad: cuerpo.cantidad, recompensaDinero: cuerpo.recompensaDinero })
      }
    }
  }
  estado = await api('GET', '/estado')
  {
    const pistaId = new Map(estado.pistas.map((p) => [normalizar(p.titulo), p.id]))
    const actual = new Map(estado.jugadores.map((j) => [normalizar(j.nombre), j]))
    for (const p of datos.personajes) {
      const j = actual.get(normalizar(p.nombre))
      if (!j) continue
      for (const o of p.objetivos) {
        const previo = j.objetivos.find((x) => x.texto.trim() === o.texto.trim())
        if (!previo) continue
        const id = o.pista ? pistaId.get(normalizar(o.pista)) ?? null : null
        if (o.pista && id == null) {
          log(`Aviso: la pista «${o.pista}» (objetivo de ${p.nombre}) no existe`)
          continue
        }
        if ((previo.recompensaPistaId ?? null) !== id) await api('PUT', `/objetivos/${previo.id}/recompensa`, { pistaId: id })
      }
    }
  }
  log(`Objetivos nuevos: ${cuenta.objetivos}`)

  // ---------- Habilidades (por nombre) ----------
  const jugadoresAhora = new Map(estado.jugadores.map((j) => [normalizar(j.nombre), j]))
  for (const p of datos.personajes) {
    const j = jugadoresAhora.get(normalizar(p.nombre))
    if (!j) continue
    for (const h of p.habilidades) {
      if (!h.nombre || !h.descripcion) {
        log(`Aviso: habilidad sin nombre o sin descripción en ${p.nombre}`)
        continue
      }
      const cuerpo = { nombre: h.nombre, descripcion: h.descripcion, tipo: h.tipo, pidePersona: h.pidePersona, pideTexto: h.pideTexto }
      const previa = (j.habilidades ?? []).find((x) => normalizar(x.nombre) === normalizar(h.nombre))
      if (!previa) {
        await api('POST', `/jugadores/${j.id}/habilidades`, cuerpo)
        cuenta.habilidades++
      } else if (previa.descripcion !== h.descripcion || previa.tipo !== h.tipo || previa.pidePersona !== h.pidePersona
          || (previa.pideTexto ?? null) !== (h.pideTexto ?? null)) {
        await api('PUT', `/habilidades/${previa.id}`, cuerpo)
      }
    }
  }
  log(`Habilidades nuevas: ${cuenta.habilidades}`)

  // ---------- Envíos: sobres, visiones y visiones de reserva ----------
  // La primera línea de la nota de una visión es su nombre (el momento o el título): así se empareja.
  const pistaPorTitulo = new Map(estado.pistas.map((p) => [normalizar(p.titulo), p.id]))
  const envios = estado.envios ?? []
  const primera = (nota) => normalizar((nota ?? '').split('\n')[0])

  async function guardarEnvio(previo, cuerpo) {
    if (previo?.enviadoEn) return // ya entregado: no se reescribe la historia
    if (!cuerpo.imagenUrl && previo?.imagenUrl) cuerpo.imagenUrl = previo.imagenUrl
    if (previo) await api('PUT', `/envios/${previo.id}`, cuerpo)
    else {
      await api('POST', '/envios', cuerpo)
      cuenta.envios++
    }
  }

  for (const p of datos.personajes) {
    const j = jugadoresAhora.get(normalizar(p.nombre))
    if (!j) continue
    for (const s of p.sobres) {
      const id = pistaPorTitulo.get(normalizar(s.titulo))
      if (!id) continue
      const previo = envios.find((e) => e.tipo === 'PISTA' && e.pistaId === id && e.programadaPara)
      await guardarEnvio(previo, { tipo: 'PISTA', pistaId: id, jugadorIds: [j.id], programadaPara: s.hora, anunciar: s.anunciar })
    }
    for (const v of p.visiones) {
      const previo = envios.find((e) => e.tipo === 'VISION' && e.programadaPara && e.jugadorIds.length === 1
          && e.jugadorIds[0] === j.id && primera(e.nota) === normalizar(v.momento))
      await guardarEnvio(previo, {
        tipo: 'VISION', imagenUrl: await enlace(v.imagenUrl), nota: `${v.momento}\n${v.nota ?? ''}`.trim(),
        jugadorIds: [j.id], programadaPara: v.hora, anunciar: false,
      })
    }
  }
  for (const v of datos.visionesReserva) {
    const previo = envios.find((e) => e.tipo === 'VISION' && !e.programadaPara && e.jugadorIds.length === 0 && !e.enviadoEn
        && primera(e.nota) === normalizar(v.titulo))
    await guardarEnvio(previo, { tipo: 'VISION', imagenUrl: await enlace(v.imagenUrl), nota: `${v.titulo}\n${v.nota ?? ''}`.trim(), jugadorIds: [], programadaPara: null })
  }
  log(`Envíos nuevos: ${cuenta.envios}`)
  log(`Imágenes subidas: ${cuenta.imagenes}`)
  if (faltan.size > 0) log(`Aviso: ${faltan.size} imágenes del fichero no se han encontrado (${[...faltan].slice(0, 3).join(', ')}${faltan.size > 3 ? '…' : ''})`)

  return cuenta
}
