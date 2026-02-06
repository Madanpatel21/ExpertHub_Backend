# 📚 Complete Educational Guide to Expert Hub Backend (Spring Boot)

> **Author:** AI Assistant | **Project:** Expert Hub Backend | **Framework:** Spring Boot 3.4.2

---

## 📋 Table of Contents

1. [Project Overview & Architecture](#chapter-1-project-overview--architecture)
2. [Authentication System (JWT + Refresh Tokens)](#chapter-2-authentication-system-jwt--refresh-tokens)
3. [Entities & Database Models](#chapter-3-entities--database-models)
4. [Repositories & Database Access](#chapter-4-repositories--database-access)
5. [Services & Business Logic](#chapter-5-services--business-logic)
6. [Controllers & REST Endpoints](#chapter-6-controllers--rest-endpoints)
7. [WebSocket Implementation (Real-time Communication)](#chapter-7-websocket-implementation-real-time-communication)
8. [Configuration Files](#chapter-8-configuration-files)
9. [Best Practices & Design Patterns](#chapter-9-best-practices--design-patterns)

---

# Chapter 1: Project Overview & Architecture

## 🎯 What is Expert Hub?

Expert Hub is a **lawyer-client consultation platform** built with Spring Boot. It enables:
- 🔐 Secure authentication (OTP + JWT)
- 📅 Appointment booking system
- 📹 Video/audio calls between lawyers and clients
- 💬 Real-time chat messaging
- 💳 Payment integration (Razorpay)
- 📱 SMS notifications (Twilio/MSG91)

## 🏗️ Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CONTROLLER LAYER                         │
│  (@RestController) - Handle HTTP requests, validation       │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     SERVICE LAYER                           │
│  (@Service) - Business logic, transactions                  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                   REPOSITORY LAYER                          │
│  (@Repository) - Database operations, JPA                   │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                   DATABASE (MySQL)                          │
└─────────────────────────────────────────────────────────────┘
```

## 📦 pom.xml - Maven Dependencies Deep Dive

```xml
<!-- Spring Boot Parent - Manages all dependency versions -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.2</version>
</parent>
```

### Core Dependencies Explained:

| Dependency | Purpose | Key Concepts |
|------------|---------|--------------|
| `spring-boot-starter-web` | REST API development | MVC, HTTP, Tomcat |
| `spring-boot-starter-data-jpa` | Database access | Hibernate, JPA, Spring Data |
| `spring-boot-starter-security` | Authentication/Authorization | Filters, SecurityContext |
| `spring-boot-starter-websocket` | Real-time communication | STOMP, WebSocket |
| `spring-boot-starter-validation` | Input validation | @Valid, Bean Validation |
| `spring-boot-starter-cache` | Caching abstraction | @Cacheable, Redis |
| `spring-boot-starter-data-redis` | Redis integration | In-memory data store |

### Third-Party Integrations:

```xml
<!-- JWT for stateless authentication -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>

<!-- Twilio for SMS/OTP -->
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>10.1.0</version>
</dependency>

<!-- Razorpay for payments (India) -->
<dependency>
    <groupId>com.razorpay</groupId>
    <artifactId>razorpay-java</artifactId>
    <version>1.4.3</version>
</dependency>

<!-- Cloudinary for image uploads -->
<dependency>
    <groupId>com.cloudinary</groupId>
    <artifactId>cloudinary-http5</artifactId>
    <version>2.0.0</version>
</dependency>

<!-- Swagger/OpenAPI for API docs -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

### 🔑 Lombok Dependency (Critical for Code Reduction):

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.40</version>
</dependency>
```

**Lombok Annotations:**
- `@Getter` - Generates getXxx() methods
- `@Setter` - Generates setXxx() methods
- `@AllArgsConstructor` - Constructor with all fields
- `@NoArgsConstructor` - No-argument constructor
- `@Data` - @Getter + @Setter + @ToString + @EqualsAndHashCode
- `@Builder` - Builder pattern implementation

---

# Chapter 2: Authentication System (JWT + Refresh Tokens)

## 🔐 Why JWT Instead of Sessions?

### Session-Based Authentication:
```
Client → Server (login) → Server creates session ID → Returns cookie
Client → Server (request + cookie) → Server validates session → Response
```

**Problem:** Sessions require server-side storage and don't scale well.

### JWT (Token-Based) Authentication:
```
Client → Server (login) → Server returns JWT token
Client → Server (request + Authorization: Bearer <token>) → Server validates JWT → Response
```

**Advantages:**
- ✅ Stateless (no server-side storage needed)
- ✅ Scalable (works across multiple servers)
- ✅ Portable (can be used by mobile apps, SPAs)
- ✅ Decentralized (any server can validate)

## 📝 JwtUtil.java - Complete Line-by-Line Explanation

```java
package Util;  // Package for utility classes

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component  // Spring bean - auto-discovered
public class JwtUtil {
    private static SecretKey KEY;  // Signing key (HMAC-SHA256)
    private static long EXPIRATION;  // Access token expiry (1 hour)
    private static long REFRESH_EXPIRATION = 30L * 24 * 60 * 60 * 1000; // 30 days in ms

    // Constructor - Spring injects values from application.properties
    public JwtUtil(
            @Value("${jwt.secret:ChangeThisSecretForProdMustBeLongEnough}") String secret,
            @Value("${jwt.expiration:3600000}") long expiration) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        JwtUtil.KEY = Keys.hmacShaKeyFor(keyBytes);  // Create HMAC-SHA key
        JwtUtil.EXPIRATION = expiration;
    }

    // Generate access token with email and role
    public static String generateToken(String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION);

        return Jwts.builder()
                .subject(email)              // "sub" claim - who this token is for
                .claim("role", role)         // Custom claim - user's role
                .issuedAt(now)               // "iat" claim - issued at time
                .expiration(expiry)          // "exp" claim - expiration time
                .signWith(KEY)               // Sign with secret key
                .compact();                  // Serialize to string
    }

    // Generate refresh token (longer expiry, different type)
    public static String generateRefreshToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + REFRESH_EXPIRATION);

        return Jwts.builder()
                .subject(email)
                .claim("type", "refresh")    // Mark as refresh token
                .issuedAt(now)
                .expiration(expiry)
                .id(UUID.randomUUID().toString())  // Unique token ID
                .signWith(KEY)
                .compact();
    }

    // Parse and extract all claims from token
    private static Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(KEY)            // Verify signature
                .build()
                .parseSignedClaims(token)   // Parse signed JWT
                .getPayload();              // Return claims (payload)
    }

    // Extract role from token
    public static String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    // Extract email (subject) from token
    public static String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    // Validate token (check expiry)
    public static boolean isTokenValid(String token) {
        try {
            return extractClaims(token)
                    .getExpiration()
                    .after(new Date());  // Not expired?
        } catch (Exception e) {
            return false;  // Invalid token
        }
    }

    // Check if token is a refresh token
    public static boolean isRefreshToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }
}
```

## 🔄 Refresh Token Flow

```
1. User logs in with credentials
2. Server validates credentials, generates:
   - Access Token (expires in 1 hour)
   - Refresh Token (expires in 30 days)
3. Client stores both tokens
4. Client uses Access Token for API requests
5. When access token expires:
   - Client sends refresh token to /api/auth/refresh
   - Server validates refresh token
   - Server issues new access token
6. If refresh token expires, user must re-login
```

## 🛡️ SecurityConfig.java - Spring Security Setup

```java
@Configuration  // Spring configuration class
@EnableWebSecurity  // Enable Spring Security
@EnableMethodSecurity  // Enable @PreAuthorize
public class securityConfig {

    @Autowired
    private jwtAuthFilter authFilter;  // JWT filter for authentication

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserInfoService();  // Load user details for authentication
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // Password hashing
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())  // Disable CSRF for REST API
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()  // Auth endpoints public
                        .requestMatchers("/api/payments/webhook").permitAll()  // Webhook public
                        .requestMatchers("/ws/**").permitAll()  // WebSocket public
                        .requestMatchers("/swagger-ui/**").authenticated()  // Swagger secured
                        .anyRequest().authenticated())  // All other endpoints require auth
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // No sessions
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}
```

## 🔒 jwtAuthFilter.java - JWT Validation Filter

```java
@Component  // Auto-discovered bean
public class jwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final tokenBlacklistService blacklistService;

    public jwtAuthFilter(JwtUtil jwtUtil, tokenBlacklistService blacklistService) {
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        // 1️⃣ Extract Authorization header
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;  // No token, continue without authentication
        }

        // 2️⃣ Extract JWT token
        String token = authHeader.substring(7);

        // 3️⃣ Check if token is blacklisted (logged out)
        if (blacklistService.isBlacklisted(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 4️⃣ Validate JWT
        try {
            if (!jwtUtil.isTokenValid(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // 5️⃣ Extract claims
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);

            // 6️⃣ Create Authentication object
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                );

            // 7️⃣ Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 8️⃣ Continue filter chain
        filterChain.doFilter(request, response);
    }
}
```

**Filter Chain Flow:**
```
Request → CSRF Filter → Session Filter → JWT Filter → Controller
                                              ↓
                                        Authentication check
```

---

# Chapter 3: Entities & Database Models

## 📋 User Entity (Complete)

```java
@Entity
@Table(name = "users")
public class user {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;  // BCrypt hashed password

    @ManyToOne(fetch = FetchType.EAGER)  // Always load role with user
    @JoinColumn(name = "role_id", nullable = false)
    private roles role;  // CLIENT, LAWYER, or ADMIN

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String name;

    private String location;
    private Boolean publicProfile;
    private String profileImagePath;
    @Column(length = 2000)
    private String bio;

    // Notification preferences
    private Boolean pushNotificationsEnabled = true;
    private Boolean emailNotificationsEnabled = true;

    // OTP for login verification
    private String otp;
}
```

### JPA Annotations Explained:

| Annotation | Purpose |
|------------|---------|
| `@Entity` | Marks class as JPA entity (maps to database table) |
| `@Table(name = "users")` | Specifies table name (if different from class name) |
| `@Id` | Marks primary key |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Auto-increment ID |
| `@Column(nullable = false, unique = true)` | Database column with constraints |
| `@ManyToOne` | Many users can have one role |
| `@JoinColumn(name = "role_id")` | Foreign key column name |

## 👤 Roles Entity

```java
@Entity
@Table(name = "roles")
public class roles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;  // CLIENT, LAWYER, ADMIN
}
```

### Role-Based Access Control (RBAC):

| Role | Permissions |
|------|------------|
| `CLIENT` | Book appointments, chat with lawyers, make payments |
| `LAWYER` | View appointments, conduct calls, manage profile |
| `ADMIN` | Full access, view all users, manage system |

## 📅 Appointment Entity

```java
@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clientId;
    private Long lawyerId;
    private LocalDateTime scheduledTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;  // PENDING, CONFIRMED, COMPLETED, CANCELLED

    @Enumerated(EnumType.STRING)
    private CallType callType;  // AUDIO, VIDEO

    private String meetingLink;
    private Long amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;  // PENDING, PAID, REFUNDED

    private String razorPayOrderId;
    private String razorPayPaymentId;
    private String razorPaySignature;

    private LocalDateTime createdAt;
}
```

### Enum Types:

```java
public enum AppointmentStatus {
    PENDING,      // Waiting for lawyer approval
    CONFIRMED,    // Lawyer accepted
    COMPLETED,    // Appointment finished
    CANCELLED,    // Cancelled by either party
    REFUNDED     // Payment refunded
}

public enum PaymentStatus {
    PENDING,
    PAID,
    REFUNDED,
    FAILED
}

public enum CallType {
    AUDIO,
    VIDEO
}
```

## 📞 Call Session Entity

```java
@Entity
@Table(name = "call_sessions")
public class CallSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String callerId;
    private String calleeId;
    private String appointmentId;

    @Enumerated(EnumType.STRING)
    private CallType callType;  // AUDIO or VIDEO

    @Enumerated(EnumType.STRING)
    private CallStatus status;  // INITIATED, CONNECTED, COMPLETED, MISSED, CANCELLED

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationInSeconds;

    public enum CallStatus {
        INITIATED,   // Call started but not answered
        CONNECTED,   // Both parties connected
        COMPLETED,   // Call ended normally
        MISSED,      // Caller hung up without answer
        CANCELLED    // Call cancelled before answer
    }
}
```

---

# Chapter 4: Repositories & Database Access

## 📦 Repository Pattern

Repositories abstract database operations:

```java
// Instead of writing SQL, we use method names!
public interface userRepository extends JpaRepository<user, Long> {
    Optional<user> findByEmail(String email);           // SELECT * FROM users WHERE email = ?
    boolean existsByEmail(String email);                // SELECT COUNT(*) FROM users WHERE email = ?
    Optional<user> findByPhoneNumber(String phone);    // SELECT * FROM users WHERE phone = ?
}
```

### UserRepository Complete:

```java
@Repository
public interface userRepository extends JpaRepository<user, Long> {
    Optional<user> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<user> findByPhoneNumber(String phone);
}
```

### RefreshTokenRepository:

```java
@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByEmail(String email);
    Optional<RefreshToken> findByEmail(String email);
}
```

### Spring Data JPA Method Naming Rules:

| Method Name | Generated SQL |
|------------|--------------|
| `findByEmail(String email)` | `SELECT * FROM users WHERE email = ?` |
| `findByEmailAndPassword(...)` | `SELECT * WHERE email = ? AND password = ?` |
| `findByEmailOrPhone(...)` | `SELECT * WHERE email = ? OR phone = ?` |
| `findByNameContaining(String name)` | `SELECT * WHERE name LIKE %?%` |
| `findByAgeGreaterThanEqual(int age)` | `SELECT * WHERE age >= ?` |
| `existsByEmail(String email)` | `SELECT COUNT(*) FROM users WHERE email = ?` |
| `deleteByEmail(String email)` | `DELETE FROM users WHERE email = ?` |

---

# Chapter 5: Services & Business Logic

## 🏢 Service Layer Pattern

Services contain **business logic** and coordinate between controllers and repositories.

### authService.java - Complete:

```java
@Service  // Spring stereotype annotation
public class authService {

    private final userRepository userRepo;
    private final roleRepository roleRepo;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisOTPService redisOtpService;
    private final RateLimitService rateLimitService;
    private final otpService OtpService;

    // Constructor injection (best practice!)
    public authService(userRepository userRepo, roleRepository roleRepo,
            JwtUtil jwtUtil, BCryptPasswordEncoder passwordEncoder,
            RedisOTPService redisOtpService, RateLimitService rateLimitService,
            otpService OtpService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.redisOtpService = redisOtpService;
        this.rateLimitService = rateLimitService;
        this.OtpService = OtpService;
    }

    // ═══════════════════════════════════════════════════════════
    // USER REGISTRATION
    // ═══════════════════════════════════════════════════════════
    public void register(registerRequest request) {
        // 1️⃣ Validate email doesn't exist
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new badRequestException("Email Already Registered");
        }

        // 2️⃣ Get role from database
        roles Role = roleRepo.findByName(request.getRole())
                .orElseThrow(() -> new badRequestException("Invalid Role"));

        // 3️⃣ Create new user entity
        user User = new user();
        User.setEmail(request.getEmail());
        User.setPassword(passwordEncoder.encode(request.getPassword()));  // 🔐 Hash password
        User.setName(request.getName());
        User.setPhoneNumber(request.getPhoneNumber());
        User.setRole(Role);

        // 4️⃣ Save to database
        userRepo.save(User);
    }

    // ═══════════════════════════════════════════════════════════
    // LOGIN WITH OTP
    // ═══════════════════════════════════════════════════════════
    public void login(loginRequest request) {
        // 1️⃣ Find user by email or phone
        user User = null;
        if (request.getEmail() != null) {
            User = userRepo.findByEmail(request.getEmail())
                    .orElseThrow(() -> new badRequestException("User Doesn't Exists"));
        } else if (request.getPhoneNumber() != null) {
            User = userRepo.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new badRequestException("User Doesn't Exists"));
        }

        // 2️⃣ Rate limiting (3 OTPs per 10 minutes)
        if (!rateLimitService.isAllowed("login_otp", User.getPhoneNumber(), 3, 10)) {
            throw new badRequestException("Too many OTP requests. Please wait 10 minutes.");
        }

        // 3️⃣ Verify password
        if (!passwordEncoder.matches(request.getPassword(), User.getPassword())) {
            throw new badRequestException("Invalid Password");
        }

        // 4️⃣ Generate and store OTP in Redis
        String otp = redisOtpService.generateAndStoreOTP("login", User.getPhoneNumber());

        // 5️⃣ Send OTP via SMS
        try {
            OtpService.sendSMSText(User.getPhoneNumber(), "Your OTP is: " + otp);
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
        }

        System.out.println("Login OTP: " + otp);
    }

    // ═══════════════════════════════════════════════════════════
    // VERIFY OTP AND GENERATE JWT
    // ═══════════════════════════════════════════════════════════
    public String verifyOtp(otpVerifyRequest request) {
        // 1️⃣ Find user by phone
        user User = userRepo.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new badRequestException("User not found"));

        // 2️⃣ Validate OTP from Redis
        if (!redisOtpService.validateOTP("login", User.getPhoneNumber(), request.getOtp())) {
            throw new badRequestException("Invalid or Expired OTP");
        }

        // 3️⃣ Generate JWT token
        String roleName = User.getRole() != null ? User.getRole().getName() : "USER";
        return jwtUtil.generateToken(User.getEmail(), roleName);
    }

    // ═══════════════════════════════════════════════════════════
    // CHANGE PASSWORD
    // ═══════════════════════════════════════════════════════════
    public void changePassword(String email, String oldPassword, String newPassword) {
        user User = userRepo.findByEmail(email)
                .orElseThrow(() -> new badRequestException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, User.getPassword())) {
            throw new badRequestException("Current password incorrect");
        }

        // Update with new password
        User.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(User);
    }

    // ═══════════════════════════════════════════════════════════
    // DELETE ACCOUNT (with cleanup)
    // ═══════════════════════════════════════════════════════════
    public void deleteAccount(Long userId) {
        user User = userRepo.findById(userId)
                .orElseThrow(() -> new badRequestException("User not found"));

        // Delete role-specific data
        if (User.getRole().getName().equals("LAWYER")) {
            // Delete lawyer profile and media
            lawyerRepo.findByUser(User).ifPresent(profile -> {
                ProfileImageService.deleteProfileImage(profile.getProfileImagePath());
                ProfileImageService.deleteProfileImage(profile.getVerificationDocumentPath());
                ProfileImageService.deleteProfileImage(profile.getVerificationVideoPath());
                lawyerRepo.delete(profile);
            });
        } else if (User.getRole().getName().equals("CLIENT")) {
            // Delete client profile
            clientRepo.findByUser(User).ifPresent(profile -> {
                ProfileImageService.deleteProfileImage(profile.getProfileImagePath());
                clientRepo.delete(profile);
            });
        }

        // Delete user profile image
        ProfileImageService.deleteProfileImage(User.getProfileImagePath());

        // Delete user
        userRepo.delete(User);
    }
}
```

---

# Chapter 6: Controllers & REST Endpoints

## 🎮 REST Controller Pattern

Controllers handle HTTP requests and responses.

### authController.java - Complete:

```java
@RestController
@RequestMapping("/api/auth")  // Base URL for all endpoints
public class authController {

    private final authService Authservice;
    private final tokenBlacklistService blacklistService;
    private final passwordResetService passwordService;
    private final RefreshTokenService refreshTokenService;

    public authController(authService AuthService, tokenBlacklistService blacklistService,
          passwordResetService passwordService, RefreshTokenService refreshTokenService) {
        this.Authservice = AuthService;
        this.blacklistService = blacklistService;
        this.passwordService = passwordService;
        this.refreshTokenService = refreshTokenService;
    }

    // ═══════════════════════════════════════════════════════════
    // POST /api/auth/register
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/register")
    public apiResponse<String> register(@RequestBody registerRequest request) {
        Authservice.register(request);
        return new apiResponse<String>("User registered successfully", true, null);
    }

    // ═══════════════════════════════════════════════════════════
    // POST /api/auth/login
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/login")
    public apiResponse<dto.authResponse> login(@RequestBody loginRequest request) {
        // Verify credentials and send OTP
        Authservice.login(request);

        // Return response
        dto.authResponse response = new dto.authResponse();
        response.setMessage("OTP sent to your phone. Please verify to complete login.");
        return new apiResponse<>("Login initiated", true, response);
    }

    // ═══════════════════════════════════════════════════════════
    // POST /api/auth/verify-otp
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/verify-otp")
    public apiResponse<dto.authResponse> verifyOtp(@RequestBody otpVerifyRequest request) {
        // Verify OTP and get JWT token
        String token = Authservice.verifyOtp(request);
        String email = Authservice.getEmailFromPhone(request.getPhoneNumber());

        // Build response
        dto.authResponse response = new dto.authResponse();
        response.setToken(token);                    // Access token
        response.setEmail(email);
        response.setMessage("Login Successful");

        // Create refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(email);
        response.setRefreshToken(refreshToken.getToken());  // Refresh token

        return new apiResponse<>("Login Successful", true, response);
    }

    // ═══════════════════════════════════════════════════════════
    // POST /api/auth/refresh
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/refresh")
    public apiResponse<dto.authResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        // Use refresh token to get new access token
        String newAccessToken = refreshTokenService.refreshAccessToken(request.getRefreshToken());
        String email = JwtUtil.extractEmail(newAccessToken);

        dto.authResponse response = new dto.authResponse();
        response.setToken(newAccessToken);
        response.setEmail(email);
        response.setMessage("Token refreshed successfully");

        // Optionally rotate refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(email);
        response.setRefreshToken(refreshToken.getToken());

        return new apiResponse<>("Token refreshed successfully", true, response);
    }

    // ═══════════════════════════════════════════════════════════
    // POST /api/auth/logout
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/logout")
    public apiResponse<String> logout(@RequestBody LogoutRequest request) {
        // Revoke refresh token
        if (request.getRefreshToken() != null) {
            refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        }

        // Blacklist access token
        String auth = request.getAuthorization();
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            java.util.Date expiry = Authservice.getJwtExpiry(token);
            blacklistService.blacklistToken(token, expiry);
        }

        return new apiResponse<>("Logged out", true, null);
    }

    // ═══════════════════════════════════════════════════════════
    // POST /api/auth/forgot-password
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/forgot-password")
    public apiResponse<String> forgotPassword(@RequestBody forgotPasswordRequest request) {
        String token = passwordService.forgotPassword(request);
        return new apiResponse<>("Password reset link sent", true, token);
    }

    // ═══════════════════════════════════════════════════════════
    // POST /api/auth/reset-password
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/reset-password")
    public apiResponse<String> resetPassword(@RequestBody resetPasswordRequest request) {
        passwordService.resetPassword(request);
        return new apiResponse<>("Password reset successfully", true, null);
    }
}
```

### API Response Wrapper:

```java
public class apiResponse<T> {
    private String message;
    private boolean success;
    private T data;

    public apiResponse(String message, boolean success, T data) {
        this.message = message;
        this.success = success;
        this.data = data;
    }

    // Getters and setters
}
```

---

# Chapter 7: WebSocket Implementation (Real-time Communication)

## 🔌 WebSocket vs HTTP

| Feature | HTTP | WebSocket |
|---------|------|----------|
| Connection | Request-response | Persistent connection |
| Communication | Client must request | Server can push |
| Overhead | High (headers each request) | Low (minimal overhead) |
| Use case | REST APIs | Real-time (chat, notifications) |

## 📡 WebSocket Configuration

```java
@Configuration
@EnableWebSocketMessageBroker  // Enable WebSocket with message broker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for topics (for broadcasting)
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages from clients
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint clients connect to
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")  // Allow all origins (configure for production)
                .withSockJS();  // Enable SockJS fallback
    }
}
```

### Key Concepts:

| Concept | Description |
|---------|-------------|
| `STOMP` | Simple Text Oriented Messaging Protocol |
| `/topic` | Publish-subscribe (broadcast to many) |
| `/queue` | Point-to-point (send to one) |
| `/app` | Application destinations (handled by @MessageMapping) |

## 💬 Chat Controller (WebSocket)

```java
@Controller
public class ChatController {

    private final PaymentVerificationService paymentVerificationService;

    public ChatController(PaymentVerificationService paymentVerificationService) {
        this.paymentVerificationService = paymentVerificationService;
    }

    // Client sends to /app/chat
    @MessageMapping("/chat")
    @SendTo("/topic/messages")  // Broadcast to all subscribers
    public ChatMessage send(ChatMessage message) {
        // Verify payment before allowing chat
        if (message.getAppointmentId() != null && message.getSenderId() != null) {
            boolean hasAccess = paymentVerificationService.canAccessChat(
                    message.getAppointmentId(),
                    Long.parseLong(message.getSenderId()));

            if (!hasAccess) {
                throw new securityException("Payment required to access chat feature");
            }
        }

        // Broadcast message to all subscribers
        return message;
    }
}
```

### Chat Message Flow:

```
1. Client connects to WebSocket (/ws)
2. Client subscribes to /topic/messages
3. Client sends message to /app/chat
4. Server:
   - Validates payment access
   - Broadcasts to /topic/messages
5. All subscribers receive message
```

## 📹 Video Call Signaling (WebRTC)

```java
@Controller
public class VideoSignalController {

    @MessageMapping("/video/offer")
    @SendTo("/topic/video/offer")
    public VideoSignal handleOffer(VideoSignal signal) {
        return signal;  // Forward offer to target
    }

    @MessageMapping("/video/answer")
    @SendTo("/topic/video/answer")
    public VideoSignal handleAnswer(VideoSignal signal) {
        return signal;
    }

    @MessageMapping("/video/ice-candidate")
    @SendTo("/topic/video/ice-candidate")
    public VideoSignal handleIceCandidate(VideoSignal signal) {
        return signal;
    }
}
```

**WebRTC Signaling Flow:**
```
Caller → Create offer → Send via WebSocket → Callee
Callee → Create answer → Send via WebSocket → Caller
Both → Exchange ICE candidates → Direct P2P connection → Video/Audio stream
```

---

# Chapter 8: Configuration Files

## ⚙️ application.properties

```properties
# Server Configuration
server.port=8080

# Database Configuration (MySQL)
spring.datasource.url=jdbc:mysql://localhost:3306/expert_hub
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# JWT Configuration
jwt.secret=YourSuperSecretKeyMustBeLongEnoughForHS256Algorithm
jwt.expiration=3600000  # 1 hour in milliseconds

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379

# File Upload
file.upload-dir=./uploads
max.file.size=10MB

# SMS (Twilio)
twilio.account.sid=your_account_sid
twilio.auth.token=your_auth_token
twilio.phone.number=+1234567890

# Payment (Razorpay)
razorpay.key_id=your_key_id
razorpay.key_secret=your_key_secret
```

---

# Chapter 9: Best Practices & Design Patterns

## ✅ Best Practices Used in This Project

### 1. **Dependency Injection via Constructor**
```java
// ✅ GOOD - Constructor injection
@Service
public class authService {
    private final userRepository userRepo;
    private final roleRepository roleRepo;

    public authService(userRepository userRepo, roleRepository roleRepo) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
    }
}

// ❌ AVOID - Field injection
@Service
public class authService {
    @Autowired
    private userRepository userRepo;
}
```

### 2. **Layer Separation**
```
Controller     → Handle HTTP, validation
Service        → Business logic, transactions
Repository     → Database operations
Entity         → Database models only
```

### 3. **DTO Pattern**
```java
// Request DTO - what client sends
public class registerRequest {
    private String email;
    private String password;
    private String name;
    private String phoneNumber;
    private String role;
}

// Entity - database model
@Entity
public class user {
    // Different from DTO!
}
```

### 4. **Exception Handling**
```java
// Global exception handler
@RestControllerAdvice
public class GlobalExceptionHandling {

    @ExceptionHandler(badRequestException.class)
    public apiResponse<?> handleBadRequest(badRequestException ex) {
        return new apiResponse<>(ex.getMessage(), false, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public apiResponse<?> handleNotFound(ResourceNotFoundException ex) {
        return new apiResponse<>(ex.getMessage(), false, null);
    }
}
```

### 5. **Security Best Practices**
- ✅ BCrypt for password hashing
- ✅ JWT with short expiration
- ✅ Refresh tokens with rotation
- ✅ Rate limiting
- ✅ Input validation

### 6. **Code Quality**
- ✅ Lombok for boilerplate reduction
- ✅ Consistent naming conventions
- ✅ Proper package structure
- ✅ Single responsibility principle

---

## 🎯 Summary of Key Concepts

| Concept | Purpose | Key Classes |
|---------|---------|-------------|
| **JWT** | Stateless authentication | `JwtUtil`, `jwtAuthFilter` |
| **Refresh Tokens** | Token rotation | `RefreshTokenService` |
| **WebSocket** | Real-time communication | `WebSocketConfig`, `ChatController` |
| **Repositories** | Database abstraction | `userRepository`, `RefreshTokenRepo` |
| **Services** | Business logic | `authService`, `CallSessionService` |
| **Controllers** | HTTP handling | `authController`, `ChatController` |
| **Entities** | Database models | `user`, `Appointment`, `CallSession` |

---

## 🚀 How to Learn Effectively

### Step-by-Step Learning Path:

1. **Week 1: Foundation**
   - Understand project structure
   - Learn Maven/pom.xml
   - Study main application class
   - Read entity definitions

2. **Week 2: Authentication**
   - Study JWT flow
   - Implement login/logout
   - Understand Spring Security
   - Practice with Postman

3. **Week 3: Database**
   - Learn JPA annotations
   - Study repository patterns
   - Practice queries
   - Understand relationships

4. **Week 4: Services & Controllers**
   - Implement new endpoints
   - Add business logic
   - Handle exceptions
   - Write unit tests

5. **Week 5: Real-time Features**
   - Implement WebSocket chat
   - Study STOMP protocol
   - Add video call signaling
   - Test with Socket.io client

6. **Week 6: Integration**
   - Integrate payment gateway
   - Add SMS notifications
   - Implement caching with Redis
   - Deploy and monitor

---

## 📚 Additional Resources

### Official Documentation:
- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security](https://docs.spring.io/spring-security/reference/index.html)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [JWT.io](https://jwt.io/)
- [STOMP Protocol](https://stomp.github.io/)

### Recommended Books:
- "Spring in Action" by Craig Walls
- "Pro Spring 5" by Iuliana Cosmina
- "Modern Java in Action" by Raoul-Gabriel Urma

---

> **Note:** This guide covers the core concepts. For complete understanding, read the actual source code and experiment by making changes. The best way to learn is by doing!

---

*End of Guide*
