# Encargo para la siguiente sesión

Documento de traspaso. Pegar el apartado 3 al abrir una sesión nueva; evita reconstruir
el contexto desde cero.

---

## 1. Dónde quedó todo

| Elemento | Estado |
|----------|--------|
| Backend, rama `pruebas` | Al día y subido |
| Frontend, rama `main` | Al día y subido |
| Interfaz web que sirve el backend | Al día, con URL relativa (funciona en cualquier red) |
| Accesibilidad | 219 hallazgos → 1; ese último ya corregido |
| Instalador `.exe` | Generado, 335 MB, con Python y modelos incluidos |
| APK versión 1.1 | **Compilado pero SIN publicar** |
| Despliegue en la nube | Evaluado; decisión pendiente |

### Lo único que bloquea

El APK 1.1 apunta a `http://192.168.101.3:8001`, una IP que ya no existe. **No debe
repartirse así.** Hay que recompilarlo con la dirección definitiva y publicarlo.

---

## 2. La decisión que hay que tomar primero

Del análisis en [PLAN-DESPLIEGUE-NUBE.md](PLAN-DESPLIEGUE-NUBE.md):

| Opción | Coste | ¿Se puede marcar asistencia? | Esfuerzo |
|--------|------:|:---------------------------:|----------|
| **C. Túnel** (recomendada) | 0 | **Sí** | Bajo |
| **B. Render, solo demostración** | 0 | **No** | Medio |
| **A. Render completo** | ~40 USD/mes | Sí | Alto |

Render gratuito no puede con el reconocimiento facial: necesita más de 1 GB de memoria
y el plan gratuito da 512 MB. Además el servicio se apaga tras 15 minutos de
inactividad y tarda cerca de un minuto en despertar, lo que hace inviable marcar
asistencia.

**Sin decidir esto, nada más avanza**, porque la dirección del servidor determina cómo
se compila el APK.

---

## 3. Encargo para pegar en la sesión nueva

> Proyecto Casa Ketteler. Backend en
> `c:\Users\yerry\Documents\Ingenieria de Software\BackendCasaKetteler\ProjectCasaKetteler`
> (rama `pruebas`) y frontend en
> `C:\Users\yerry\Documents\Ingenieria de Software\II\Front Ketteler\front-ketteler`
> (rama `main`).
>
> Contexto: lee `docs/SIGUIENTE-SESION.md` y `docs/PLAN-DESPLIEGUE-NUBE.md`.
>
> Decidimos la opción **[C / B]**. Quiero, en este orden:
>
> 1. Dejar el sistema accesible desde fuera con esa opción.
> 2. Recompilar el APK apuntando a la dirección nueva y publicarlo en la página de
>    descarga (repositorio `CasaKetteler-Pagina`).
> 3. Actualizar `docs/DESPLIEGUE.md` y `docs/OPERACION.md` con el procedimiento.
>
> Trabaja por partes y confírmame cada una antes de seguir.

---

## 4. Orden de trabajo y cuánto pesa cada parte

Ordenado por prioridad: si hay que cortar, se corta desde abajo.

| # | Tarea | Peso | Se puede dejar fuera |
|---|-------|------|:--------------------:|
| 1 | Montar el acceso externo (túnel o Render) | Medio | No |
| 2 | Recompilar el APK con la dirección nueva | Bajo | No |
| 3 | Publicar el APK en la página de descarga | Bajo | No |
| 4 | Comprobar de punta a punta (entrar y marcar) | Bajo | No |
| 5 | Actualizar `DESPLIEGUE.md` y `OPERACION.md` | Bajo | Sí, si aprieta |
| 6 | Apartado de despliegue para el informe | Medio | Sí |
| 7 | Dejar el túnel como servicio automático | Bajo | Sí |

Las cuatro primeras son el camino crítico: con eso el sistema queda usable desde fuera y
los residentes con una app que funciona. Lo demás es documentación y comodidad.

---

## 5. Datos que harán falta a mano

| Dato | Dónde está |
|------|-----------|
| Contraseña de MySQL de la PC destino | La define quien instala |
| Firma del APK | `android/casaketteler-release.keystore` + `keystore.properties` |
| Repositorio de la página | `github.com/192200-coder/CasaKetteler-Pagina` |
| Dirección del servidor en la app | `src/environments/environment.prod.ts` |

> **Recordatorio:** subir `versionCode` en `android/app/build.gradle` en cada APK nuevo
> (va por 2). Sin eso, Android no instala la versión nueva sobre la anterior.

---

## 6. Advertencias que conviene no olvidar

- **La llave de firma es irreemplazable.** Si se pierde, no se podrá actualizar la app
  nunca más. Debe estar respaldada fuera de la computadora.
- El `JWT_SECRET` de desarrollo quedó en el historial de git: en producción debe ser
  otro (el instalador ya genera uno nuevo).
- La red Wi-Fi de la residencia (SSID/BSSID) se registra **a mano** en la tabla
  `tresidence`. Sin ese dato **ningún residente puede marcar asistencia**.
- Si la PC de destino se restaura al reiniciar (Deep Freeze y similares), la instalación
  desaparece al apagarla.
