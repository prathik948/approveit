# ApproveIt – Payment Approval Service

## 1. Project Overview

ApproveIt is a backend REST API application developed using Java and Spring Boot. It simulates a secure Maker-Checker payment approval workflow.

The application allows Makers to create and submit payments, Checkers to approve or reject payments, and Admins to release approved payments.

No actual money is transferred. Payment release is simulated.

## 2. Technologies Used

- Java 21
- Spring Boot 4.1.2
- Spring Data JPA
- Hibernate
- H2 Database
- Maven
- REST APIs
- JUnit 5
- Mockito
- Postman

## 3. User Roles

| Role | Responsibilities |
|---|---|
| MAKER | Create, edit and submit payments |
| CHECKER | Approve or reject submitted payments |
| ADMIN | Release approved payments |
| MAKER + CHECKER | Perform both Maker and Checker operations, except approving or rejecting their own payments |

The caller is identified using the `X-User-Id` HTTP request header. No actual login or authentication system is implemented.

## 4. Payment Lifecycle

```text
DRAFT
  |
  | Submit by Maker
  v
SUBMITTED
  |
  | Checker approval(s)
  v
APPROVED
  |
  | Admin release
  v
RELEASED
```

A Checker can also reject a submitted payment:

```text
SUBMITTED → REJECTED
```

`REJECTED` and `RELEASED` are final states.

## 5. Business Rules

1. Only Makers can create payments.
2. Only the original Maker can edit or submit a payment.
3. Makers cannot approve or reject their own payments.
4. Payments up to ₹10,00,000 require one Checker approval.
5. Payments above ₹10,00,000 require two distinct Checker approvals.
6. A Checker cannot approve the same payment twice.
7. Rejection requires a reason.
8. Each Maker can submit a maximum of ₹50,00,000 per calendar day (IST).
9. Rejected payments and payments edited back to DRAFT do not count towards the daily limit.
10. Editing a submitted payment resets it to DRAFT and clears previous approvals.
11. Only Admins can release approved payments.
12. Every action is recorded in the audit log, including failed attempts.

## 6. API Endpoints

Base URL:

`http://localhost:8080`

All endpoints that perform user-specific actions require:

`X-User-Id: USER_ID`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/payments` | Create payment |
| GET | `/api/payments` | Get all payments |
| GET | `/api/payments/{id}` | Get payment by ID |
| PUT | `/api/payments/{id}` | Edit payment |
| PUT | `/api/payments/{id}/submit` | Submit payment |
| PUT | `/api/payments/{id}/approve` | Approve payment |
| PUT | `/api/payments/{id}/reject` | Reject payment |
| PUT | `/api/payments/{id}/release` | Release payment |
| GET | `/api/audit-logs` | Get audit logs |

## 7. API Request Examples

### 7.1 Create Payment

**Method:** POST

**URL:** `/api/payments`

**Header:**
`X-User-Id: M1`

**Body (JSON):**

```json
{
  "beneficiaryName": "John Doe",
  "beneficiaryId": "BEN001",
  "amount": 25000.00,
  "purpose": "Vendor payment"
}
```

Expected status: `201 Created`

Initial payment status: `DRAFT`

### 7.2 Get All Payments

**Method:** GET

**URL:** `/api/payments`

Returns the list of payments.

### 7.3 Get Payment By ID

**Method:** GET

**URL:** `/api/payments/1`

Returns the details of a specific payment.

### 7.4 Edit Payment

**Method:** PUT

**URL:** `/api/payments/1`

**Header:**
`X-User-Id: M1`

**Body (JSON):**

```json
{
  "beneficiaryName": "John Doe Updated",
  "beneficiaryId": "BEN001",
  "amount": 30000.00,
  "purpose": "Updated vendor payment"
}
```

Only the original Maker can edit a DRAFT or SUBMITTED payment.

### 7.5 Submit Payment

**Method:** PUT

**URL:** `/api/payments/1/submit`

**Header:**
`X-User-Id: M1`

Submits a DRAFT payment for approval.

Expected status after submission: `SUBMITTED`

### 7.6 Approve Payment

**Method:** PUT

**URL:** `/api/payments/1/approve`

**Header:**
`X-User-Id: C1`

Approves a submitted payment.

The payment becomes `APPROVED` once the required number of distinct Checkers have approved it.

### 7.7 Reject Payment

**Method:** PUT

**URL:** `/api/payments/1/reject`

**Header:**
`X-User-Id: C1`

**Body (JSON):**

```json
{
  "reason": "Incorrect beneficiary details"
}
```

The payment status changes to `REJECTED`.

### 7.8 Release Payment

**Method:** PUT

**URL:** `/api/payments/1/release`

**Header:**
`X-User-Id: A1`

Releases an approved payment.

Expected status after release: `RELEASED`

### 7.9 Get Audit Logs

**Method:** GET

**URL:** `/api/audit-logs`

Returns audit records containing the actor, action, timestamp, state before, state after, outcome and message.

## 8. Sample Workflow

### Scenario 1: Payment of ₹25,000

1. M1 creates a payment.
2. M1 submits the payment.
3. C1 approves the payment.
4. A1 releases the payment.

Final status: `RELEASED`

### Scenario 2: Payment of ₹15,00,000

1. M2 creates a payment.
2. M2 submits the payment.
3. C1 provides the first approval.
4. Payment remains `SUBMITTED`.
5. C2 provides the second approval.
6. Payment becomes `APPROVED`.
7. A1 releases the payment.

Final status: `RELEASED`

## 9. Database Configuration

The application uses the H2 database with file-based persistence.

Database URL:

`jdbc:h2:file:./data/approveitdb`

H2 Console:

`http://localhost:8080/h2-console`

Database credentials:

- Username: `sa`
- Password: (blank)

## 10. Running the Application

1. Clone or download the project.
2. Open the project in IntelliJ IDEA.
3. Ensure Java 21 is configured.
4. Load Maven dependencies.
5. Run `ApproveItApplication.java`.

The application starts at:

`http://localhost:8080`

## 11. Running Automated Tests

Run all automated tests using Maven:

```bash
mvn test
```

Or execute `PaymentServiceTest.java` directly from IntelliJ IDEA.

The current test suite contains 27 passing tests.

## 12. Key Features

- Role-based payment operations
- Maker-Checker approval workflow
- Multi-level payment approvals
- Daily submission limit
- Payment lifecycle validation
- Rejection with mandatory reason
- Audit logging for successful and failed actions
- Transaction management
- Pessimistic locking for concurrent approvals
- H2 database persistence
- Automated unit and integration testing

## 13. Future Enhancements

- JWT-based authentication and authorization
- Swagger/OpenAPI documentation
- Frontend dashboard
- Notifications for payment approvals
- Production database integration

---

**Project Name:** ApproveIt – Payment Approval Service

**Developed Using:** Java, Spring Boot, Spring Data JPA and H2 Database