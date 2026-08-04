# Pruebas de Carga y Estrés — Plan JMeter

Plan de pruebas usado en las secciones **8.2 (Carga)** y **8.3 (Estrés)** del informe.

## Requisitos

- [Apache JMeter 5.6.3](https://jmeter.apache.org/) (o superior).
- Backend en ejecución en `http://localhost:8001` con MySQL encendido.
- Un usuario **administrador** válido. El plan inicia sesión una vez y reutiliza el token
  JWT en todas las peticiones (grupo *setUp*). Ajusta el correo/contraseña en el sampler
  **"POST login"** del `.jmx` a un administrador real de tu base de datos.

## Qué mide

Tres operaciones de lectura del panel de administración, todas con acceso a base de datos:

- `GET /casaketteler/indexuser`
- `GET /casaketteler/attendance/kpi`
- `GET /casaketteler/attendance/filter`

## Cómo ejecutar

```powershell
# Prueba de CARGA (20 usuarios, 60 s)
jmeter -n -t casaketteler-load.jmx -l carga.jtl -Jthreads=20 -Jrampup=10 -Jduration=60

# Prueba de ESTRÉS (escalar: repetir subiendo -Jthreads)
jmeter -n -t casaketteler-load.jmx -l estres_100.jtl -Jthreads=100 -Jrampup=5 -Jduration=30
```

Parámetros configurables por línea de comandos:

| Propiedad | Significado | Valor por defecto |
|-----------|-------------|:-----------------:|
| `threads` | Usuarios concurrentes | 10 |
| `rampup` | Segundos para levantar todos los hilos | 10 |
| `duration` | Segundos de carga sostenida | 60 |

El archivo `.jtl` resultante contiene una fila por petición (tiempo, código, éxito); se
puede abrir en la interfaz de JMeter (*Summary Report* / *Aggregate Report*) o procesar
con scripts para obtener promedios y percentiles.
