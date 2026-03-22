$files = Get-ChildItem -Path src -Filter *.java -Recurse | Select-Object -ExpandProperty FullName
& "C:\Program Files (x86)\Java\jdk-1.8\bin\javac.exe" -encoding UTF-8 -d bin -cp "..\MASON_14.jar" $files
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed"
    exit 1
}
& "C:\Program Files (x86)\Java\jdk-1.8\bin\java.exe" -cp "bin;..\MASON_14.jar" tileworld.TileworldMain
