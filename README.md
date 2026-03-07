# PayGuard - Idempotent Payment Processing System 🛡️

PayGuard is a robust, backend-only payment and recharge processing system designed to simulate the transactional guarantees of real-world fintech applications.

The system guarantees **exactly-once payment execution**, successfully preventing duplicate charges, race conditions, and double-spending even under high concurrent load or network timeouts.

## 🧠 The Engineering Challenges Solved

Most basic CRUD applications fail under real-world financial traffic. PayGuard was engineered to solve three specific enterprise challenges:

1. **The "Double Click" Problem (Idempotency):** If a user's network lags and they click "Recharge" multiple times, the system must only deduct the balance once.
2. **The "Double Spend" Problem (Concurrency):** If multiple API requests attempt to drain the exact same wallet simultaneously, the database must safely block all but one.
3. **Data Integrity (ACID Transactions):** If the database crashes mid-process, partial data must be rolled back to prevent users from losing money without receiving their recharge.

## 🛠️ Tech Stack

* **Core:** Java 21, Spring Boot 3.x, Spring Web
* **Database & ORM:** MySQL, Spring Data JPA, Hibernate
* **Caching & Distributed Locks:** Redis (Upstash Cloud)
* **Testing:** JUnit 5, ExecutorService (for multi-threaded concurrency testing)
* **Tools:** Lombok, Spring Validation

## 🏗️ System Architecture & Request Flow

1. **Request Validation:** Incoming payload is strictly validated (e.g., amount > 0, idempotency key present).
2. **Redis Idempotency Check (O(1) Time):** The `IdempotencyKey` is checked against Redis using an atomic `SETNX` operation.
    * *If key exists:* The system intercepts the request and returns the cached transaction response immediately.
    * *If key is new:* The request proceeds to the database.
3. **Optimistic Locking (@Version):** The user's wallet is fetched. When the balance deduction is saved, Hibernate verifies the record's version hash. If a concurrent thread altered the wallet milliseconds prior, an `OptimisticLockException` is thrown and the transaction is safely aborted.

## 🚀 API Documentation

### 1. Process Recharge Payment
Executes a wallet deduction and records a successful transaction.

**Endpoint:** `POST /api/v1/payments/recharge`

**Request Body:**
```json
{
    "idempotencyKey": "unique-uuid-or-timestamp-12345",
    "userId": "user_123",
    "amount": 50.00
}
```
### 2. Success Response (200 OK):

```json
{
  "id": 1,
  "idempotencyKey": "unique-uuid-or-timestamp-12345",
  "userId": "user_123",
  "amount": 50.00,
  "status": "SUCCESS",
  "createdAt": "2026-03-07T15:30:00.123456",
  "updatedAt": "2026-03-07T15:30:00.123456"
}
```
### 3. 🧪 Concurrency Testing

To mathematically prove the system's thread safety, the application includes a custom integration test suite 
(`ConcurrencyIntegrationTest.java`).

The test utilizes an `ExecutorService` and `CountDownLatch` to blast the payment service with **10 simultaneous threads** attempting to drain a single ₹500 wallet at the exact same millisecond.
The test asserts that exactly 1 thread succeeds, 9 threads fail gracefully via `ObjectOptimisticLockingFailureException`, and the final wallet balance never drops below zero.

## 💻 Local Setup & Installation
To run this project locally, you will need Java 21+ and a MySQL instance.

**1. Clone the repository:**

```bash
    git clone [https://github.com/nawab-noddy/PayGuard.git]
```
**2. Create the MySQL Database:**
```bash
CREATE DATABASE payguard_db;
```
**3. Configure Environment Variables:**
For security, database credentials and Redis API keys are managed via environment variables. Configure the following in your IDE or system environment before running:

`DB_USERNAME` : Your MySQL username (e.g., root)

`DB_PASSWORD` : Your MySQL password

`REDIS_PASSWORD` : Your Upstash Redis password

**4. Run the Application:**
```bash
./mvnw spring-boot:run
```

Author-
Mohammad Anas

Thank you for Reading.