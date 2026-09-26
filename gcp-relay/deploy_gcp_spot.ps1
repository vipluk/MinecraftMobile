# Skrypt automatycznego wdrożenia maszyny Spot w GCP (Frankfurt, Niemcy)
# Projekt GCP: wordcast-495801 (lub aktywny projekt gcloud)

$PROJECT_ID = "wordcast-495801"
$INSTANCE_NAME = "minecraft-relay-spot"
$ZONE = "europe-west3-a"
$MACHINE_TYPE = "e2-small" # lub e2-micro

Write-Host "=== Wdrażanie maszyny GCP Spot dla pearium.com w Frankfurt ($ZONE) ===" -ForegroundColor Cyan

# 1. Tworzenie reguł zapory ogniowej (Firewall)
Write-Host "Tworzenie reguł zapory ogniowej..." -ForegroundColor Yellow
gcloud compute firewall-rules create allow-minecraft-relay `
    --project=$PROJECT_ID `
    --direction=INGRESS `
    --priority=1000 `
    --network=default `
    --action=ALLOW `
    --rules=tcp:80,tcp:443,tcp:7000,tcp:25565 `
    --source-ranges=0.0.0.0/0 `
    --target-tags=minecraft-relay `
    --description="Reguła dla WWW (80/443), Tunelu FRP (7000) i Minecraft (25565)" `
    --quiet

# 2. Tworzenie instancji SPOT (maksymalna zniżka rzędu ~80%)
Write-Host "Uruchamianie taniej instancji Spot..." -ForegroundColor Yellow
gcloud compute instances create $INSTANCE_NAME `
    --project=$PROJECT_ID `
    --zone=$ZONE `
    --machine-type=$MACHINE_TYPE `
    --provisioning-model=SPOT `
    --instance-termination-action=STOP `
    --image-family=ubuntu-2204-lts `
    --image-project=ubuntu-os-cloud `
    --boot-disk-size=15GB `
    --boot-disk-type=pd-standard `
    --tags=minecraft-relay,http-server,https-server `
    --metadata-from-file=startup-script="$PSScriptRoot\setup_relay.sh" `
    --quiet

Write-Host "Pobieranie zewnętrznego adresu IP..." -ForegroundColor Yellow
$EXTERNAL_IP = (gcloud compute instances describe $INSTANCE_NAME --zone=$ZONE --format='get(networkInterfaces[0].accessConfigs[0].natIP)').Trim()

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Maszyna GCP Spot została utworzona!" -ForegroundColor Green
Write-Host "Zewnętrzny adres IP: $EXTERNAL_IP" -ForegroundColor Cyan
Write-Host ""
Write-Host "KROK NASTĘPNY - KONFIGURACJA DOMENY pearium.com:" -ForegroundColor Yellow
Write-Host "1. W panelu zarządzania domeną pearium.com (Cloudflare / OVH / CyberFolks) dodaj rekord:" -ForegroundColor White
Write-Host "   Typ: A | Nazwa: @ (lub pearium.com) | Wartość: $EXTERNAL_IP" -ForegroundColor White
Write-Host "   Typ: A | Nazwa: www | Wartość: $EXTERNAL_IP" -ForegroundColor White
Write-Host ""
Write-Host "2. Połącz się przez SSH i uruchom skrypt setup_relay.sh:" -ForegroundColor White
Write-Host "   gcloud compute ssh $INSTANCE_NAME --zone=$ZONE" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Green
