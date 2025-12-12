$log = "C:\Projects\pulselink\deploy_retry.log"
"Starting deployment retry loop at $(Get-Date)" | Set-Content $log

for ($i=1; $i -le 3; $i++) {
    "Waiting 5 minutes before attempt $i..." | Add-Content $log
    Start-Sleep -Seconds 300
    
    "Attempting deploy ($i/3) at $(Get-Date)..." | Add-Content $log
    
    $p = Start-Process -FilePath "cmd.exe" -ArgumentList "/c firebase deploy --only functions:sendDataMessage" -RedirectStandardOutput "deploy_temp.out" -RedirectStandardError "deploy_temp.err" -PassThru -Wait -NoNewWindow
    
    if (Test-Path "deploy_temp.out") { Get-Content "deploy_temp.out" | Add-Content $log; Remove-Item "deploy_temp.out" }
    if (Test-Path "deploy_temp.err") { Get-Content "deploy_temp.err" | Add-Content $log; Remove-Item "deploy_temp.err" }
    
    if ($p.ExitCode -eq 0) {
        "SUCCESS: Deployment passed on attempt $i." | Add-Content $log
        exit 0
    }
}
"FAILED: Giving up after 3 attempts." | Add-Content $log
