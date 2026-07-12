# Run once as Administrator to allow TouchBridge to listen on all interfaces.
netsh http add urlacl url=http://+:47831/ user=Everyone
Write-Host "URL reservation added. You can now run TouchBridge without Administrator."
