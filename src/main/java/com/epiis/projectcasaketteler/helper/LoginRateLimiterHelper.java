package com.epiis.projectcasaketteler.helper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * Bloqueo por IP, independiente del bloqueo por cuenta que ya existe en
 * BusinessUser.login(). Evita fuerza bruta distribuida entre muchas cuentas
 * distintas desde la misma conexión. En memoria: suficiente para un solo
 * servidor; si algún día hay más de una instancia detrás de un balanceador,
 * esto debería moverse a un almacén compartido (Redis).
 */
@Component
public class LoginRateLimiterHelper {

    private static final int MAX_INTENTOS_POR_IP = 15;
    private static final long VENTANA_MS = 15 * 60 * 1000; // 15 minutos

    private static class Contador {
        AtomicInteger intentos = new AtomicInteger(0);
        volatile long inicioVentana = System.currentTimeMillis();
    }

    private final ConcurrentHashMap<String, Contador> intentosPorIp = new ConcurrentHashMap<>();

    public boolean estaBloqueada(String ip) {
        Contador contador = intentosPorIp.get(ip);
        if (contador == null || haExpiradoVentana(contador)) {
            return false;
        }
        return contador.intentos.get() >= MAX_INTENTOS_POR_IP;
    }

    public void registrarIntentoFallido(String ip) {
        Contador contador = intentosPorIp.computeIfAbsent(ip, k -> new Contador());
        if (haExpiradoVentana(contador)) {
            synchronized (contador) {
                if (haExpiradoVentana(contador)) {
                    contador.intentos.set(0);
                    contador.inicioVentana = System.currentTimeMillis();
                }
            }
        }
        contador.intentos.incrementAndGet();
    }

    public void registrarLoginExitoso(String ip) {
        intentosPorIp.remove(ip);
    }

    private boolean haExpiradoVentana(Contador contador) {
        return (System.currentTimeMillis() - contador.inicioVentana) > VENTANA_MS;
    }
}