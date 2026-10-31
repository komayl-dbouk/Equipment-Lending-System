# University Equipment Lending System

A RESTful Web API for managing equipment loans at a university, built with **Java** and **Spring Boot**.

The system will help university staff maintain an inventory of equipment, record student borrowing and returns, check availability, and track overdue loans.

> **Status:** Proposal / planning. The features and endpoints below describe the intended first version and are not yet implemented.

## Overview

University equipment such as laptops, projectors, cameras, and laboratory tools is often tracked using spreadsheets or paper records. This can make it difficult to identify available items, confirm who has borrowed an item, and monitor return deadlines.

This project aims to centralize those records and enforce consistent lending rules. The first version will be a **staff-managed backend API**; student accounts and a user interface are outside the initial scope.

## Planned Features

- **Student management:** Register, view, and update student records.
- **Equipment management:** Add, view, update, and, when permitted, delete equipment records.
- **Availability checking:** Identify equipment available for borrowing.
- **Loan management:** Record loans with borrowing and due dates.
- **Return management:** Record returns and make equipment available again.
- **Overdue tracking:** Find active loans past their due dates.
- **Loan history:** View loans associated with a student or equipment item.

## Technology Stack

| Component | Technology |
| --- | --- |
| Language | Java |
| Framework | Spring Boot |
| API | REST / JSON |
| Persistence | Spring Data JPA, relational database |
| Build tool | Maven |
| Validation | Jakarta Bean Validation |
| Testing | JUnit, Spring Boot Test |
| API testing / documentation | Postman or OpenAPI |

The application will follow a layered design: **controllers** handle HTTP requests, **services** enforce business rules, and **repositories** manage database access.

## Data Model

| Entity | Main Fields | Description |
| --- | --- | --- |
| **Student** | ID, name, university email | The registered borrower. |
| **Equipment** | ID, inventory code, name, category, description | One physical item with a unique inventory code. |
| **Loan** | ID, student, equipment, borrowing date, due date, return date | One borrowing transaction and its return. |

A student can have many loans. An equipment item can have many loans over time, but **only one active loan at a time**. A loan is considered active while its return date is empty; equipment availability is derived from this condition.

## Business Rules

- Only registered students can borrow equipment.
- Equipment cannot be borrowed if it already has an active loan.
- The due date must be later than the borrowing date.
- A loan can be returned only once, and its return date cannot precede the borrowing date.
- An active loan is overdue when its due date has passed.
- Students and equipment referenced by existing loans cannot be deleted, preserving loan history.
- Concurrent borrowing requests must not create more than one active loan for the same item.

## Planned API Endpoints

All endpoints use the `/api` prefix and exchange JSON data.

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/students` | Register a student. |
| `GET` | `/api/students/{id}` | Get a student by ID. |
| `PUT` | `/api/students/{id}` | Update a student. |
| `POST` | `/api/equipment` | Add equipment. |
| `GET` | `/api/equipment` | List equipment. |
| `GET` | `/api/equipment?available=true` | List available equipment. |
| `PUT` | `/api/equipment/{id}` | Update equipment details. |
| `DELETE` | `/api/equipment/{id}` | Delete equipment if allowed. |
| `POST` | `/api/loans` | Create a loan. |
| `GET` | `/api/loans` | List loans; optionally filter by student or equipment. |
| `POST` | `/api/loans/{id}/return` | Record an equipment return. |
| `GET` | `/api/loans/overdue` | List overdue active loans. |

Endpoints may be refined during implementation.

## Example Workflow

1. A staff member registers a student and adds a camera to the inventory.
2. The student requests the camera, and staff check that it is available.
3. Staff create a loan with a borrowing date and due date.
4. The camera becomes unavailable until it is returned.
5. Staff record the return, making the camera available again.
6. The completed loan remains accessible in the loan history.

If the due date passes without a recorded return, the loan appears in the overdue list.

## Testing and Deliverables

Core tests will cover successful borrowing and returns, unavailable equipment, duplicate returns, invalid dates, overdue detection, and simultaneous requests for the same item.

The planned deliverables are the Spring Boot source code, relational data model, documented API with sample requests, automated tests, and a demonstration of the lending workflow.
