# 🪵 WoodExcess API

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![CI/CD](https://github.com/JoaoZabarella/woodexcess-api/actions/workflows/ci.yml/badge.svg)](https://github.com/JoaoZabarella/woodexcess-api/actions)

A **micro SaaS platform** that connects woodworkers and store owners, enabling them to list, manage, and trade surplus materials. Built as a **professional freelance project** to reduce waste, optimize inventory, and promote sustainable practices within the woodworking industry.

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [API Endpoints](#-api-endpoints)
- [Real-Time Chat System](#-real-time-chat-system)
- [Testing](#-testing)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Environment Variables](#-environment-variables)
- [Database Migrations](#-database-migrations)
- [Professional Experience](#-professional-experience)

---

## ✨ Features

### **Authentication & Authorization**
- ✅ User registration with email validation
- ✅ JWT-based authentication (access + refresh tokens)
- ✅ Role-based access control (USER, ADMIN)
- ✅ Refresh token rotation with security context validation
- ✅ Login rate limiting (protection against brute-force attacks)

### **User Management**
- ✅ Complete user profile management
- ✅ Password strength validation
- ✅ Phone number validation (Brazilian format)
- ✅ User activation/deactivation (soft delete)

### **Address Management**
- ✅ Multiple addresses per user (up to 5)
- ✅ Automatic address filling via ViaCEP API
- ✅ Primary address designation
- ✅ Address validation (no duplicates)
- ✅ Soft delete for addresses

### **Material Listings**
- ✅ Create, read, update, and deactivate material listings
- ✅ **Image Upload**: Upload, manage, and reorder listing images (AWS S3 integration)
- ✅ Advanced filtering (material type, location, price range, condition)
- ✅ Pagination and sorting
- ✅ Owner/Admin authorization
- ✅ Public listing browsing (GET endpoints)
- ✅ Denormalized location fields for fast queries

### **Real-Time Chat System** 🆕
- ✅ WebSocket-based real-time messaging (STOMP protocol)
- ✅ Context-aware chat (linked to specific listings)
- ✅ Message history with pagination
- ✅ Unread message indicators
- ✅ JWT authentication for WebSocket connections
- ✅ Rate limiting for message sending
- ✅ Soft delete for messages (audit trail)
- ✅ Recent conversations list

### **Coming Soon**
- 🔄 Push notifications for new messages
- 🔄 Negotiation system with offer/counter-offer
- 🔄 User dashboard with analytics
- 🔄 Email notifications

---

## 🛠 Tech Stack

### **Core**
- **Java 21** (LTS)
- **Spring Boot 3.3.6**
- **Maven** (dependency management)

### **Frameworks & Libraries**
- **Spring Web** - REST API
- **Spring WebSocket** - Real-time communication
- **Spring Data JPA** - ORM with Hibernate
- **Spring Security** - Authentication & Authorization
- **Spring Validation** - Bean validation
- **Spring Cloud OpenFeign** - HTTP client for ViaCEP integration
- **Spring Cache** - Caching support
- **SpringDoc OpenAPI** - Swagger UI & API Documentation

### **Database**
- **PostgreSQL 16** (production)
- **H2** (testing with Testcontainers)
- **Flyway** - Database migrations

### **Security & Auth**
- **JWT** (JSON Web Tokens)
- **BCrypt** - Password hashing
- **Bucket4j** - Rate limiting (Login, Messages, WebSocket)

### **Real-Time Communication**
- **STOMP** - Simple Text Oriented Messaging Protocol
- **SockJS** - WebSocket fallback for legacy browsers

### **Cloud & Storage**
- **AWS S3** - Image storage
- **Thumbnailator** - Image processing & resizing

### **Utilities**
- **Lombok** - Reduce boilerplate code
- **MapStruct** - DTO mapping
- **Jackson** - JSON serialization

### **Testing**
- **JUnit 5** - Unit testing
- **Mockito** - Mocking framework
- **Spring Boot Test** - Integration testing
- **Testcontainers** - PostgreSQL containers for tests
- **MockMvc** - REST API testing

### **DevOps & Quality**
- **GitHub Actions** - CI/CD pipeline
- **Qodana** - Code quality analysis by JetBrains
- **JaCoCo** - Code coverage (85%+)
- **Docker** - Containerization

---
## 🚀 Getting Started

### **Prerequisites**
- Java 21+ ([Download](https://www.oracle.com/java/technologies/downloads/))
- Maven 3.9+ ([Download](https://maven.apache.org/download.cgi))
- Docker Desktop (for Testcontainers) ([Download](https://www.docker.com/products/docker-desktop))
- PostgreSQL 16+ (production) ([Download](https://www.postgresql.org/download/))

### **1. Clone the repository**
git clone https://github.com/JoaoZabarella/woodexcess-api.git
cd woodexcess-api
### **2. Configure database**

**Option A: Docker (recommended for development)**
docker run -d
--name woodexcess-postgres
-e POSTGRES_DB=woodexcess
-e POSTGRES_USER=postgres
-e POSTGRES_PASSWORD=postgres
-p 5432:5432
postgres:16-alpine
**Option B: Local PostgreSQL**
CREATE DATABASE woodexcess;
### **3. Configure environment variables**

Create a `.env` file or set environment variables:
Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/woodexcess
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

JWT
JWT_SECRET=your-secret-key-min-256-bits-long-replace-this-in-production
JWT_ACCESS_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000

ViaCEP
VIACEP_URL=https://viacep.com.br/ws

AWS S3 (For Image Upload)
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_S3_BUCKET=woodexcess-listings
AWS_REGION=us-east-1

WebSocket
WEBSOCKET_ALLOWED_ORIGINS=http://localhost:3000,https://yourdomain.com

Rate Limiting
APP_RATE_LIMIT_LOGIN_CAPACITY=5
APP_RATE_LIMIT_LOGIN_REFILL_TOKENS=5
APP_RATE_LIMIT_LOGIN_REFILL_MINUTES=15
APP_RATE_LIMIT_MESSAGE_CAPACITY=20
APP_RATE_LIMIT_MESSAGE_REFILL_TOKENS=10
APP_RATE_LIMIT_MESSAGE_REFILL_MINUTES=1
APP_RATE_LIMIT_WEBSOCKET_CAPACITY=30
APP_RATE_LIMIT_WEBSOCKET_REFILL_TOKENS=15
APP_RATE_LIMIT_WEBSOCKET_REFILL_MINUTES=1

### **4. Run the application**

**Using Maven:**
**Using Docker:**
docker build -t woodexcess-api .
docker run -p 8080:8080 woodexcess-api
### **5. Access the API**
- **Base URL:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **WebSocket Endpoint:** `ws://localhost:8080/ws`
- **Health Check:** `http://localhost:8080/actuator/health`

---

## 📍 API Endpoints

### **Authentication**

#### Register
`POST /api/users/register`
{
"name": "John Doe",
"email": "john@example.com",
"phone": "11987654321",
"password": "StrongPass123!@#"
}
#### Login
`POST /api/auth/login`
{
"email": "john@example.com",
"password": "StrongPass123!@#"
}
**Response:**
{
"accessToken": "eyJhbGciOiJIUzUxMiJ9...",
"refreshToken": "550e8400-e29b-41d4-a716-446655440000",
"type": "Bearer",
"expiresIn": 900000
}

#### Refresh Token
`POST /api/auth/refresh`

---

### **Address Management**

- `POST /api/addresses` - Create Address (Manual)
- `POST /api/addresses/from-cep` - Create Address (via ViaCEP)
- `GET /api/addresses` - List User Addresses
- `GET /api/addresses/primary` - Get Primary Address
- `PUT /api/addresses/{id}` - Update Address
- `PATCH /api/addresses/{id}/set-primary` - Set as Primary
- `DELETE /api/addresses/{id}` - Delete Address (soft delete)

---

### **Material Listings**

#### Create Listing
`POST /api/listings`
{
"title": "Sobra de Madeira de Lei - Ipê",
"description": "Tábuas de ipê em excelente estado",
"materialType": "WOOD",
"price": 150.50,
"quantity": 10,
"condition": "USED",
"addressId": "address-uuid"
}

#### List Listings (with filters)
`GET /api/listings?materialType=WOOD&city=São Paulo&minPrice=100&maxPrice=200&page=0&size=10`

**Query Parameters:**
- `materialType` - WOOD, MDF, PLYWOOD, VENEER, PARTICLE_BOARD, OTHER
- `condition` - NEW, USED, SCRAP
- `city` - Filter by city
- `state` - Filter by state (SP, RJ, etc.)
- `minPrice` / `maxPrice` - Price range
- `page` / `size` - Pagination
- `sort` - Sorting (e.g., `createdAt,desc`)

#### Listing Images
- `POST /api/listings/{listingId}/images` - Upload image (Multipart)
- `GET /api/listings/{listingId}/images` - Get all images
- `PUT /api/listings/{listingId}/images/{imageId}/primary` - Set image as primary
- `PUT /api/listings/{listingId}/images/reorder` - Reorder images
- `DELETE /api/listings/{listingId}/images/{imageId}` - Delete image

---

## 💬 Real-Time Chat System

### **Overview**

The chat system enables real-time communication between buyers and sellers within the context of a specific listing. Built with WebSocket (STOMP protocol) for instant messaging and REST API for message history.

### **Key Features**

- ✅ **Real-time messaging** via WebSocket
- ✅ **Context-aware**: Messages linked to specific listings
- ✅ **JWT authentication** for WebSocket connections
- ✅ **Message history** with pagination
- ✅ **Unread indicators** and conversation management
- ✅ **Rate limiting** to prevent spam
- ✅ **Soft delete** for audit compliance
- ✅ **Optimized queries** with EntityGraph (no N+1)

---

### **REST API Endpoints**

#### Send Message (HTTP)
`POST /api/messages`
{
"recipientId": "recipient-user-uuid",
"listingId": "listing-uuid",
"content": "Olá, ainda tem esse material disponível?"
}
**Response:**
{
"id": "message-uuid",
"senderId": "sender-uuid",
"senderEmail": "sender@example.com",
"senderName": "João Silva",
"recipientId": "recipient-uuid",
"recipientEmail": "recipient@example.com",
"recipientName": "Maria Santos",
"listingId": "listing-uuid",
"listingTitle": "Sobra de Madeira de Lei - Ipê",
"content": "Olá, ainda tem esse material disponível?",
"isRead": false,
"createdAt": "2025-12-12T22:30:00",
"updatedAt": "2025-12-12T22:30:00"
}

#### Get Conversation History
`GET /api/messages/conversation?user={otherUserId}&listing={listingId}`

Returns all messages between current user and another user about a specific listing.

#### Get Messages by Listing
`GET /api/messages/listing/{listingId}?page=0&size=20`

Returns paginated messages for a listing where the user is sender or recipient.

#### Get Recent Conversations
`GET /api/messages/conversations`

Returns list of recent conversations with last message preview and unread count.

**Response:**

[
{
"otherUserId": "user-uuid",
"otherUserName": "Maria Santos",
"otherUserEmail": "maria@example.com",
"listingId": "listing-uuid",
"listingTitle": "Sobra de Madeira de Lei - Ipê",
"lastMessage": "Combinado, te passo o endereço!",
"lastMessageTime": "2025-12-12T22:35:00",
"unreadCount": 2,
"isSender": false
}
]

#### Get Unread Count
`GET /api/messages/unread-count`

Returns total number of unread messages for the authenticated user.

#### Mark Conversation as Read
`PATCH /api/messages/mark-read?sender={senderId}&listing={listingId}`

Marks all messages from a specific sender about a listing as read.

#### Delete Message (Soft Delete)
`DELETE /api/messages/{messageId}`

Soft deletes a message. Only the sender can delete their own messages.

#### Get Deleted Messages (Admin - Audit)
`GET /api/messages/audit/deleted?page=0&size=20`

Returns soft-deleted messages for audit purposes (requires ADMIN role).

---

### **WebSocket Integration**

#### **1. Connection**

Connect to the WebSocket endpoint with JWT authentication:

**Endpoint:** `ws://localhost:8080/ws`

**JavaScript Example (SockJS + STOMP):**
mport SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const token = localStorage.getItem('accessToken'); // JWT token

const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

const headers = {
'Authorization': Bearer ${token}
};

stompClient.connect(headers,
(frame) => {
console.log('Connected: ' + frame);
// Subscribe to receive private messages
stompClient.subscribe('/user/queue/messages', (message) => {
const received = JSON.parse(message.body);
console.log('New message:', received);
// Update UI with new message
});
},
(error) => {
console.error('WebSocket error:', error);
}
);

#### **2. Subscribe to Messages**

After connecting, subscribe to your private message queue:

**Destination:** `/user/queue/messages`

All messages sent to you will arrive in this queue.

#### **3. Send Message**

Send a message via WebSocket:

**Destination:** `/app/chat.send`

**Payload:**
{
"recipientId": "recipient-user-uuid",
"listingId": "listing-uuid",
"content": "Mensagem em tempo real!"
}
**JavaScript Example:**
function sendMessage(recipientId, listingId, content) {
if (stompClient && stompClient.connected) {
const message = {
recipientId: recipientId,
listingId: listingId,
content: content
};
stompClient.send('/app/chat.send', {}, JSON.stringify(message));
}
}


#### **4. Receive Messages**

Messages arrive automatically in the subscribed queue:

stompClient.subscribe('/user/queue/messages', (message) => {
const chatMessage = JSON.parse(message.body);

// chatMessage structure:
// {
// id: "message-uuid",
// senderId: "sender-uuid",
// senderEmail: "sender@example.com",
// recipientId: "recipient-uuid",
// listingId: "listing-uuid",
// content: "Message content",
// isRead: false,
// createdAt: "2025-12-12T22:30:00"
// }

displayMessage(chatMessage);
});
#### **5. Disconnect**

Properly disconnect when done:

if (stompClient) {
stompClient.disconnect(() => {
console.log('Disconnected');
});
}

---

### **Complete React Example**

import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

function ChatComponent({ listingId, recipientId, authToken }) {
const [messages, setMessages] = useState([]);
const [newMessage, setNewMessage] = useState('');
const [connected, setConnected] = useState(false);
const stompClientRef = useRef(null);

useEffect(() => {
// Connect to WebSocket
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);
stompClientRef.current = stompClient;
const headers = {
'Authorization': `Bearer ${authToken}`
};

stompClient.connect(headers,
() => {
console.log('WebSocket connected');
setConnected(true);

    // Subscribe to private messages
    stompClient.subscribe('/user/queue/messages', (message) => {
      const received = JSON.parse(message.body);
      setMessages(prev => [...prev, received]);
    });
},
(error) => {
console.error('Connection error:', error);
setConnected(false);
}
);

return () => {
if (stompClient) {
stompClient.disconnect();
}
};
}, [authToken]);

const sendMessage = () => {
if (!stompClientRef.current || !newMessage.trim()) return;

const payload = {
recipientId: recipientId,
listingId: listingId,
content: newMessage.trim()
};

stompClientRef.current.send(
'/app/chat.send',
{},
JSON.stringify(payload)
);

setNewMessage('');
};

return (
<div className="chat-container">
<div className="connection-status">
{connected ? '🟢 Connected' : '🔴 Disconnected'}
</div>

  <div className="messages">
    {messages.map(msg => (
      <div key={msg.id} className="message">
        <strong>{msg.senderName}:</strong> {msg.content}
        <span className="time">{msg.createdAt}</span>
      </div>
    ))}
  </div>

  <div className="message-input">
    <input
      type="text"
      value={newMessage}
      onChange={(e) => setNewMessage(e.target.value)}
      onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
      placeholder="Type a message..."
    />
    <button onClick={sendMessage}>Send</button>
  </div>
</div>
);
}

export default ChatComponent;
---

### **Rate Limiting**

The chat system implements three-layer rate limiting:

1. **Login:** 5 attempts per 15 minutes
2. **HTTP Messages:** 20 messages per minute
3. **WebSocket Messages:** 30 messages per minute

Rate limits are per user and automatically refill over time.

---

### **Security Features**

- ✅ JWT authentication required for WebSocket connections
- ✅ Users can only send messages to others (not to themselves)
- ✅ Users can only view conversations they're part of
- ✅ Users can only delete their own messages
- ✅ Messages are validated for content length and XSS prevention
- ✅ Soft delete maintains audit trail

---

### **Performance Optimizations**

- ✅ **EntityGraph** to prevent N+1 query problems
- ✅ **Database indexes** for fast conversation queries
- ✅ **Pagination** for large message histories
- ✅ **Lazy loading** with fetch strategies
- ✅ **Connection pooling** for database
- ✅ **Rate limiting** to prevent abuse

---

## 🧪 Testing

### **Run all tests**
mvn clean test
### **Run with coverage**
mvn clean test jacoco:report

View report: `target/site/jacoco/index.html`

### **Test structure**
- **Unit tests:** `src/test/java/service/` (MessageServiceTest, etc.)
- **Integration tests:** `src/test/java/integration/` (MessageControllerIntegrationTest, WebSocketIntegrationTest)
- **Test coverage:** **85%+**

### **Key Test Scenarios**
- ✅ Send message via REST API
- ✅ Send message via WebSocket
- ✅ Retrieve conversation history
- ✅ Mark messages as read
- ✅ Unread message count
- ✅ Rate limiting enforcement
- ✅ Soft delete functionality
- ✅ Authorization checks
- ✅ WebSocket authentication

---

## 🔄 CI/CD Pipeline

### **GitHub Actions Workflows**

#### **1. CI/CD Pipeline** (`ci.yml`)
- Runs on every push/PR to `main` and `develop`
- Java 21 + Maven
- Testcontainers with PostgreSQL
- JaCoCo coverage report (85%+ required)
- Artifact upload on failure

#### **2. Code Quality** (`qodana_code_quality.yml`)
- Qodana static analysis
- Detects bugs, vulnerabilities, code smells
- Posts comments on PRs

#### **3. Security Scan** (`security.yml`)
- Dependency vulnerability scanning
- SARIF report generation

---

## 🔐 Environment Variables

### **Required**

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | Database connection string | `jdbc:postgresql://localhost:5432/woodexcess` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | JWT signing key (min 256 bits) | `your-secret-key-here` |

### **Optional**

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_ACCESS_EXPIRATION_MS` | Access token expiration | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token expiration | `604800000` (7 days) |
| `VIACEP_URL` | ViaCEP API base URL | `https://viacep.com.br/ws` |
| `AWS_ACCESS_KEY_ID` | AWS Access Key | - |
| `AWS_SECRET_ACCESS_KEY` | AWS Secret Key | - |
| `AWS_S3_BUCKET` | S3 Bucket Name | `woodexcess-listings` |
| `AWS_REGION` | AWS Region | `us-east-1` |
| `WEBSOCKET_ALLOWED_ORIGINS` | CORS origins for WebSocket | `http://localhost:3000` |

---

## 🗄 Database Migrations

Migrations are managed by **Flyway** and located in `src/main/resources/db/migration/`.

### **Migration naming convention**
`V{version}__{description}.sql`

**Current migrations:**
- `V1__create_users_table.sql`
- `V2__create_refresh_tokens_table.sql`
- `V3__create_addresses_table.sql`
- `V4__create_material_listings_table.sql`
- `V5__create_listing_images_table.sql`
- `V6__create_messages_table.sql`
- `V7__add_soft_delete_to_messages.sql`

### **Run migrations manually**
mvn flyway:migrate

---

## 📊 Code Quality Metrics

- **Test Coverage:** 85%+
- **Code Quality:** Qodana scan passing
- **Security:** No critical vulnerabilities
- **Performance:** Rate limiting enabled on all critical endpoints
- **Maintainability:** Clean architecture with separation of concerns

---

## 💼 Professional Experience

This project was developed as a **professional freelance work** to demonstrate full-stack backend development capabilities for a SaaS platform.

### **Technical Highlights**

- ✅ **Enterprise-grade architecture** with Spring Boot best practices
- ✅ **Real-time communication** via WebSocket (STOMP)
- ✅ **Comprehensive security** (JWT, rate limiting, soft delete for audit)
- ✅ **Performance optimization** (EntityGraph, database indexes, caching)
- ✅ **High test coverage** (85%+) with unit and integration tests
- ✅ **CI/CD pipeline** with automated testing and code quality checks
- ✅ **Production-ready** with Docker, AWS S3, PostgreSQL
- ✅ **API documentation** with OpenAPI/Swagger
- ✅ **Clean code** with SOLID principles and design patterns

### **Skills Demonstrated**

- Backend Development (Java 21, Spring Boot 3.3.6)
- Real-time Systems (WebSocket, STOMP)
- Database Design (PostgreSQL, Flyway migrations)
- Security Engineering (JWT, BCrypt, Rate Limiting)
- Cloud Integration (AWS S3)
- Testing (JUnit, Mockito, Testcontainers)
- DevOps (Docker, GitHub Actions)
- API Design (REST, WebSocket)
- Code Quality (Qodana, JaCoCo)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Contact

**João Pedro Zabarella Muniz**  
*Full-Stack Backend Developer | Freelance*

- GitHub: [@JoaoZabarella](https://github.com/JoaoZabarella)
- Repository: [woodexcess-api](https://github.com/JoaoZabarella/woodexcess-api)

---

**Developed with ❤️ for sustainable woodworking practices.**  
*Professional freelance project showcasing enterprise-grade backend development.*
