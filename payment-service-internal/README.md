# Payment Service Internal

Kafka consumer service for internal payment processing.

- consumes from `ORDERTOPIC`,
- simulates internal payment authorization,
- updates order payment status via Order Service callback.

## Local run

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
Set-Location D:\java-workspace\restaurantmanagement
.\mvnw.cmd -pl payment-service-internal -am spring-boot:run
```

