import Avatar from './Avatar.jsx'

/**
 * Marcador en directo para el Máster. Las reglas de puntos viven en el backend
 * (JuegoService.ranking); aquí solo se enseñan con su desglose.
 */
export default function RankingMaster({ datos }) {
  const filas = datos.ranking ?? []
  const porId = new Map(datos.jugadores.map((j) => [j.id, j]))
  const entregadas = filas.filter((f) => f.acusacionFinal).length

  return (
    <section>
      <h2>Ranking</h2>
      <p className="tenue">
        {entregadas} de {filas.length} han entregado su acusación final. Se actualiza solo.
      </p>
      <ol className="ranking">
        {filas.map((f, i) => {
          const j = porId.get(f.jugadorId)
          return (
            <li key={f.jugadorId} className={`tarjeta fila-ranking ${f.asesino ? 'es-asesino' : ''}`}>
              <details>
                <summary>
                  <span className="posicion">{i + 1}</span>
                  <Avatar nombre={f.nombre} imagenUrl={j?.imagenUrl} tamano="mini" />
                  <span className="ranking-texto">
                    <span>
                      {f.nombre}
                      {f.asesino && <span className="etiqueta fallido">Asesino</span>}
                    </span>
                    <span className="tenue">
                      {f.objetivosCumplidos}/{f.objetivosTotal} objetivos · {f.pistas} pistas
                      {f.pistasCompradas ? ` (${f.pistasCompradas} compradas)` : ''} · {f.dinero} monedas
                    </span>
                    <Acusacion fila={f} />
                  </span>
                  <span className="puntos">{f.puntos}</span>
                </summary>
                {f.desglose.length === 0 ? (
                  <p className="tenue">Todavía sin puntos.</p>
                ) : (
                  <ul className="desglose">
                    {f.desglose.map((d) => (
                      <li key={d}>{d}</li>
                    ))}
                  </ul>
                )}
                {f.acusacionFinal?.razon && <p className="tenue">«{f.acusacionFinal.razon}»</p>}
              </details>
            </li>
          )
        })}
      </ol>

      <h2>Cómo se puntúa</h2>
      <ul className="desglose leyenda">
        <li>Acertar el asesino en la acusación final: +100</li>
        <li>Acertar el arma: +50</li>
        <li>Objetivo principal cumplido: +50 · cada secundario: +20</li>
        <li>Cada pista comprada en la tienda: −10 (premia investigar en vez de comprar)</li>
        <li>Un punto por cada 50 monedas que le queden</li>
        <li>El asesino: +15 por cada acusación final que no le señala</li>
      </ul>
    </section>
  )
}

function Acusacion({ fila: f }) {
  if (f.asesino) return null
  if (!f.acusacionFinal) return <span className="tenue">Sin acusación final</span>
  const a = f.acusacionFinal
  return (
    <span className="ranking-acusacion">
      <span className={f.aciertaAsesino ? 'acierto' : 'fallo'}>{f.aciertaAsesino ? '✓' : '✕'} {a.sospechoso ?? 'nadie'}</span>
      {a.arma && <span className={f.aciertaArma ? 'acierto' : 'fallo'}>{f.aciertaArma ? '✓' : '✕'} {a.arma}</span>}
    </span>
  )
}
