# Payment Service External

Kafka consumer service for external payment providers (e.g., Stripe).

- consumes from `ORDERTOPIC`,
- simulates third-party payment processing,
- updates order payment status via Order Service callback.

## Local run

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
Set-Location D:\java-workspace\restaurantmanagement
.\mvnw.cmd -pl payment-service-external -am spring-boot:run
```

