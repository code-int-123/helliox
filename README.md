# Helliox

A Spring Boot REST API that conducts a medical consultation questionnaire and determines which prescriptions are forbidden based on the patient's answers.

---

## How It Works

1. The client fetches a list of questions via `GET /questions`
2. Each question has a set of options — some options are marked as forbidden
3. The client submits answers via `POST /consultation`
4. The API evaluates each answer and returns a list of forbidden prescriptions

Questions and prescriptions are loaded from JSON files at startup — no database required.

---

## Endpoints

### `GET /questions`
Returns all consultation questions with their answer options.

**Response `200`**
```json
[
  {
    "uuid": "7f3a1c2e-84b5-4d9f-a6e0-1b2c3d4e5f60",
    "question": "Do you take drugs?",
    "options": [
      { "id": "A", "option": "Yes" },
      { "id": "B", "option": "No" }
    ]
  }
]
```

---

### `POST /consultation`
Submits answers and returns forbidden prescriptions.

**Request body**
```json
[
  { "questionId": "7f3a1c2e-84b5-4d9f-a6e0-1b2c3d4e5f60", "answer": "A" }
]
```

**Response `200`**
```json
{
  "forbiddenPrescriptions": ["Aspirin", "Antiseptic"]
}
```

**Error responses**

| Status | Reason |
|--------|--------|
| `400`  | Missing or null required fields, invalid UUID format, malformed JSON |
| `415`  | Missing or unsupported `Content-Type` header |
| `500`  | Unexpected server error |

---

## Project Structure

```
src/main/java/com/example/helliox/
├── controller/
│   ├── ConsultationController.java   # REST endpoints
│   └── GlobalExceptionHandler.java   # Centralised error handling
├── mapper/
│   └── QuestionMapper.java           # Domain → API model mapping
├── model/
│   ├── Option.java                   # Answer option (id, text, isForbidden)
│   ├── Prescription.java             # Prescription (id, name)
│   └── QA.java                       # Question with options and forbidden prescriptions
└── service/
    └── ConsultationService.java      # Business logic and JSON loading

src/main/resources/
├── api.yaml                          # OpenAPI 3.0 specification
├── qa.json                           # Questions and options data
└── prescription.json                 # Prescriptions data
```

---

## Data Files

### `prescription.json`
Defines available prescriptions with a UUID and name.

```json
[
  { "id": "c3d4e5f6-a7b8-9012-cdef-123456789012", "prescriptionName": "Aspirin" }
]
```

### `qa.json`
Defines questions, answer options, and which prescription UUIDs become forbidden when a forbidden option is selected.

```json
[
  {
    "questionId": "7f3a1c2e-84b5-4d9f-a6e0-1b2c3d4e5f60",
    "question": "Do you take drugs?",
    "forbiddenPrescriptions": ["c3d4e5f6-a7b8-9012-cdef-123456789012"],
    "options": [
      { "id": "A", "option": "Yes", "isForbidden": true },
      { "id": "B", "option": "No",  "isForbidden": false }
    ]
  }
]
```

---

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 21 | Language |
| Spring Boot 4 | Application framework |
| OpenAPI Generator | Code generation from `api.yaml` |
| Jackson | JSON parsing |
| Lombok | Boilerplate reduction |
| JUnit 5 + Mockito | Unit and integration testing |

---

## Running the Application

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

---

## Running Tests

```bash
./mvnw test
```

Tests include:
- **Unit tests** — `QuestionMapperTest`, `ConsultationServiceTest`
- **Controller tests** — `ConsultationControllerTest` (with mocks)
- **E2E tests** — `ConsultationE2eTest` (full Spring context, real data)