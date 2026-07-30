; ===========================================================================
;  CASA KETTELER - Instalador grafico (Inno Setup 6)
; ===========================================================================
;  Genera UN SOLO archivo .exe que lleva dentro todo el sistema: el backend,
;  la interfaz web, los scripts de reconocimiento facial y el script de
;  preparacion del entorno.
;
;  No se compila a mano: usa  .\construir-instalador.ps1  desde la carpeta
;  del proyecto, que arma la carga util y llama al compilador.
; ===========================================================================

#define Nombre        "Casa Ketteler"
#define Version       "1.0"
#define Organizacion  "Residencia Universitaria Casa Ketteler"
#define Lanzador      "iniciar-casa-ketteler.bat"

; Carpeta con los archivos ya reunidos por construir-instalador.ps1
#ifndef Carga
  #define Carga "carga"
#endif

[Setup]
AppId={{8F3C1A64-2E77-4B59-9D0E-CA5E1B7A9F42}
AppName={#Nombre}
AppVersion={#Version}
AppVerName={#Nombre} {#Version}
AppPublisher={#Organizacion}
VersionInfoVersion={#Version}
VersionInfoDescription=Instalador del sistema de asistencia Casa Ketteler

; Se instala en C:\CasaKetteler y NO en "Program Files" a proposito: el sistema
; escribe continuamente en storage\, logs\ y backups\, y dentro de Program Files
; Windows lo bloquearia salvo con permisos de administrador permanentes.
DefaultDirName={sd}\CasaKetteler
DisableProgramGroupPage=yes
DefaultGroupName={#Nombre}

; Hace falta administrador: se registra el arranque automatico y se configura
; el servicio de MySQL.
PrivilegesRequired=admin

OutputDir=salida
OutputBaseFilename=CasaKetteler-Instalador-{#Version}
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

; Aviso de requisitos previos antes de copiar nada.
InfoBeforeFile=ANTES-DE-INSTALAR.txt

UninstallDisplayName={#Nombre} {#Version}
UninstallDisplayIcon={app}\{#Lanzador}

[Languages]
Name: "es"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "preparar"; \
  Description: "Preparar el entorno ahora (instala las librerias de reconocimiento facial)"; \
  GroupDescription: "Despues de copiar los archivos:"

Name: "escritorio"; \
  Description: "Crear accesos directos en el escritorio"; \
  GroupDescription: "Accesos directos:"; \
  Flags: unchecked

[Files]
; --- Backend compilado ---
Source: "{#Carga}\target\*";         DestDir: "{app}\target";         Flags: ignoreversion recursesubdirs

; --- Interfaz web (la sirve el propio backend) ---
Source: "{#Carga}\frontend\*";       DestDir: "{app}\frontend";       Flags: ignoreversion recursesubdirs

; --- Reconocimiento facial (sin el entorno virtual: se crea en esta PC) ---
Source: "{#Carga}\python_scripts\*"; DestDir: "{app}\python_scripts"; Flags: ignoreversion recursesubdirs

; --- Scripts de operacion y configuracion ---
Source: "{#Carga}\*.bat";            DestDir: "{app}";                Flags: ignoreversion
Source: "{#Carga}\*.ps1";            DestDir: "{app}";                Flags: ignoreversion
Source: "{#Carga}\application.properties"; DestDir: "{app}";          Flags: ignoreversion

; --- Documentacion de operacion ---
Source: "{#Carga}\docs\*";           DestDir: "{app}\docs";           Flags: ignoreversion recursesubdirs skipifsourcedoesntexist

; --- Modelos de reconocimiento ya descargados (si se incluyeron) ---
; Ahorra unos 260 MB de descarga en la PC de destino.
Source: "{#Carga}\python_scripts\.deepface\*"; DestDir: "{app}\python_scripts\.deepface"; \
  Flags: ignoreversion recursesubdirs skipifsourcedoesntexist

; --- Paquetes de Python para instalar sin Internet (si se incluyeron) ---
Source: "{#Carga}\paquetes-python\*"; DestDir: "{app}\paquetes-python"; \
  Flags: ignoreversion recursesubdirs skipifsourcedoesntexist

[Dirs]
; Carpetas de trabajo con permiso de escritura para cualquier usuario: el
; sistema guarda aqui fotos, documentos, respaldos y registros.
Name: "{app}\storage";  Permissions: users-modify
Name: "{app}\temp";     Permissions: users-modify
Name: "{app}\backups";  Permissions: users-modify
Name: "{app}\logs";     Permissions: users-modify

[Icons]
Name: "{group}\Encender Casa Ketteler";  Filename: "{app}\iniciar-casa-ketteler.bat";  WorkingDir: "{app}"
Name: "{group}\Apagar Casa Ketteler";    Filename: "{app}\detener-casa-ketteler.bat";  WorkingDir: "{app}"
Name: "{group}\Abrir el sistema";        Filename: "http://localhost:8001"
Name: "{group}\Manual de operacion";     Filename: "{app}\docs\OPERACION.md"
Name: "{group}\{cm:UninstallProgram,{#Nombre}}"; Filename: "{uninstallexe}"

Name: "{userdesktop}\Encender Casa Ketteler"; Filename: "{app}\iniciar-casa-ketteler.bat"; \
  WorkingDir: "{app}"; Tasks: escritorio
Name: "{userdesktop}\Apagar Casa Ketteler";   Filename: "{app}\detener-casa-ketteler.bat"; \
  WorkingDir: "{app}"; Tasks: escritorio

[Run]
; Prepara el entorno: comprueba programas base, genera el .env, crea la base de
; datos, instala las librerias de Python y registra el arranque automatico.
; Se ejecuta en una consola VISIBLE para que se vea el avance (tarda bastante).
Filename: "powershell.exe"; \
  Parameters: "-NoProfile -ExecutionPolicy Bypass -File ""{app}\instalar.ps1"" -Carpeta ""{app}"""; \
  WorkingDir: "{app}"; \
  StatusMsg: "Preparando el entorno (puede tardar varios minutos)..."; \
  Flags: waituntilterminated; \
  Tasks: preparar

; Abrir el sistema al terminar (opcional, lo elige el usuario al final).
Filename: "http://localhost:8001"; \
  Description: "Abrir el sistema en el navegador"; \
  Flags: postinstall shellexec skipifsilent nowait

[UninstallRun]
; Se detiene el sistema y se quita la tarea de arranque automatico antes de borrar.
Filename: "{app}\detener-casa-ketteler.bat"; RunOnceId: "detener"; Flags: runhidden waituntilterminated
Filename: "schtasks.exe"; Parameters: "/Delete /TN ""Casa Ketteler"" /F"; \
  RunOnceId: "quitartarea"; Flags: runhidden

[UninstallDelete]
; El entorno de Python y los modelos los crea el instalador, no vienen en el
; paquete, asi que hay que borrarlos explicitamente.
Type: filesandordirs; Name: "{app}\python_scripts\venv_perfecto"
Type: filesandordirs; Name: "{app}\python_scripts\.deepface"
Type: filesandordirs; Name: "{app}\logs"
Type: files;          Name: "{app}\.env"

[Messages]
es.WelcomeLabel2=Este asistente instalara [name/ver] en esta computadora.%n%nSe copiara el sistema completo y luego se preparara el entorno: base de datos, librerias de reconocimiento facial y arranque automatico al encender la PC.%n%nSe recomienda cerrar las demas aplicaciones antes de continuar.
es.FinishedLabel=La instalacion termino.%n%nRevisa el resumen que aparecio en la ventana de preparacion: si quedaron puntos pendientes (por ejemplo la red Wi-Fi de la residencia), estan anotados ahi y tambien en la carpeta logs.
