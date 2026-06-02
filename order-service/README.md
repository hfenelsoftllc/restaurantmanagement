# Order Service

Spring Boot microservice that:

- exposes secured REST endpoints for creating and managing orders,
- persists orders to MySQL,
- publishes signed order events to Kafka topic `ORDERTOPIC`.

## Local run

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
Set-Location D:\java-workspace\restaurantmanagement
.\mvnw.cmd -pl order-service -am spring-boot:run
```

## Run tests

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
Set-Location D:\java-workspace\restaurantmanagement
.\mvnw.cmd -pl order-service -am test
```

## Key endpoints

- `POST /api/v1/orders`
- `GET /api/v1/orders/{orderId}`
- `GET /api/v1/orders`
- `PUT /api/v1/orders/{orderId}/status`
- `DELETE /api/v1/orders/{orderId}`

Swagger UI: `http://localhost:9096/docs`

