package com.bazaarly.config;

import com.bazaarly.entity.*;
import com.bazaarly.entity.Enums.*;
import com.bazaarly.repo.*;
import com.bazaarly.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Seeds demo data on first start (only when the database is empty). Disable with app.seed-demo-data=false. */
@Component @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepo users; private final CategoryRepo categories; private final ProductRepo products; private final CouponRepo coupons;
    private final AddressRepo addresses; private final ReviewRepo reviews; private final OrderRepo orders; private final ReviewService reviewService;
    private final PasswordEncoder encoder;
    @Value("${app.seed-demo-data}") private boolean seed;
    private final Random rnd = new Random(42);

    @Override public void run(String... args) {
        if (users.count() > 0) return;
        User admin = user("Platform Admin", "admin@bazaarly.com", "Admin@123", Role.ADMIN, UserStatus.ACTIVE);
        User s1 = user("Ravi Kulkarni", "seller@bazaarly.com", "Seller@123", Role.SELLER, UserStatus.ACTIVE); s1.setStoreName("Urban Cart Traders"); s1.setStoreDescription("Electronics, home and lifestyle essentials."); users.save(s1);
        User s2 = user("Meera Shah", "books@bazaarly.com", "Seller@123", Role.SELLER, UserStatus.ACTIVE); s2.setStoreName("Page Turner Books & Pantry"); s2.setStoreDescription("Books and everyday groceries."); users.save(s2);
        User pending = user("Arjun Mehta", "newseller@bazaarly.com", "Seller@123", Role.SELLER, UserStatus.PENDING); pending.setStoreName("Fresh Start Crafts"); users.save(pending);
        User cust = user("Priya Sharma", "customer@bazaarly.com", "Customer@123", Role.CUSTOMER, UserStatus.ACTIVE);
        Address a = new Address(); a.setUser(cust); a.setFullName("Priya Sharma"); a.setPhone("9876543210"); a.setLine1("Flat 402, Lotus Residency"); a.setLine2("Baner Road"); a.setCity("Pune"); a.setState("Maharashtra"); a.setPincode("411045"); a.setDefaultAddress(true); addresses.save(a);
        if (!seed) return;

        Category el = cat("Electronics", "🎧", "Phones, laptops, audio and more"), fa = cat("Fashion", "👕", "Clothing, footwear and accessories"),
                gr = cat("Grocery", "🥬", "Staples, snacks and beverages"), bk = cat("Books", "📚", "Fiction, non-fiction and study material"),
                fu = cat("Furniture", "🛋️", "Furniture for home and office"), hk = cat("Home & Kitchen", "🍳", "Appliances, cookware and decor");

        List<Product> ps = new ArrayList<>();
        ps.add(p(s1, el, "Nimbus X5 5G Smartphone", "Nimbus", 24999, 12, 60, "6.7-inch AMOLED 120Hz display, 50MP triple camera, 5000mAh battery with 67W fast charging.", Map.of("Display", "6.7\" AMOLED 120Hz", "Battery", "5000 mAh", "Camera", "50MP + 8MP + 2MP", "OS", "Android 14"), v("Color", "Midnight Black", 0), v("Color", "Ocean Blue", 0), v("Storage", "128 GB", 0), v("Storage", "256 GB", 3000)));
        ps.add(p(s1, el, "AeroBook 14 Ultra-Light Laptop", "Aerotech", 58990, 8, 25, "14-inch 2.2K display, Intel Core i5 13th Gen, 16GB RAM, all-day battery. Weighs just 1.3 kg.", Map.of("Processor", "Core i5-1340P", "RAM", "16 GB LPDDR5", "Display", "14\" 2.2K IPS", "Weight", "1.3 kg"), v("Storage", "512 GB SSD", 0), v("Storage", "1 TB SSD", 6000)));
        ps.add(p(s1, el, "PulseBuds Pro Wireless Earbuds", "Sonara", 4999, 40, 200, "Active noise cancellation, 36-hour battery life with case, IPX5 sweat resistance and low-latency gaming mode.", Map.of("Battery", "36 hours", "ANC", "Yes", "Bluetooth", "5.3", "Water resistance", "IPX5"), v("Color", "White", 0), v("Color", "Graphite", 0)));
        ps.add(p(s1, el, "StreamStick 4K Smart TV Player", "Nimbus", 3499, 30, 80, "Turn any TV smart. 4K HDR streaming, voice remote and built-in casting.", Map.of("Resolution", "4K HDR", "Voice remote", "Yes", "Storage", "8 GB")));
        ps.add(p(s1, el, "Orbit 55\" 4K Android TV", "Orbit", 36999, 22, 15, "55-inch 4K UHD LED, Dolby Audio, 3 HDMI ports, built-in Chromecast.", Map.of("Screen", "55 inch", "Resolution", "3840x2160", "Refresh rate", "60 Hz", "HDMI", "3")));
        ps.add(p(s1, el, "VoltMax 20000mAh Power Bank", "Voltmax", 1799, 35, 6, "22.5W fast charging power bank with dual USB-C and digital display.", Map.of("Capacity", "20000 mAh", "Output", "22.5W", "Ports", "2x USB-C, 1x USB-A")));
        ps.add(p(s1, fa, "Classic Fit Cotton Shirt", "Wearwell", 1499, 45, 150, "Breathable 100% cotton shirt for office and casual wear. Machine washable.", Map.of("Fabric", "100% Cotton", "Fit", "Classic", "Care", "Machine wash"), v("Size", "S", 0), v("Size", "M", 0), v("Size", "L", 0), v("Size", "XL", 100), v("Color", "Sky Blue", 0), v("Color", "White", 0)));
        ps.add(p(s1, fa, "TrailRunner Lite Running Shoes", "Striders", 3299, 35, 90, "Lightweight mesh running shoes with cushioned EVA midsole and anti-slip outsole.", Map.of("Upper", "Breathable mesh", "Sole", "EVA + rubber", "Use", "Running / Gym"), v("Size", "UK 7", 0), v("Size", "UK 8", 0), v("Size", "UK 9", 0), v("Size", "UK 10", 0)));
        ps.add(p(s1, fa, "Slim Stretch Denim Jeans", "Wearwell", 2199, 30, 120, "Mid-rise slim fit jeans with stretch for all-day comfort.", Map.of("Fabric", "98% Cotton 2% Elastane", "Fit", "Slim"), v("Size", "30", 0), v("Size", "32", 0), v("Size", "34", 0), v("Color", "Indigo", 0), v("Color", "Black", 0)));
        ps.add(p(s1, fa, "Everyday Canvas Backpack 28L", "Roamer", 1299, 25, 7, "Water-resistant canvas backpack with padded laptop sleeve up to 15.6 inches.", Map.of("Capacity", "28 L", "Laptop sleeve", "15.6\"", "Material", "Waxed canvas"), v("Color", "Olive", 0), v("Color", "Navy", 0)));
        ps.add(p(s2, gr, "Himalayan Rock Salt 1 kg", "PureNest", 99, 0, 500, "Natural unrefined rock salt, rich in minerals.", Map.of("Weight", "1 kg", "Type", "Rock salt")));
        ps.add(p(s2, gr, "Cold-Pressed Groundnut Oil 1 L", "PureNest", 329, 10, 300, "Traditional wood-pressed groundnut oil. No chemicals, no preservatives.", Map.of("Volume", "1 L", "Process", "Cold pressed")));
        ps.add(p(s2, gr, "Assam Gold Tea 500 g", "ChaiCo", 249, 15, 400, "Strong, malty CTC Assam tea leaves for the perfect masala chai.", Map.of("Weight", "500 g", "Type", "CTC")));
        ps.add(p(s2, gr, "Premium California Almonds 1 kg", "NuttyFields", 899, 20, 180, "Crunchy, protein-rich almonds, hand-sorted and vacuum packed.", Map.of("Weight", "1 kg", "Origin", "California")));
        ps.add(p(s2, gr, "Millet Muesli with Dates 750 g", "GrainGood", 349, 18, 9, "Fiber-rich breakfast muesli with ragi, jowar and dates. No added sugar.", Map.of("Weight", "750 g", "Sugar", "No added sugar")));
        ps.add(p(s2, bk, "The Alchemy of Habits", "Lakeshore Press", 399, 25, 250, "A practical guide to building lasting routines using small, science-backed steps.", Map.of("Author", "Nisha Rao", "Pages", "288", "Language", "English"), v("Format", "Paperback", 0), v("Format", "Hardcover", 250)));
        ps.add(p(s2, bk, "Data Structures Made Simple", "CodeCraft Publishers", 649, 20, 140, "Learn core data structures with visual explanations and Java examples.", Map.of("Author", "Dr. S. Iyer", "Pages", "464", "Edition", "3rd")));
        ps.add(p(s2, bk, "Monsoon Letters (Novel)", "Lakeshore Press", 299, 10, 100, "A moving novel about family, memory and the rains of Konkan.", Map.of("Author", "Anika Deshpande", "Pages", "320", "Language", "English")));
        ps.add(p(s2, bk, "UPSC Prelims Practice Papers", "ExamEdge", 549, 30, 8, "20 full-length mock tests with detailed solutions.", Map.of("Pages", "612", "Edition", "2026")));
        ps.add(p(s1, fu, "ErgoFlex Mesh Office Chair", "Sitwell", 7999, 38, 40, "Ergonomic mesh back chair with adjustable lumbar support, armrests and 3-year warranty.", Map.of("Material", "Mesh + nylon", "Warranty", "3 years", "Max load", "120 kg"), v("Color", "Black", 0), v("Color", "Grey", 0)));
        ps.add(p(s1, fu, "Solid Sheesham Study Table", "WoodCraft", 11499, 20, 18, "Handcrafted sheesham wood desk with 2 drawers and cable cut-out.", Map.of("Material", "Sheesham wood", "Dimensions", "120x60x75 cm", "Drawers", "2")));
        ps.add(p(s1, fu, "3-Seater Fabric Sofa", "Sitwell", 27999, 25, 5, "Comfortable 3-seater sofa with high-density foam and solid wood frame.", Map.of("Seating", "3 persons", "Frame", "Solid wood", "Upholstery", "Fabric"), v("Color", "Ash Grey", 0), v("Color", "Teal", 1500)));
        ps.add(p(s1, hk, "SwiftChef 5L Air Fryer", "Kitchenly", 6499, 40, 70, "Cook crispy meals with up to 90% less oil. 8 presets, digital touch panel.", Map.of("Capacity", "5 L", "Power", "1500 W", "Presets", "8")));
        ps.add(p(s1, hk, "Triply Stainless Steel Cookware Set (5 pc)", "Kitchenly", 5999, 33, 45, "Induction-friendly triply cookware with stay-cool handles.", Map.of("Pieces", "5", "Material", "Stainless steel", "Induction", "Yes")));
        ps.add(p(s1, hk, "Cotton Bedsheet Set - King (3 pc)", "HomeLoom", 1199, 45, 220, "300 thread count cotton bedsheet with 2 pillow covers.", Map.of("Size", "King", "Thread count", "300", "Pieces", "3"), v("Color", "Sage", 0), v("Color", "Ivory", 0), v("Color", "Charcoal", 0)));
        // limited-time deals
        ps.get(2).setDiscountEndsAt(LocalDateTime.now().plusDays(3)); ps.get(5).setDiscountEndsAt(LocalDateTime.now().plusHours(30));
        for (int i : new int[]{0, 2, 6, 14, 19, 21}) ps.get(i).setFeatured(true);
        for (Product p : ps) { p.setViewCount(rnd.nextInt(900) + 20); p.setSoldCount(rnd.nextInt(60)); products.save(p); }

        // demo coupons
        coupon("WELCOME10", "10% off on your order (max ₹300)", CouponType.PERCENT, 10, 499, 300, CouponScope.ALL, null, 90);
        coupon("FLAT200", "Flat ₹200 off on orders above ₹1999", CouponType.FIXED, 200, 1999, null, CouponScope.ALL, null, 60);
        coupon("BOOKLOVER15", "15% off on all books", CouponType.PERCENT, 15, 299, 200, CouponScope.CATEGORY, bk.getId(), 45);
        coupon("FESTIVE25", "Limited-time: 25% off electronics (max ₹2500)", CouponType.PERCENT, 25, 2999, 2500, CouponScope.CATEGORY, el.getId(), 7);

        // demo shoppers, delivered orders (for analytics) and reviews
        List<User> shoppers = new ArrayList<>();
        for (int i = 1; i <= 6; i++) shoppers.add(user("Demo Shopper " + i, "shopper" + i + "@demo.com", UUID.randomUUID().toString(), Role.CUSTOMER, UserStatus.ACTIVE));
        shoppers.add(cust);
        for (int n = 0; n < 40; n++) {
            User u = shoppers.get(rnd.nextInt(shoppers.size())); Order o = new Order(); o.setUser(u);
            o.setOrderNumber("BZSEED" + (1000 + n)); o.setCreatedAt(LocalDateTime.now().minusDays(rnd.nextInt(14)).minusHours(rnd.nextInt(20)));
            BigDecimal sub = BigDecimal.ZERO; Set<Integer> used = new HashSet<>();
            for (int k = 0, c = 1 + rnd.nextInt(3); k < c; k++) {
                int idx = rnd.nextInt(ps.size()); if (!used.add(idx)) continue; Product p = ps.get(idx); int q = 1 + rnd.nextInt(2);
                OrderItem oi = new OrderItem(); oi.setOrder(o); oi.setProduct(p); oi.setProductName(p.getName()); oi.setImage(p.getImages().get(0)); oi.setUnitPrice(p.getSellingPrice()); oi.setQuantity(q);
                o.getItems().add(oi); sub = sub.add(p.getSellingPrice().multiply(BigDecimal.valueOf(q)));
            }
            o.setSubtotal(sub); o.setDiscount(BigDecimal.ZERO); o.setShipping(BigDecimal.ZERO); o.setTotal(sub);
            o.setShippingName(u.getName()); o.setShippingAddress("Demo address, Pune, Maharashtra - 411001"); o.setPaymentMethod(n % 3 == 0 ? "COD" : "ONLINE"); o.setPaymentStatus(PaymentStatus.PAID);
            OrderStatus[] flow = {OrderStatus.DELIVERED, OrderStatus.DELIVERED, OrderStatus.DELIVERED, OrderStatus.SHIPPED, OrderStatus.PROCESSING, OrderStatus.CONFIRMED};
            o.setStatus(flow[rnd.nextInt(flow.length)]); if (o.getStatus() == OrderStatus.DELIVERED) o.setDeliveredAt(o.getCreatedAt().plusDays(3));
            o.addEvent("Order placed"); orders.save(o);
        }
        String[] titles = {"Excellent value", "Does the job", "Not as expected", "Loved it!", "Great quality", "Decent"};
        String[] texts = {"Great product, delivery was quick and packaging was neat.", "Works as described. Happy with the purchase.", "Quality could have been better for the price.", "Exceeded my expectations, would buy again!", "Solid build and good finish. Recommended.", "Average experience but support was helpful."};
        int[] ratings = {5, 4, 2, 5, 4, 3};
        for (int i = 0; i < ps.size(); i++) {
            int count = 2 + rnd.nextInt(4);
            for (int j = 0; j < count && j < shoppers.size(); j++) {
                int k = (i + j) % 6; Review r = new Review(); r.setUser(shoppers.get(j)); r.setProduct(ps.get(i)); r.setRating(Math.min(5, Math.max(1, ratings[k] + (rnd.nextInt(3) - 1) * (rnd.nextBoolean() ? 1 : 0))));
                r.setTitle(titles[k]); r.setComment(texts[k]); r.setCreatedAt(LocalDateTime.now().minusDays(rnd.nextInt(30))); reviews.save(r);
            }
            reviewService.recalc(ps.get(i));
        }
    }

    private User user(String name, String email, String pw, Role role, UserStatus st) {
        User u = new User(); u.setName(name); u.setEmail(email); u.setPassword(encoder.encode(pw)); u.setRole(role); u.setStatus(st); u.setPhone("98" + (10000000 + rnd.nextInt(89999999))); return users.save(u);
    }
    private Category cat(String n, String icon, String d) { Category c = new Category(); c.setName(n); c.setIcon(icon); c.setDescription(d); return categories.save(c); }
    private static String[] vs(String t, String v, int adj) { return new String[]{t, v, String.valueOf(adj)}; }
    private static String[] v(String t, String v, int adj) { return vs(t, v, adj); }
    private void coupon(String code, String desc, CouponType t, int val, int min, Integer max, CouponScope sc, Long scopeId, int days) {
        Coupon c = new Coupon(); c.setCode(code); c.setDescription(desc); c.setType(t); c.setValue(BigDecimal.valueOf(val)); c.setMinPurchase(BigDecimal.valueOf(min));
        c.setMaxDiscount(max == null ? null : BigDecimal.valueOf(max)); c.setScope(sc); c.setScopeId(scopeId); c.setExpiresAt(LocalDateTime.now().plusDays(days)); c.setUsageLimit(1000); coupons.save(c);
    }
    private Product p(User seller, Category cat, String name, String brand, int price, int disc, int stock, String desc, Map<String, String> specs, String[]... variants) {
        Product p = new Product(); p.setSeller(seller); p.setCategory(cat); p.setName(name); p.setBrand(brand); p.setPrice(BigDecimal.valueOf(price)); p.setDiscountPercent(disc);
        p.setStock(stock); p.setDescription(desc); p.setSpecifications(new LinkedHashMap<>(specs));
        String seed = name.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        p.setImages(new ArrayList<>(List.of("https://picsum.photos/seed/" + seed + "-1/800/800", "https://picsum.photos/seed/" + seed + "-2/800/800", "https://picsum.photos/seed/" + seed + "-3/800/800")));
        for (String[] v : variants) { ProductVariant pv = new ProductVariant(); pv.setProduct(p); pv.setType(v[0]); pv.setValue(v[1]); pv.setPriceAdjustment(new BigDecimal(v[2])); p.getVariants().add(pv); }
        p.computePrice(); return p;
    }
}
