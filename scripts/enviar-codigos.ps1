<#
.SYNOPSIS
  Manda a cada jugador su correo con el enlace de acceso y el código, a partir de un CSV y una plantilla.

.DESCRIPTION
  Lee un CSV (Nombre;Email;Codigo) y una plantilla de texto con {NOMBRE}, {CODIGO} y {ENLACE}.
  Los datos reales viven en contenido-privado/ (fuera de git). Tres formas de usarlo:

    # 1) Mandarlos todos desde tu Gmail (pide usuario y contraseña de aplicación):
    .\scripts\enviar-codigos.ps1 -De tu@gmail.com

    # 2) Primero una prueba: todos los correos llegan a tu propia dirección.
    .\scripts\enviar-codigos.ps1 -De tu@gmail.com -Prueba tu@gmail.com

    # 3) Sin contraseñas: genera una página con un botón por persona que abre Gmail ya relleno.
    .\scripts\enviar-codigos.ps1 -Enlaces

  Gmail con verificación en dos pasos necesita una "contraseña de aplicación"
  (Cuenta de Google → Seguridad → Contraseñas de aplicaciones). Las filas sin correo se saltan.
#>
param(
  [string]$Csv = "contenido-privado/la-ultima-noche/destinatarios.csv",
  [string]$Plantilla = "contenido-privado/la-ultima-noche/correo-plantilla.txt",
  [string]$Asunto = "La Última Noche · tu personaje te espera (23-25 oct)",
  [string]$Base = "https://misterio-en-vivo.onrender.com",
  [string]$De = "",
  [string]$Prueba = "",
  [switch]$Enlaces
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding $false
$plantillaTexto = [IO.File]::ReadAllText((Resolve-Path $Plantilla), $utf8)
$filas = Import-Csv -Path $Csv -Delimiter ';' -Encoding UTF8

function Cuerpo($fila) {
  $enlace = "$Base/?codigo=$($fila.Codigo)"
  return $plantillaTexto.Replace('{NOMBRE}', $fila.Nombre).Replace('{CODIGO}', $fila.Codigo).Replace('{ENLACE}', $enlace)
}

if ($Enlaces) {
  $salida = Join-Path (Split-Path (Resolve-Path $Csv)) 'correos.html'
  $html = New-Object System.Text.StringBuilder
  [void]$html.Append('<!doctype html><html lang="es"><head><meta charset="utf-8"><title>Correos de acceso</title>')
  [void]$html.Append('<style>body{font-family:system-ui;max-width:40rem;margin:2rem auto;padding:0 1rem}a.b{display:block;padding:.8rem 1rem;margin:.5rem 0;background:#1f2937;color:#fff;border-radius:.5rem;text-decoration:none}a.w{background:#128c7e;margin-top:-.3rem}small{color:#ddd}</style></head><body>')
  [void]$html.Append('<h1>Correos de acceso</h1><p>Cada botón abre tu correo con el mensaje ya escrito; solo tienes que darle a enviar.</p>')
  foreach ($f in $filas) {
    $cuerpo = Cuerpo $f
    $mailto = 'mailto:' + [uri]::EscapeDataString($f.Email) + '?subject=' + [uri]::EscapeDataString($Asunto) + '&body=' + [uri]::EscapeDataString($cuerpo)
    $aviso = if ($f.Email) { '' } else { ' <small>(sin correo en el CSV)</small>' }
    [void]$html.Append("<a class=`"b`" href=`"$mailto`">Correo a $($f.Nombre)$aviso</a>")
    # Con teléfono (columna opcional Telefono, con prefijo de país y sin espacios): el mismo mensaje por WhatsApp.
    $telefono = if ($f.PSObject.Properties['Telefono']) { ($f.Telefono -replace '[^0-9]', '') } else { '' }
    if ($telefono) {
      $wa = 'https://wa.me/' + $telefono + '?text=' + [uri]::EscapeDataString($cuerpo)
      [void]$html.Append("<a class=`"b w`" href=`"$wa`" target=`"_blank`">WhatsApp a $($f.Nombre)</a>")
    }
  }
  [void]$html.Append('</body></html>')
  [IO.File]::WriteAllText($salida, $html.ToString(), $utf8)
  Write-Host "Página creada: $salida"
  exit 0
}

if (-not $De) { throw "Indica tu dirección con -De tu@gmail.com (o usa -Enlaces para no mandar nada desde aquí)." }
$cred = Get-Credential -UserName $De -Message "Gmail: tu dirección y la contraseña de aplicación"

$enviados = 0
$saltados = @()
foreach ($f in $filas) {
  if (-not $f.Email -and -not $Prueba) { $saltados += $f.Nombre; continue }
  $para = if ($Prueba) { $Prueba } else { $f.Email }
  $asuntoFinal = if ($Prueba) { "[PRUEBA $($f.Nombre)] $Asunto" } else { $Asunto }
  Send-MailMessage -From $De -To $para -Subject $asuntoFinal -Body (Cuerpo $f) -Encoding $utf8 `
    -SmtpServer 'smtp.gmail.com' -Port 587 -UseSsl -Credential $cred
  Write-Host "Enviado a $($f.Nombre) ($para)"
  $enviados++
}
Write-Host "Enviados: $enviados"
if ($saltados.Count -gt 0) { Write-Warning "Sin correo en el CSV: $($saltados -join ', ')" }
