# create-kafka-topics.ps1
# PowerShell script to create all required Kafka topics.
# Run AFTER docker-compose up has started kafka-broker.
# Usage: .\scripts\create-kafka-topics.ps1

param(
    [string]$KafkaBroker = "localhost:29092"
)

Write-Host "=== Creating Kafka Topics ===" -ForegroundColor Cyan

$Topics = @(
    @{ Name = "ORDERTOPIC";             Partitions = 3; Replication = 1 },
    @{ Name = "ORDER_EVENTS_DLQ";       Partitions = 1; Replication = 1 },
    @{ Name = "ORDER_PAYMENT_RESPONSES";Partitions = 3; Replication = 1 },
    @{ Name = "PAYMENT_STATUS_UPDATES"; Partitions = 3; Replication = 1 }
)

foreach ($topic in $Topics) {
    Write-Host "Creating topic: $($topic.Name)  (partitions=$($topic.Partitions), replication=$($topic.Replication))" -ForegroundColor Yellow
    docker exec kafka-broker kafka-topics `
        --bootstrap-server kafka-broker:9092 `
        --create `
        --if-not-exists `
        --topic $topic.Name `
        --partitions $topic.Partitions `
        --replication-factor $topic.Replication

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] $($topic.Name) ready" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Failed to create $($topic.Name)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Topic List ===" -ForegroundColor Cyan
docker exec kafka-broker kafka-topics --bootstrap-server kafka-broker:9092 --list

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Cyan

