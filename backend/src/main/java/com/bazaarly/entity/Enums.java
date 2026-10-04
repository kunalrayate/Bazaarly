package com.bazaarly.entity;

public class Enums {
    public enum Role { CUSTOMER, SELLER, ADMIN }
    public enum UserStatus { ACTIVE, PENDING, REJECTED, BLOCKED }
    public enum OrderStatus { PLACED, CONFIRMED, PROCESSING, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED, RETURNED }
    public enum PaymentStatus { PENDING, PAID, FAILED, REFUNDED }
    public enum ReturnStatus { NONE, REQUESTED, REJECTED, REFUNDED }
    public enum CouponType { PERCENT, FIXED }
    public enum CouponScope { ALL, CATEGORY, PRODUCT }
}
