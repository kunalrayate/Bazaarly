# 🛒 Bazaarly - Full-Stack E-Commerce Platform

> A modern, multi-vendor online marketplace inspired by platforms like Amazon, built with **Java, Spring Boot, Hibernate/JPA, MySQL and Angular**.

Bazaarly delivers a complete shopping experience: customers discover products, add them to a cart, pay, and track orders. Sellers run their own store dashboards, and administrators control the whole platform.

![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen) ![MySQL](https://img.shields.io/badge/MySQL-8-blue) ![Angular](https://img.shields.io/badge/Angular-17-red) ![JWT](https://img.shields.io/badge/Auth-JWT-purple)

---
## 📖 Overview
I built Bazaarly, a full-stack, multi-vendor e-commerce platform inspired by marketplaces like Amazon, to practice designing and building a real-world system instead of a simple CRUD app. E-commerce brings together many of the problems that production software has to solve: multiple user roles, secure authentication, payments, inventory, search, discounts and order workflows. The application supports three roles. Customers can browse, search, filter, order, pay and track deliveries. Sellers manage their own products, stock and orders. Administrators approve sellers, manage the catalog and coupons, and monitor sales analytics. I chose this project because it let me work on both the technical side (secure APIs, database design, concurrency) and the business side (order lifecycle, returns and refunds, coupon rules) in one application.

## 📄 What I learned

Building Bazaarly strengthened my skills across the full stack. On the backend, I designed layered REST APIs with Spring Boot, implemented JWT authentication with role-based authorization, and handled validation and exception handling. With Hibernate/JPA and MySQL, I modelled complex relationships, wrote dynamic search and filter queries with pagination, and used transactions and locking so stock is never oversold. I built a coupon engine, an order state machine, a scheduled job that ends expired deals, and a pluggable payment gateway that can be switched from a test gateway to Razorpay. On the frontend, I built an Angular single-page application using lazy-loaded routes, route guards, HTTP interceptors and role-based dashboards. I also practiced debugging real issues such as database connection errors and CORS problems, and learned to structure a large project cleanly and document it for other developers.

## ✨ Features and Operations 

1. Customer Operations :
	Customers can register and log in, browse products by category, search by keyword, filter by price, brand, rating and availability, and sort by price, rating, popularity or newest arrivals.
2. Cart, Wishlist and Coupons :
	Customers can add products (with variants such as size, color or storage) to the cart, change quantities, save items to the wishlist and apply discount coupons.
3. Checkout and Payment :
	Customers can manage multiple delivery addresses, place orders and pay online (test payment, Razorpay-ready) or use Cash on Delivery.
4. Order Tracking :
	Every order moves through the stages Order Placed, Confirmed, Processing, Shipped, Out for Delivery and Delivered. Customers can cancel eligible orders and request returns or refunds.
5. Reviews and Ratings :
	Customers who received a product can rate and review it. The product page shows the average rating, number of reviews and rating distribution.
6. Seller Operations :
	Sellers register their store (admin approval required), add and manage products with images and variants, set prices and discounts, update stock, process orders and view sales and revenue reports.
7. Admin Operations :
	Admins approve or reject sellers, manage customers, categories, products, orders, coupons and reported reviews, and view platform-wide statistics such as revenue, popular products and categories.
8. Inventory Management :
	Stock is automatically reduced when an order is placed and restored when an order is cancelled or returned. Products with low stock are highlighted for sellers and admins.
9. Personalized Recommendations :
	Product suggestions are based on recently viewed products, previous purchases, wishlist, category and frequently bought together items.


## Attributes
Each product record consists of the following attributes:
- **id**: Product ID (Unique Identifier)
- **name**: Product name
- **description**: Detailed product description
- **brand**: Brand name
- **category**: Product category (Electronics, Fashion, Grocery, Books, Furniture, etc.)
- **price**: Maximum retail price (MRP)
- **discountPercent**: Discount percentage
- **sellingPrice**: Final price after discount
- **stock**: Available quantity
- **images**: List of product images
- **specifications**: Key-value product specifications
- **variants**: Options such as size, color, storage with optional price add-ons
- **ratingAvg / ratingCount**: Average rating and number of reviews

Other main entities: **User** (Customer, Seller, Admin), **Address**, **CartItem**, **WishlistItem**, **Order**, **OrderItem**, **Coupon**, **Review**, **Notification**.


## 🧰 Technologies Used

- **Spring Boot** : Core framework used to build the REST API with the Model-View-Controller pattern. Handles request mapping, validation and exception handling.
- **Spring Security + JWT** : Stateless authentication and role-based authorization for the Customer, Seller and Admin roles. Passwords are stored using BCrypt.
- **Hibernate / Spring Data JPA** : Object-relational mapping used to perform CRUD operations, complex queries and dynamic search filters on the database.
- **MySQL** : Relational database used to store users, products, orders, coupons, reviews and notifications.
- **Angular** : Frontend single-page application (standalone components, lazy-loaded routes, route guards and HTTP interceptors) written in TypeScript.
- **HTML & CSS** : Used to create a responsive and visually appealing user interface.
- **Maven** : Build and dependency management tool for the backend.
- **Node.js & npm** : Used to install dependencies and run the Angular application.
- **Apache Tomcat (embedded)** : The embedded servlet container that runs the Spring Boot application.
- **Docker (optional)** : Used to run the MySQL database with a single command.


## To run this project on your system, follow these steps 

 1. Prerequisites :
  - JDK 17 or later
  - Maven 3.8 or later
  - MySQL 8 (or Docker Desktop)
  - Node.js 18 or later

 2. Clone the repository :
  - git clone https://github.com/kunalrayate/bazaarly.git
  - cd bazaarly

 3. Start the database :
  - Install MySQL and keep the username as root and password as root, or run: docker compose up -d
  - The database named bazaarly and all tables are created automatically on the first run.
  - To use a different password, set the DB_PASSWORD environment variable or edit backend/src/main/resources/application.properties.

 4. Run the backend :
  - cd backend
  - mvn spring-boot:run
  - The API starts on http://localhost:8080 and demo data is added automatically.

 5. Run the frontend :
  - cd frontend
  - npm install
  - npm start
  - The application opens on http://localhost:4200


## 🏗️ Project Structure

```
bazaarly
├── backend (Spring Boot)
│   └── src/main/java/com/bazaarly
│       ├── config    -> Security, JWT, CORS, exception handling, demo data
│       ├── entity    -> JPA entities
│       ├── repo      -> Spring Data repositories
│       ├── service   -> Business logic (orders, pricing, coupons, stats)
│       ├── payment   -> Payment gateway interface and test gateway
│       └── web       -> REST controllers
└── frontend (Angular)
    └── src/app
        ├── core      -> API service, auth service, interceptor, guards
        ├── shared    -> Reusable components
        └── pages     -> Home, products, cart, checkout, orders, seller, admin
```


## Backend Dependencies (Spring Initializr)

 - Spring Web : For building REST APIs and handling HTTP requests.
 - Spring Security : For authentication and role-based access control.
 - Spring Data JPA : For database access using Hibernate.
 - Validation : For validating request data.
 - MySQL Driver : MySQL Connector to connect the application with the MySQL database.
 - Lombok : To reduce boilerplate code in entity classes.
 - JJWT : For creating and validating JSON Web Tokens.


## Future Enhancements

 - Razorpay payment gateway integration
 - Email and SMS notifications
 - Separate sub-orders for products from multiple sellers
 - Unit and integration tests


## 👨‍💻 Author

For any inquiries, questions, or feedback related to this project, feel free to reach out to me:

- GitHub :  (https://github.com/kunalrayate)
- Email :  (kunalrayate126@gmail.com)


