package com.misterioenvivo.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Abre los sobres y manda las visiones cuando llega su hora. Si el servidor estaba
 * dormido (plan gratuito), lo atrasado se entrega en cuanto despierta.
 */
@Component
public class Programador {

    private static final Logger log = LoggerFactory.getLogger(Programador.class);

    private final MecanicasService mecanicas;

    public Programador(MecanicasService mecanicas) {
        this.mecanicas = mecanicas;
    }

    @Scheduled(initialDelay = 5_000, fixedDelay = 15_000)
    public void entregar() {
        try {
            int n = mecanicas.entregarPendientes();
            if (n > 0) {
                log.info("Entregados {} envíos programados", n);
            }
        } catch (RuntimeException e) {
            log.warn("No se pudieron entregar los envíos programados", e);
        }
    }
}
