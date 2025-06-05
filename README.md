# BPM Worker Spring Boot Starter

> **Developer's Teaser:** Turn any Spring method into a Camunda worker with just one annotation!

```java
@BPMWorker("process-payment")
@BPMResult(flatten = true, flattenPrefix = "payment_")
public PaymentResult processPayment(@BPMVariable("amount") Double amount,
                                   @BPMVariable("customerId") String customerId) 
        throws @BPMError(errorCode = "INSUFFICIENT_FUNDS") PaymentException {
    
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
**Modern Java** - Records, var keyword, switch expressions, Java 17+  

## Quick Start

### 1. Add Dependency
```xml
<dependency>
    <groupId>com.jeevision.bpm</groupId>
    <artifactId>bpm-worker-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
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
    
    @BPMWorker("process-order")
    @BPMResult(name = "orderId")
    public String processOrder(@BPMVariable("customerEmail") String email,
                              @BPMVariable("items") List<OrderItem> items) {
        // Your business logic
        return "ORDER_" + System.currentTimeMillis();
    }
}
```

## Core Annotations

### `@BPMWorker`
Marks a method as a Camunda external task worker
```java
@BPMWorker("my-topic")           // Simple
@BPMWorker(value = "my-topic",   // Advanced
           fetchSize = 5, 
           lockDuration = 60000)
```

### `@BPMVariable`
Injects process variables into method parameters
```java
@BPMVariable("customerName")                    // Named variable
@BPMVariable                                    // Uses parameter name
@BPMVariable(value = "optional", 
             required = false, 
             defaultValue = "N/A")             // With defaults
```

### `@BPMResult`
Sets method return value as process variables
```java
@BPMResult(name = "result")                     // Simple
@BPMResult(flatten = true,                      // Flatten object
           flattenPrefix = "order_")
@BPMResult(nullHandling = NullHandling.SKIP)    // Null handling
```

### `@BPMError`
Maps exceptions to BPMN errors
```java
// In throws clause (preferred)
throws @BPMError(errorCode = "VALIDATION_FAILED") ValidationException

// On exception class
@BPMError(errorCode = "INSUFFICIENT_FUNDS")
public class InsufficientFundsException extends Exception { }

// Method-level mappings
@BPMError(errorMappings = {
    @BPMError.ErrorMapping(exception = IllegalArgumentException.class, 
                          errorCode = "INVALID_INPUT")
})
```

## Error Handling

The library automatically distinguishes between:

- **BPMN Errors** (business exceptions) - Trigger error boundary events, not retryable
- **Technical Failures** (system issues) - Create incidents, retryable with backoff

```java
@BPMWorker("validate-payment")
public String validatePayment(@BPMVariable("amount") Double amount) 
        throws @BPMError(errorCode = "INVALID_AMOUNT") IllegalArgumentException {
    
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

@BPMWorker("calculate-order")
@BPMResult(flatten = true, flattenPrefix = "order_")
public OrderResult calculateOrder(@BPMVariable("items") List<Item> items) {
    return new OrderResult("ORD123", 99.50, "CONFIRMED");
    // Creates: order_orderId, order_total, order_status
}
```

### Complex Data Processing
```java
@BPMWorker("process-customer-data")
public String processCustomer(@BPMVariable Map<String, Object> customerData,
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
@BPMWorker("handle-business-logic")
public String handleLogic(@BPMVariable("type") String type) 
        throws @BPMError(errorCode = "VALIDATION_ERROR") ValidationException,
               @BPMError(errorCode = "BUSINESS_RULE_ERROR") BusinessException {
    
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
| `bpm.worker.base-url` | `http://localhost:8080/engine-rest` | Camunda REST API URL |
| `bpm.worker.worker-id` | `spring-boot-worker` | Worker identifier |
| `bpm.worker.max-tasks` | `10` | Max tasks to fetch at once |
| `bpm.worker.lock-duration` | `30000` | Task lock duration (ms) |
| `bpm.worker.auth.username` | - | Basic auth username |
| `bpm.worker.auth.password` | - | Basic auth password |
| `bpm.worker.auth.token` | - | Bearer token |

## Requirements

- **Java 17+**
- **Spring Boot 3.x**  
- **Camunda 7.23+**

## Author

**Slava Yermakov** (v.yermakov@gmail.com)

---

*Build powerful Camunda integrations with minimal code!*