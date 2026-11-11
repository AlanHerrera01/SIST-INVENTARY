param(
    [Parameter(Mandatory=$true)][string]$MySqlBin,
    [Parameter(Mandatory=$true)][ValidatePattern('^inventory_test_[a-z0-9_]+$')][string]$SourceDatabase,
    [int]$Port = 3307,
    [string]$DatabaseUser = 'root'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$artifactDirectory = Join-Path $projectRoot 'backend/target/recovery-test'
New-Item -ItemType Directory -Force $artifactDirectory | Out-Null
$restoreDatabase = 'inventory_test_restore_' + [Guid]::NewGuid().ToString('N')
$dumpFile = Join-Path $artifactDirectory "$restoreDatabase.sql"
$mysqlExe = Join-Path $MySqlBin 'mysql.exe'
$dumpExe = Join-Path $MySqlBin 'mysqldump.exe'
$connectionArgs = @('--host=127.0.0.1', "--port=$Port", "--user=$DatabaseUser", '--protocol=TCP', '--default-character-set=utf8mb4')
$oldPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $env:TEST_MYSQL_PASSWORD
function Invoke-TestSql([string]$Sql) {
    $result = & $mysqlExe @connectionArgs --batch --skip-column-names "--execute=$Sql"
    if ($LASTEXITCODE -ne 0) { throw 'MySQL verification command failed' }
    return $result
}
try {
    # No --databases: the dump must not contain CREATE/DROP of the source database.
    & $dumpExe @connectionArgs --single-transaction --routines --triggers --hex-blob --set-gtid-purged=OFF --no-tablespaces "--result-file=$dumpFile" $SourceDatabase
    if ($LASTEXITCODE -ne 0) { throw 'Backup failed' }
    Invoke-TestSql "CREATE DATABASE $restoreDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci" | Out-Null
    $restoreArgs = $connectionArgs + @($restoreDatabase)
    $process = Start-Process -FilePath $mysqlExe -ArgumentList $restoreArgs -RedirectStandardInput $dumpFile -RedirectStandardOutput (Join-Path $artifactDirectory 'restore.out') -RedirectStandardError (Join-Path $artifactDirectory 'restore.err') -WindowStyle Hidden -Wait -PassThru
    if ($process.ExitCode -ne 0) { throw 'Restore failed; inspect backend/target/recovery-test/restore.err' }
    $tableNames = @(Invoke-TestSql "SELECT table_name FROM information_schema.tables WHERE table_schema='$SourceDatabase' ORDER BY table_name")
    foreach ($table in $tableNames) {
        if ($table -notmatch '^[a-z0-9_]+$') { throw 'Unexpected table identifier' }
        $sourceCount = Invoke-TestSql "SELECT COUNT(*) FROM $SourceDatabase.$table"
        $restoredCount = Invoke-TestSql "SELECT COUNT(*) FROM $restoreDatabase.$table"
        if ($sourceCount -ne $restoredCount) { throw "Row count mismatch: $table" }
        $sourceChecksum = ((Invoke-TestSql "CHECKSUM TABLE $SourceDatabase.$table EXTENDED") -split '\s+')[-1]
        $restoredChecksum = ((Invoke-TestSql "CHECKSUM TABLE $restoreDatabase.$table EXTENDED") -split '\s+')[-1]
        if ($sourceChecksum -eq 'NULL' -or $sourceChecksum -ne $restoredChecksum) { throw "Checksum mismatch: $table" }
    }
    foreach ($objectKind in @('routines','triggers')) {
        $schemaColumn = if ($objectKind -eq 'routines') { 'routine_schema' } else { 'trigger_schema' }
        $sourceCount = Invoke-TestSql "SELECT COUNT(*) FROM information_schema.$objectKind WHERE $schemaColumn='$SourceDatabase'"
        $restoredCount = Invoke-TestSql "SELECT COUNT(*) FROM information_schema.$objectKind WHERE $schemaColumn='$restoreDatabase'"
        if ($sourceCount -ne $restoredCount) { throw "Object count mismatch: $objectKind" }
    }
    $result = "PASS: backup/restauracion de $($tableNames.Count) tablas, datos, historial Flyway, rutinas y triggers."
    [System.IO.File]::WriteAllText((Join-Path $artifactDirectory 'result.txt'), $result)
    Write-Host $result
} finally {
    # Only this invocation's newly generated destination is removed; source is never modified.
    if ($restoreDatabase -notmatch '^inventory_test_restore_[a-f0-9]{32}$') { throw 'Unsafe cleanup target' }
    Invoke-TestSql "DROP DATABASE IF EXISTS $restoreDatabase" | Out-Null
    $env:MYSQL_PWD = $oldPassword
}
