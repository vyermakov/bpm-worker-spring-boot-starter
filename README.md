 BPM Worker Spring Boot Starter

> **Developer's Teaser:** Turn any Spring method into a CIB Sevent worker with just one annotation!

```java
@BpmWorker("process-payment")
@BpmResult("payment")
public PaymentResult processPayment(@BpmVariable Double amount,
                                   @BpmVariable String customerId) 
        throws @BpmError(code = "INSUFFICIENT_FUNDS") PaymentException {
    
    // Your business logic here
    return new PaymentResult(generateId(), amount, "SUCCESS");
}
```

**That's it!** No boilerplate, no manual registration, no complex configuration. Just annotate and go!

---

## What You Get

**Zero Configuration** - Auto-discovers and registers your workers  
**Smart Variable Injection** - Automatic type conversion from process variables  
**Flexible Result Handling** - Flatten POJOs into multiple process variables  
**Intelligent Error Handling** - BPMN errors vs technical failures, automatically handled  

## Quick Start

### 1. Add Dependency
```xml
<dependency>
    <groupId>com.jeevision.bpm</groupId>
    <artifactId>bpm-worker-spring-boot-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

### 2. Configure Connection
```yaml
bpm:
  worker:
    base-url: http://localhost:8080/engine-rest
    worker-id: my-app-worker
    auth:
      username: demo
      password: demo
```

### 3. Create Workers
```java
@Service
public class OrderService {
    
    @BpmWorker("process-order")
    @BpmResult(name = "orderId")
    public String processOrder(@BpmVariable("customerEmail") String email,
                              @BpmVariable("items") List<OrderItem> items) {
        // Your business logic
        return "ORDER_" + System.currentTimeMillis();
    }
}
```

## Core Annotations

### `@BpmWorker`
Marks a method as a CIB Seven external task worker
```java
@BpmWorker("my-topic")           // Simple
@BpmWorker(value = "my-topic",   // Advanced
           fetchSize = 5, 
           lockDuration = 60000)
```

### `@BpmVariable`
Injects process variables into method parameters
```java
@BpmVariable("customerName")                    // Named variable
@BpmVariable                                    // Uses parameter name
@BpmVariable(value = "optional", 
             required = false, 
             defaultValue = "N/A")             // With defaults
```

### `@BpmResult`
Sets method return value as process variables
```java
@BpmResult(name = "result")                     // Simple
@BpmResult(flatten = true,                      // Flatten object
           flattenPrefix = "order_")
@BpmResult(nullHandling = NullHandling.SKIP)    // Null handling
```

### `@BpmError`
Maps exceptions to BPMN errors
```java
// In throws clause (preferred)
throws @BpmError(code = "VALIDATION_FAILED") ValidationException
```

## Error Handling

The library automatically distinguishes between:

- **BPMN Errors** (business exceptions) - Trigger error boundary events, not retryable
- **Technical Failures** (system issues) - Create incidents, retryable with backoff

```java
@BpmWorker("validate-payment")
public String validatePayment(@BpmVariable("amount") Double amount) 
        throws @BpmError(code = "INVALID_AMOUNT") IllegalArgumentException {
    
    if (amount <= 0) {
        throw new IllegalArgumentException("Amount must be positive"); // → BPMN Error
    }
    
    if (externalService.isDown()) {
        throw new RuntimeException("Service unavailable"); // → Technical Incident
    }
    
    return "VALIDATED";
}
```

## Advanced Examples

### Record Support
```java
public record OrderResult(String orderId, Double total, String status) {}

@BpmWorker("calculate-order")
@BpmResult(flatten = true, flattenPrefix = "order_")
public OrderResult calculateOrder(@BpmVariable("items") List<Item> items) {
    return new OrderResult("ORD123", 99.50, "CONFIRMED");
    // Creates: order_orderId, order_total, order_status
}
```

### Complex Data Processing
```java
@BpmWorker("process-customer-data")
public String processCustomer(@BpmVariable Map<String, Object> customerData,
                            ExternalTask task) {
    var customerId = (String) customerData.get("id");
    var email = (String) customerData.get("email");
    
    // Access full task context
    log.info("Processing customer {} in process {}", 
             customerId, task.getProcessInstanceId());
    
    return "PROCESSED";
}
```

### Error Inheritance
```java
@BpmWorker("handle-business-logic")
public String handleLogic(@BpmVariable("type") String type) 
        throws @BpmError(code = "VALIDATION_ERROR") ValidationException,
               @BpmError(code = "BUSINESS_RULE_ERROR") BusinessException {
    
    return switch (type) {
        case "VALIDATE" -> throw new ValidationException("Failed validation");
        case "BUSINESS" -> throw new BusinessException("Rule violation");
        case "SYSTEM" -> throw new RuntimeException("System error"); // Technical failure
        default -> "SUCCESS";
    };
}
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `bpm.worker.enabled` | `true` | Enable/disable workers |
| `bpm.worker.base-url` | `http://localhost:8080/engine-rest` | CIB Seven REST API URL |
| `bpm.worker.worker-id` | `spring-boot-worker` | Worker identifier |
| `bpm.worker.max-tasks` | `10` | Max tasks to fetch at once |
| `bpm.worker.lock-duration` | `30000` | Task lock duration (ms) |
| `bpm.worker.auth.username` | - | Basic auth username |
| `bpm.worker.auth.password` | - | Basic auth password |
| `bpm.worker.auth.token` | - | Bearer token |

## Requirements

- **Java 17+**
- **Spring Boot 3.x**  
- **CIB Seven 1.0+**

## Author

**Slava Yermakov** (v.yermakov@gmail.com)

---

*Build powerful CIB Seven integrations with minimal code!*