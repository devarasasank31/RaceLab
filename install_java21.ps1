$url = 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%2B11/OpenJDK21U-jdk_x64_windows_hotspot_21.0.5_11.msi'
$out = 'C:\temp\OpenJDK21.msi'
New-Item -ItemType Directory -Force -Path C:\temp
Write-Host "Downloading Java 21..."
Invoke-WebRequest -Uri $url -OutFile $out
Write-Host "Installing Java 21..."
Start-Process msiexec.exe -ArgumentList "/i", $out, "/quiet", "/norestart" -Wait
Write-Host "Java 21 installed!"