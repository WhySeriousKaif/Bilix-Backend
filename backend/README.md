# 🏪 Molla POS System - Backend API

A comprehensive **Point of Sale (POS) System** backend built with **Spring Boot 3.5.9** and **PostgreSQL**. This system supports multi-store, multi-branch retail operations with role-based access control, real-time inventory management, order processing, analytics, and more.

---

## 📋 Table of Contents

- [Project Overview](#project-overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Setup & Installation](#setup--installation)
- [API Documentation (Swagger)](#api-documentation-swagger)
- [Authentication & Authorization](#authentication--authorization)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [Database Schema](#database-schema)
- [Configuration](#configuration)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)

---

## 🎯 Project Overview

**Molla POS System** is a production-ready backend API for managing retail operations across multiple stores and branches. It provides:

- **Multi-tenant architecture** supporting multiple stores and branches
- **Role-based access control** with 4 user roles (Super Admin, Store Admin, Branch Manager, Cashier)
- **Complete inventory management** with real-time stock tracking
- **Order processing** with payment integration (Razorpay & Stripe)
- **Analytics & reporting** with shift reports and sales analytics
- **RESTful API** with comprehensive documentation via Swagger UI

---

## ✨ Features

### 🔐 Authentication & Security
- JWT-based authentication with 24-hour token expiry
- Role-based access control (RBAC)
- Password encryption using BCrypt
- API rate limiting to prevent abuse
- CORS configuration for frontend integration

### 👥 User Management
- User registration and login
- Profile management
- Role assignment (Super Admin, Store Admin, Branch Manager, Cashier)
- Employee management per store/branch

### 🏬 Store & Branch Management
- Create and manage multiple stores
- Branch management per store
- Store moderation by Super Admin
- Branch-specific settings (working days, hours, contact info)

### 📦 Product & Inventory
- Product catalog with categories
- SKU-based product management
- Real-time inventory tracking per branch
- Stock alerts and management
- Product search and filtering

### 🛒 Orders & Payments
- Order creation and management
- Multiple payment methods (Cash, Card, UPI, Razorpay, Stripe)
- Order status tracking
- Refund processing
- Customer management

### 📊 Analytics & Reporting
- Shift reports for cashiers
- Sales analytics per branch
- Top-selling products
- Payment summaries
- Order history with pagination

### 🔧 Additional Features
- File upload support
- Email notifications (welcome emails)
- Caching for performance optimization
- Global exception handling
- Input validation with Jakarta Validation
- Comprehensive API documentation (Swagger)

---

## 🛠️ Technology Stack

### Core Framework
- **Spring Boot** 3.5.9
- **Java** 17
- **Maven** (Build Tool)

### Database & ORM
- **MySQL** 8.0+
- **Spring Data JPA**
- **Hibernate** (JPA Implementation)

### Security
- **Spring Security** 6.5.7
- **JWT** (JSON Web Tokens) - jjwt 0.11.5
- **BCrypt** Password Encoding

### Additional Libraries
- **Lombok** - Reduces boilerplate code
- **Jakarta Validation** - Input validation
- **Spring Mail** - Email notifications
- **Spring Cache** - Caching support
- **Springdoc OpenAPI** 2.6.0 - Swagger/OpenAPI documentation
- **Razorpay Java SDK** 1.4.8 - Payment gateway
- **Stripe Java SDK** 31.2.0 - Payment gateway

---

## 📦 Prerequisites

Before running the application, ensure you have:

- **Java 17** or higher
- **Maven 3.6+**
- **MySQL 8.0+** (or compatible database)
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code)
- **Postman** or similar API testing tool (optional)

---

## 🚀 Setup & Installation

### Step 1: Clone the Repository
```bash
git clone https://github.com/WhySeriousKaif/pos-point-of-Scale.git
cd molla-pos-system
```

### Step 2: Database Setup

1. **Create MySQL Database:**
```sql
CREATE DATABASE pos_db;
```

2. **Update Database Credentials** in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/pos_db
spring.datasource.username=root
spring.datasource.password=your_password
```

### Step 3: Configure JWT Secret

Update the JWT secret in `application.properties`:
```properties
jwt.secret=your-secret-key-here
```

### Step 4: Build the Project
```bash
mvn clean install
```

### Step 5: Run the Application
```bash
mvn spring-boot:run
```

Or run the main class: `com.molla.MollaPosSystemApplication`

### Step 6: Verify Application is Running

The application will start on **http://localhost:5001**

You should see:
```
Started MollaPosSystemApplication in X.XXX seconds
```

---

## 📚 API Documentation (Swagger)

### Swagger UI URL

Once the application is running, access the interactive API documentation at:

**🔗 Swagger UI:** `http://localhost:5001/swagger-ui/index.html`

**🔗 OpenAPI JSON:** `http://localhost:5001/v3/api-docs`

### How to Use Swagger UI

1. **Open Swagger UI** in your browser: `http://localhost:5001/swagger-ui/index.html`
2. **Test Public Endpoints:**
   - Try `/auth/login` or `/auth/register` to get a JWT token
3. **Authorize:**
   - Click the **🔒 Authorize** button at the top right
   - Paste your JWT token (format: `Bearer <your-token>` or just `<your-token>`)
   - Click **Authorize**
4. **Test Protected Endpoints:**
   - Now you can test all protected endpoints directly from Swagger UI
   - See request/response schemas, try different parameters, and view examples

### Swagger Features

- ✅ **Interactive API Testing** - Test endpoints without Postman
- ✅ **Auto-generated Documentation** - All endpoints with schemas
- ✅ **JWT Authentication Support** - Easy token-based testing
- ✅ **Request/Response Examples** - See data structures
- ✅ **Organized by Controllers** - Easy navigation

---

## 🔐 Authentication & Authorization

### User Roles

The system supports **4 user roles** with different access levels:

1. **ROLE_ADMIN (Super Admin)**
   - Full system access
   - Can view all stores
   - Can moderate stores
   - Can delete any store
   - Access to `/api/super-admin/**` endpoints

2. **ROLE_STORE_ADMIN (Store Admin)**
   - Manages their own store
   - Can create branches
   - Can manage employees
   - Can view store analytics

3. **ROLE_BRANCH_MANAGER (Branch Manager)**
   - Manages their assigned branch
   - Can manage branch inventory
   - Can view branch orders
   - Can manage branch employees

4. **ROLE_BRANCH_CASHIER (Cashier)**
   - Creates orders
   - Processes payments
   - Manages customers
   - Views own shift reports

### Authentication Flow

1. **Registration/Login:**
   ```
   POST /auth/register
   POST /auth/login
   ```
   - Returns JWT token in response
   - Token expires after 24 hours

2. **Using JWT Token:**
   - Include token in request header:
   ```
   Authorization: Bearer <your-jwt-token>
   ```

3. **Protected Endpoints:**
   - Most `/api/**` endpoints require authentication
   - Public endpoints: `/auth/**`, `/api/products/public/**`, `/api/payments/**`

### Security Configuration

- **Stateless Sessions:** No server-side session storage (JWT handles state)
- **Password Encryption:** BCrypt hashing (one-way encryption)
- **Rate Limiting:** IP-based rate limiting (stricter for login endpoints)
- **CORS:** Configured for frontend integration

---

## 📁 Project Structure

```
molla-pos-system/
├── src/
│   ├── main/
│   │   ├── java/com/molla/
│   │   │   ├── configuration/          # Security, JWT, Swagger, Cache configs
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtFilter.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   ├── RateLimitingFilter.java
│   │   │   │   └── ...
│   │   │   ├── controllers/            # REST API Controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── StoreController.java
│   │   │   │   ├── BranchController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   └── ...
│   │   │   ├── service/                 # Business Logic Layer
│   │   │   │   ├── impl/
│   │   │   │   │   ├── UserServiceImp.java
│   │   │   │   │   ├── ProductServiceImp.java
│   │   │   │   │   └── ...
│   │   │   │   └── ...
│   │   │   ├── repository/              # Data Access Layer
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── ProductRepository.java
│   │   │   │   └── ...
│   │   │   ├── model/                   # JPA Entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Store.java
│   │   │   │   ├── Product.java
│   │   │   │   └── ...
│   │   │   ├── payload/                 # DTOs and Request/Response Objects
│   │   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── exceptions/               # Custom Exceptions
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── NotFoundException.java
│   │   │   │   └── ...
│   │   │   ├── util/                    # Utility Classes
│   │   │   │   └── JwtUtil.java
│   │   │   └── MollaPosSystemApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── ...
│   └── test/
└── pom.xml
```

### Architecture Pattern

**Layered Architecture:**
- **Controllers** → Handle HTTP requests/responses (no business logic)
- **Services** → Business logic and orchestration
- **Repositories** → Data access (Spring Data JPA)
- **Models** → JPA entities (database tables)
- **DTOs** → Data transfer objects (API contracts)

---

## 🌐 API Endpoints

### Authentication Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/auth/register` | Register new user | Public |
| POST | `/auth/login` | Login user | Public |

### User Management

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/users/profile` | Get current user profile | Authenticated |
| GET | `/api/users/{id}` | Get user by ID | Authenticated |
| GET | `/api/users/all` | Get all users | Super Admin |

### Store Management

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/stores` | Create store | Store Admin |
| GET | `/api/stores` | Get all stores | Super Admin |
| GET | `/api/stores/{id}` | Get store by ID | Authenticated |
| PUT | `/api/stores/{id}` | Update store | Store Admin |
| DELETE | `/api/stores/{id}` | Delete store | Super Admin |
| PUT | `/api/stores/{id}/moderate` | Moderate store | Super Admin |
| GET | `/api/stores/my-store` | Get my store | Store Admin |
| GET | `/api/stores/my-store/employees` | Get store employees | Store Admin |

### Branch Management

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/branches` | Create branch | Store Admin |
| GET | `/api/branches` | Get all branches | Authenticated |
| GET | `/api/branches/{id}` | Get branch by ID | Authenticated |
| GET | `/api/branches/store/{storeId}` | Get branches by store | Authenticated |
| PUT | `/api/branches/{id}` | Update branch | Store Admin |
| DELETE | `/api/branches/{id}` | Delete branch | Store Admin |

### Product Management

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/products` | Create product | Store Admin/Manager |
| GET | `/api/products` | Get all products | Authenticated |
| GET | `/api/products/{id}` | Get product by ID | Authenticated |
| GET | `/api/products/store/{storeId}` | Get products by store | Authenticated |
| GET | `/api/products/public` | Get public products | Public |
| PUT | `/api/products/{id}` | Update product | Store Admin/Manager |
| DELETE | `/api/products/{id}` | Delete product | Store Admin |

### Category Management

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/categories` | Create category | Store Admin |
| GET | `/api/categories` | Get all categories | Authenticated |
| GET | `/api/categories/{id}` | Get category by ID | Authenticated |
| GET | `/api/categories/store/{storeId}` | Get categories by store | Authenticated |
| PUT | `/api/categories/{id}` | Update category | Store Admin |
| DELETE | `/api/categories/{id}` | Delete category | Store Admin |

### Inventory Management

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/inventories` | Create inventory | Store Admin/Manager |
| GET | `/api/inventories` | Get all inventories | Authenticated |
| GET | `/api/inventories/{id}` | Get inventory by ID | Authenticated |
| GET | `/api/inventories/branch/{branchId}` | Get inventories by branch | Authenticated |
| PUT | `/api/inventories/{id}` | Update inventory | Store Admin/Manager |
| DELETE | `/api/inventories/{id}` | Delete inventory | Store Admin |

### Order Management

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/orders` | Create order | Cashier |
| GET | `/api/orders/{id}` | Get order by ID | Authenticated |
| GET | `/api/orders/branch/{branchId}` | Get orders by branch | Authenticated |
| GET | `/api/orders/branch/{branchId}/paged` | Get orders (paginated) | Authenticated |
| PUT | `/api/orders/{id}` | Update order | Cashier/Manager |
| DELETE | `/api/orders/{id}` | Delete order | Manager |

### Customer Management

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/customers` | Create customer | Cashier |
| GET | `/api/customers` | Get all customers | Authenticated |
| GET | `/api/customers/{id}` | Get customer by ID | Authenticated |
| GET | `/api/customers/search` | Search customers | Authenticated |
| PUT | `/api/customers/{id}` | Update customer | Cashier |
| DELETE | `/api/customers/{id}` | Delete customer | Manager |

### Employee Management

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/employees` | Create employee | Store Admin |
| GET | `/api/employees` | Get all employees | Authenticated |
| GET | `/api/employees/{id}` | Get employee by ID | Authenticated |
| GET | `/api/employees/store/{storeId}` | Get employees by store | Authenticated |
| GET | `/api/employees/branch/{branchId}` | Get employees by branch | Authenticated |
| PUT | `/api/employees/{id}` | Update employee | Store Admin |
| DELETE | `/api/employees/{id}` | Delete employee | Store Admin |

### Shift Reports & Analytics

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/shift-reports/start` | Start shift | Cashier |
| PATCH | `/api/shift-reports/end` | End shift | Cashier |
| GET | `/api/shift-reports/current` | Get current shift | Cashier |
| GET | `/api/shift-reports/cashier/{cashierId}` | Get cashier reports | Authenticated |
| GET | `/api/shift-reports/branch/{branchId}` | Get branch reports | Authenticated |
| GET | `/api/shift-reports` | Get all reports | Authenticated |

### Refund Management

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/refunds` | Create refund | Cashier/Manager |
| GET | `/api/refunds` | Get all refunds | Authenticated |
| GET | `/api/refunds/{id}` | Get refund by ID | Authenticated |
| GET | `/api/refunds/order/{orderId}` | Get refunds by order | Authenticated |

### Payment Integration

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/payments/create-order` | Create Razorpay order | Public |
| POST | `/api/payment/create-payment-intent` | Create Stripe payment | Public |

### File Upload

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/files/upload` | Upload file | Authenticated |

---

## 🗄️ Database Schema

### Core Entities

- **User** - Stores user information and roles
- **Store** - Multi-tenant store information
- **Branch** - Branches belonging to stores
- **Category** - Product categories
- **Product** - Product catalog
- **Inventory** - Stock levels per branch
- **Customer** - Customer information
- **Order** - Order transactions
- **OrderItem** - Order line items
- **Refund** - Refund records
- **ShiftReport** - Analytics and reporting

### Key Relationships

- **Store** → **Branch** (One-to-Many)
- **Store** → **User** (Store Admin)
- **Branch** → **User** (Branch Manager, Cashiers)
- **Product** → **Category** (Many-to-One)
- **Product** → **Store** (Many-to-One)
- **Inventory** → **Product** + **Branch** (Many-to-One each)
- **Order** → **Branch** + **Customer** + **User** (Cashier)
- **OrderItem** → **Order** + **Product** (Many-to-One each)

---

## ⚙️ Configuration

### Application Properties

Key configuration in `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=5001

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/pos_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect

# JWT Configuration
jwt.secret=your-secret-key-here

# Payment Gateway (Razorpay)
razorpay.api.key=your-razorpay-key
razorpay.api.secret=your-razorpay-secret

# Payment Gateway (Stripe)
stripe.public.key=your-stripe-public-key
stripe.secret.key=your-stripe-secret-key
```

### Environment Variables

For production deployment, use environment variables:

- `SPRING_DATASOURCE_URL` - Database connection URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `JWT_SECRET` - JWT signing secret
- `PORT` - Server port (default: 5001)
- `ALLOWED_ORIGINS` - CORS allowed origins (comma-separated)

---

## 🧪 Testing

### Manual Testing with Postman

1. **Import Postman Collection:**
   - Use the provided Postman collection or create requests manually
   - Base URL: `http://localhost:5001`

2. **Test Flow:**
   - Register/Login to get JWT token
   - Use token in `Authorization: Bearer <token>` header
   - Test protected endpoints

### Testing with Swagger UI

1. Open Swagger UI: `http://localhost:5001/swagger-ui/index.html`
2. Test endpoints interactively
3. Use "Authorize" button to add JWT token

### Sample Test Data

**Register Super Admin:**
```json
POST /auth/register
{
  "fullName": "Super Admin",
  "email": "admin@molla.com",
  "password": "admin123",
  "phone": "1234567890",
  "role": "ROLE_STORE_ADMIN"
}
```

**Login:**
```json
POST /auth/login
{
  "email": "admin@molla.com",
  "password": "admin123"
}
```

---

## 🚀 Deployment

### Local Deployment

1. Ensure MySQL is running
2. Create database: `CREATE DATABASE pos_db;`
3. Update `application.properties` with database credentials
4. Run: `mvn spring-boot:run`

### Production Deployment

#### Option 1: Railway
- Configure environment variables
- Deploy using Railway CLI or GitHub integration

#### Option 2: Render
- Use `render.yaml` configuration
- Set environment variables in dashboard

#### Option 3: Fly.io
- Use `fly.toml` configuration
- Deploy using Fly CLI

### Production Checklist

- [ ] Update JWT secret (use strong, random secret)
- [ ] Configure production database
- [ ] Set up environment variables
- [ ] Configure CORS for frontend domain
- [ ] Enable HTTPS
- [ ] Set up monitoring and logging
- [ ] Configure email service (if needed)

---

## 📝 Key Features Explained

### 1. JWT Authentication
- Stateless authentication using JSON Web Tokens
- 24-hour token expiry
- Token includes user email and role
- Validated on every protected request

### 2. Role-Based Access Control (RBAC)
- 4 distinct user roles with different permissions
- Security rules enforced at controller and service levels
- Super Admin has full system access

### 3. Caching
- Spring Cache enabled for read-heavy endpoints
- Cache names: `productsByStore`, `categoriesByStore`, etc.
- Improves performance for frequently accessed data

### 4. Rate Limiting
- IP-based rate limiting
- Stricter limits for login endpoints (prevents brute force)
- Returns HTTP 429 when limit exceeded

### 5. Global Exception Handling
- Consistent error response format
- Standard exceptions: `NotFoundException`, `BadRequestException`
- All errors return JSON with timestamp, error type, message, status

### 6. Input Validation
- Jakarta Validation annotations on DTOs
- `@Valid` annotation in controllers
- Automatic validation error handling

### 7. Pagination & Sorting
- Implemented for orders endpoint
- Supports page, size, sortBy, direction parameters
- Returns Spring `Page` object with metadata

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License.

---

## 👨‍💻 Author

**MD Kaif Molla**
**https://github.com/WhySeriousKaif**

---

## 📞 Support

For support, email kaif00786001@gmail.com or open an issue in the repository.

---

## 🎓 For Teachers/Evaluators

### Project Highlights

1. **Complete RESTful API** - All CRUD operations for core entities
2. **Security Implementation** - JWT authentication, RBAC, password encryption
3. **Database Design** - Normalized schema with proper relationships
4. **Code Quality** - Layered architecture, separation of concerns
5. **Documentation** - Comprehensive Swagger/OpenAPI documentation
6. **Error Handling** - Global exception handler with consistent responses
7. **Performance** - Caching, pagination, optimized queries
8. **External Integrations** - Payment gateways (Razorpay, Stripe), Email (SMTP)
9. **Analytics** - Shift reports with sales analytics
10. **Best Practices** - Validation, security, clean code principles

### How to Evaluate

1. **Start the Application:**
   ```bash
   mvn spring-boot:run
   ```

2. **Access Swagger UI:**
   - Open: `http://localhost:5001/swagger-ui/index.html`
   - Review all endpoints and schemas

3. **Test Authentication:**
   - Register a user
   - Login to get JWT token
   - Test protected endpoints

4. **Review Code Structure:**
   - Check layered architecture
   - Review security configuration
   - Examine exception handling
   - Validate business logic separation

5. **Database Verification:**
   - Check entity relationships
   - Verify JPA annotations
   - Review query optimization

### Questions to Consider

- How is security implemented? (JWT, RBAC, password encryption)
- How are errors handled? (Global exception handler)
- What design patterns are used? (Layered architecture, DTO pattern)
- How is performance optimized? (Caching, pagination)
- How are external services integrated? (Payment gateways, email)

---

**Thank you for reviewing this project!** 🎉

For detailed API testing, please refer to the Swagger UI at: **http://localhost:5001/swagger-ui/index.html**
