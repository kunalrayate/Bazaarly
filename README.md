# Bazaarly - Full-stack E-commerce Platform

Java 17 · Spring Boot 3.2 · Spring Security (JWT) · Hibernate/JPA · MySQL 8 · Angular 17

An original Amazon-style marketplace with **three roles** (Customer, Seller, Admin), full shopping flow
(discover → product → cart → coupon → checkout → test payment → order tracking), inventory, reviews,
recommendations and analytics dashboards.

## Run it

**1. Database** - MySQL 8 on localhost:3306 (user `root` / password `root`), or `docker compose up -d`.
The `bazaarly` database and all tables are created automatically.
Different credentials? `export DB_USER=... DB_PASSWORD=...` (or edit `backend/src/main/resources/application.properties`).

**2. Backend** (needs JDK 17+ and Maven)
```
cd backend
mvn spring-boot:run          # http://localhost:8080
```
On first start demo data is seeded (24 products, categories, coupons, demo orders/reviews).
Turn this off with `app.seed-demo-data=false`.

**3. Frontend** (needs Node 18+)
```
cd frontend
npm install
npm start                    # http://localhost:4200
```
API URL is in `frontend/src/environments/environment.ts`.

## Demo accounts
| Role | Email | Password |
|---|---|---|
| Customer | customer@bazaarly.com | Customer@123 |
| Seller | seller@bazaarly.com | Seller@123 |
| Seller (2nd store) | books@bazaarly.com | Seller@123 |
| Seller (pending approval) | newseller@bazaarly.com | Seller@123 |
| Admin | admin@bazaarly.com | Admin@123 |

Coupons to try: `WELCOME10`, `FLAT200`, `BOOKLOVER15` (books only), `FESTIVE25` (electronics only).

Try this flow: log in as customer → search/filter → open the Nimbus X5 phone → pick color/storage → add to cart →
apply `WELCOME10` → checkout → "Pay now (success)". Then log in as seller → Orders → move it
Confirmed → Processing → Shipped → Out for delivery → Delivered. Back as customer you can now review the product
and request a return.

## Feature map
**Customer** - register/login, category browse, keyword search, filters (price, brand, category, rating, availability), sorting
(price/rating/popularity/newest), gallery, variants, cart with quantity, wishlist, coupons, multiple addresses, checkout,
test payment (retry on failure), order history, 6-step tracking, cancel, return/refund, verified-purchase reviews with
rating distribution, report review, profile, notifications, recently viewed, recommendations.

**Seller** - store registration (needs admin approval), product CRUD with image upload, variants + price add-ons,
discounts & limited-time deals, inline stock editing, low-stock alerts, incoming orders, status updates,
return decisions, revenue / best sellers / sales trend / order stats.

**Admin** - dashboard (revenue, orders, customers, sellers, trends, popular products & categories), customers & sellers
(approve / reject / block), categories, product listings (feature / unlist), all orders, coupons & offers
(percentage, fixed, category/product scoped, min purchase, cap, usage limit, start/expiry), reported reviews, low-stock inventory.

**Rules implemented** - stock is locked (pessimistic lock) and deducted at order time, restored on cancel/return;
expired deals are switched off by a scheduler; free delivery above ₹500; return window 7 days;
online orders can't be confirmed until paid; refunds mark payment `REFUNDED`.

## Project layout
```
backend/src/main/java/com/bazaarly
  config/   security + JWT, exception handling, demo data seeder
  entity/   JPA entities      repo/  Spring Data repositories
  service/  pricing & coupons, orders & inventory, recommendations, stats, reviews
  payment/  PaymentGateway interface + MockPaymentGateway
  web/      REST controllers (auth, catalog, cart, orders, seller, admin, ...)
frontend/src/app
  core/ api, auth, interceptor + guards     shared/ cards, stars, charts
  pages/ home, products, product-detail, cart, checkout, orders, order-detail, account, seller, admin
```

## Switching to Razorpay
The whole payment flow goes through `payment/PaymentGateway.java` (create → verify → refund).
1. Add the `com.razorpay:razorpay-java` dependency to `pom.xml`.
2. Create `RazorpayPaymentGateway implements PaymentGateway` annotated `@Service`, and remove `@Service` from `MockPaymentGateway`:
   - `createPayment` → create a Razorpay Order (amount in paise) and return `{key, razorpayOrderId, amount, currency, paymentRef}`.
   - `verify` → validate `razorpay_signature` of `razorpay_order_id|razorpay_payment_id` with your key secret (`Utils.verifyPaymentSignature`).
   - `refund` → `client.payments.refund(paymentId, ...)`.
3. Frontend: load `https://checkout.razorpay.com/v1/checkout.js` and, in `checkout.ts` / `order-detail.ts`,
   replace the test modal with `new Razorpay({ key, order_id, handler: res => post('/payments/{id}/confirm', res) })`.
   The `/api/payments/{orderId}/initiate` and `/confirm` endpoints stay the same.
Keep keys in environment variables, never in git.

## Notes / next steps
- Passwords are BCrypt-hashed; JWT lifetime 24h. Change `app.jwt.secret` before deploying.
- Orders with items from several sellers share one order status (kept simple on purpose). Per-seller sub-orders are the natural next step.
- Product images from the seed use picsum.photos (needs internet). Uploaded images are stored in `backend/uploads`.
- Production hardening to consider: Flyway migrations instead of `ddl-auto=update`, Redis cache, Elasticsearch for search, rate limiting, tests.
