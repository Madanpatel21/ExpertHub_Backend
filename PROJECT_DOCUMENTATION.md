# Expert Hub API Documentation

## Complete Project Workflow for Clients

---

## Table of Contents
1. [Authentication Workflow](#authentication-workflow)
2. [User Registration & Login](#user-registration--login)
3. [Finding a Lawyer](#finding-a-lawyer)
4. [Booking an Appointment](#booking-an-appointment)
5. [Payments](#payments)
6. [Video/Audio Calls](#videoaudio-calls)
7. [Chat System](#chat-system)
8. [Subscription Management](#subscription-management)
9. [Reviews & Ratings](#reviews--ratings)
10. [Support & Notifications](#support--notifications)

---

## Authentication Workflow

### User Registration & Login

```
┌─────────────────────────────────────────────────────────────────┐
│                    AUTHENTICATION FLOW                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────┐    OTP Sent    ┌─────────────────────────┐    │
│   │  Register   │ ─────────────► │   Verify OTP            │    │
│   │  /register  │                │   /verify-otp           │    │
│   └─────────────┘                └─────────────────────────┘    │
│          │                               │                      │
│          │                               ▼                      │
│   User created with               JWT Token +                   │
│   role CLIENT/LAWYER              Refresh Token                 │
│                                                                 │
│   ┌─────────────┐    OTP Sent    ┌─────────────────────────┐    │
│   │   Login     │ ─────────────► │   Verify OTP            │    │
│   │   /login    │                │   /verify-otp           │    │
│   └─────────────┘                └─────────────────────────┘    │
│          │                               │                      │
│          │                               ▼                      │
│   Credentials                         JWT Token +               │
│   Verified                           Refresh Token              │
│                                                                 │
│   ┌─────────────┐                ┌─────────────────────────┐    │
│   │  Logout     │                │   Refresh Token         │    │
│   │  /logout    │                │   /refresh              │    │
│   └─────────────┘                └─────────────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Authentication Endpoints

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/auth/register` | POST | Register new user | ❌ No |
| `/api/auth/login` | POST | Initiate login (sends OTP) | ❌ No |
| `/api/auth/verify-otp` | POST | Verify OTP and get JWT token | ❌ No |
| `/api/auth/refresh` | POST | Refresh access token | ❌ No |
| `/api/auth/logout` | POST | Logout current session | ✅ Yes |
| `/api/auth/logout-all` | POST | Logout from all devices | ✅ Yes |
| `/api/auth/forgot-password` | POST | Request password reset | ❌ No |
| `/api/auth/reset-password` | POST | Reset password with token | ❌ No |

---

## User Registration & Login

### Registration Flow
```
1. Client sends: { email, phoneNumber, password, role (CLIENT/LAWYER) }
2. System creates user with PENDING verification status
3. Returns: "User registered successfully"
```

### Login Flow
```
1. Client sends: { email, phoneNumber, password }
2. System verifies credentials
3. System sends OTP to phone
4. Client verifies OTP with: { phoneNumber, otp }
5. System returns: JWT Token + Refresh Token
```

### JWT Token Usage
```
Header: Authorization: Bearer <JWT_TOKEN>
```

---

## Finding a Lawyer

### Search Flow
```
┌─────────────────────────────────────────────────────────────────┐
│                    LAWYER SEARCH WORKFLOW                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────────┐                                           │
│   │   Search Lawyers│                                           │
│   │POST /api/lawyers│                                           │
│   └────────┬────────┘                                           │
│            │                                                    │
│            ▼                                                    │
│   ┌─────────────────┐     ┌─────────────────┐                   │
│   │   Filter by     │────►│   Get Results   │                   │
│   │   - City        │     │   - Profile     │                   │
│   │   - Practice    │     │   - Rating      │                   │
│   │   - Experience  │     │   - Reviews     │                   │
│   └─────────────────┘     └─────────────────┘                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Lawyer Search Endpoints

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/lawyers/search` | POST | Search lawyers with filters | ✅ Yes |
| `/api/lawyers/{id}` | GET | Get lawyer profile details | ✅ Yes |

### Search Request Body
```json
{
  "city": "Colombo",
  "practiceArea": "Criminal Law",
  "minExperience": 5,
  "maxFees": 5000,
  "sortBy": "rating"
}
```

### Lawyer Profile Includes
- Full Name
- Bar Council Number
- Years of Experience
- Practice Area
- City
- Consultation Fees
- **Verification Status** ✅
- Average Rating ⭐
- Total Reviews
- Profile Image

---

## Booking an Appointment

### Appointment Workflow
```
┌─────────────────────────────────────────────────────────────────┐
│                  APPOINTMENT BOOKING FLOW                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  CLIENT                    SYSTEM                    LAWYER     │
│    │                          │                          │      │
│    │  1. Book Appointment     │                          │      │
│    ├─────────────────────────►│                          │      │
│    │                          │                          │      │
│    │  2. Status: PENDING      │                          │      │
│    │◄─────────────────────────┤                          │      │
│    │                          │                          │      │
│    │                          │  3. Notify Lawyer        │      │
│    │                          ├─────────────────────────►│      │
│    │                          │                          │      │
│    │                          │         4. Approve/Reject│      │
│    │                          │◄─────────────────────────┤      │
│    │                          │                          │      │
│    │  5. Status Updated       │                          │      │
│    │◄─────────────────────────┤                          │      │
│    │                          │                          │      │
│    │  6. Make Payment         │                          │      │
│    │  (if approved)           │                          │      │
│    │                          │                          │      │
│    │  7. Video/Chat Session   │                          │      │
│    │                          │                          │      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Client Appointment Endpoints

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/client/appointments` | POST | Book new appointment | ✅ Yes |
| `/api/client/appointments` | GET | Get my appointments | ✅ Yes |
| `/api/client/appointments/{id}/cancel` | PUT | Cancel appointment | ✅ Yes |
| `/api/client/appointments/{id}/reschedule` | PUT | Reschedule appointment | ✅ Yes |

### Lawyer Appointment Endpoints

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/lawyer/appointments` | GET | Get my appointments | ✅ Yes |
| `/api/lawyer/appointments/{id}/approve` | PUT | Approve appointment | ✅ Yes |
| `/api/lawyer/appointments/{id}/reject` | PUT | Reject appointment | ✅ Yes |

### Appointment Status Flow
```
PENDING ──► APPROVED ──► PAID ──► COMPLETED
   │          │           │
   │          │           │
   ▼          ▼           ▼
CANCELLED  REJECTED    CANCELLED
```

---

## Payments

### Payment Workflow
```
┌─────────────────────────────────────────────────────────────────┐
│                      PAYMENT FLOW                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   1. Create Order                                               │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ POST /api/payments/create-order                         │   │
│   │ { amount, currency, appointmentId, userId }             │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   2. Razorpay Order Created                                     │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ { orderId, amount, currency }                           │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   3. Client Pays via Razorpay UI                                │
│            │                                                    │
│            ▼                                                    │
│   4. Mark Appointment as Paid                                   │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ PUT /api/appointments/{id}/mark-paid                    │   │
│   │ { paymentId }                                           │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   5. Chat/Video Call Enabled                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Payment Endpoints

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/payments/create-order` | POST | Create Razorpay payment order | ✅ Yes |
| `/api/payments/verify` | POST | Verify payment signature | ✅ Yes |
| `/api/payments/retry-verify/{transactionId}` | POST | Retry verification | ✅ Yes |
| `/api/appointments/{id}/mark-paid` | PUT | Mark appointment as paid | ✅ Yes |
| `/api/appointments/{id}/payment-status` | GET | Check payment status | ✅ Yes |
| `/api/appointments/{id}/verify-access` | GET | Verify chat/call access | ✅ Yes |

---

## Video/Audio Calls

### Call Workflow
```
┌─────────────────────────────────────────────────────────────────┐
│                    VIDEO/AUDIO CALL FLOW                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   PREREQUISITE: Appointment must be PAID                        │
│                                                                 │
│   1. Start Call                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ POST /api/calls/start                                   │   │
│   │ { calleeId, appointmentId, callType (VIDEO/AUDIO) }     │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   2. Call Session Created (Status: INITIATED)                   │
│            │                                                    │
│            ▼                                                    │
│   3. Callee Connects                                            |
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ PUT /api/calls/{id}/connect                             │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   4. Call in Progress (Status: CONNECTED)                       │
│                                                                 │
│   5. End Call                                                   │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ PUT /api/calls/{id}/end                                 │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Call Endpoints

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/calls/start` | POST | Start audio/video call | ✅ Yes |
| `/api/calls/{id}/connect` | PUT | Connect to call | ✅ Yes |
| `/api/calls/{id}/end` | PUT | End call | ✅ Yes |
| `/api/calls/{id}/cancel` | PUT | Cancel call | ✅ Yes |
| `/api/calls/{id}/missed` | PUT | Mark as missed | ✅ Yes |
| `/api/calls/history` | GET | Get call history | ✅ Yes |

### WebSocket for Real-time Signaling
```
Endpoint: /ws/video
Topic: /topic/signal/{callSessionId}
```

---

## Chat System

### Chat Workflow
```
┌─────────────────────────────────────────────────────────────────┐
│                       CHAT WORKFLOW                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   PREREQUISITE: Appointment must be PAID                        │
│                                                                 │
│   1. Connect to WebSocket                                       │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ Endpoint: /ws/chat                                      │   │
│   │ Subscribe: /topic/messages                              │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   2. Send Message                                               │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ MessageMapping: /app/chat                               │   │
│   │ { appointmentId, senderId, content, timestamp }         │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   3. Receive Message (All participants)                         │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ @SendTo: /topic/messages                                │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Chat Security
- **Payment Verification Required**: Only clients with PAID appointments can access chat
- Real-time message delivery via WebSocket
- Chat messages stored for future reference

---

## Subscription Management

### Subscription Workflow
```
┌─────────────────────────────────────────────────────────────────┐
│                  SUBSCRIPTION MANAGEMENT FLOW                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   1. Subscribe                                                  │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ POST /api/subscriptions/create                          │   │
│   │ { planId, customerId, totalCount }                      │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   2. Subscription Created (Status: ACTIVE)                      │
│            │                                                    │
│            ▼                                                    │
│   3. Access Granted                                             │
│                                                                 │
│   ──────────────────────────────────────────────────────────    │
│                                                                 │
│   4. Cancel Subscription (Anytime)                              │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ POST /api/subscriptions/{subscriptionId}/cancel         │   │
│   │ { cancelImmediately: true/false, requestRefund: true }  │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   Options:                                                      │
│   - Cancel Immediately: Access revoked now                      │
│   - Cancel End of Period: Access until billing period ends      │
│                                                                 │
│   5. Request Refund                                             │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ POST /api/subscriptions/{subscriptionId}/refund         │   │
│   │ { reason }                                              │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Subscription Endpoints

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/subscriptions/create` | POST | Create new subscription | ✅ Yes |
| `/api/subscriptions/{id}/cancel` | POST | Cancel subscription | ✅ Yes |
| `/api/subscriptions/{id}/refund` | POST | Request refund | ✅ Yes |
| `/api/subscriptions/{subscriptionId}` | GET | Get subscription details | ✅ Yes |
| `/api/subscriptions/active` | GET | Get active subscriptions | ✅ Yes |
| `/api/subscriptions` | GET | Get all subscriptions | ✅ Yes |
| `/api/subscriptions/check-active` | GET | Check active status | ✅ Yes |
| `/api/subscriptions/{subscriptionId}` | DELETE | Delete/cancel subscription | ✅ Yes |

### Subscription Status
```
ACTIVE ──► CANCELLED ──► REFUNDED
   │           │
   │           └──► PENDING_REFUND (if refund requested)
   │
   └──► EXPIRED (when period ends)
```

---

## Reviews & Ratings

### Review Workflow
```
┌─────────────────────────────────────────────────────────────────┐
│                    REVIEWS & RATINGS FLOW                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   PREREQUISITE: Appointment must be COMPLETED                   │
│                                                                 │
│   1. Submit Review                                              │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ POST /api/reviews                                       │   │
│   │ { lawyerId, rating, comment }                           │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   2. Review Saved                                               │
│            │                                                    │
│            ▼                                                    │
│   3. Lawyer's Average Rating Updated                            │
│                                                                 │
│   ──────────────────────────────────────────────────────────    │
│                                                                 │
│   4. View Reviews                                               │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ GET /api/reviews/lawyer/{lawyerId}                      │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   5. View Rating                                                │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ GET /api/reviews/lawyer/{lawyerId}/rating               │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Review Endpoints

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/reviews` | POST | Submit review | ✅ Yes |
| `/api/reviews/lawyer/{lawyerId}` | GET | Get lawyer reviews | ✅ Yes |
| `/api/reviews/lawyer/{lawyerId}/rating` | GET | Get lawyer rating | ✅ Yes |

---

## Support & Notifications

### Support Workflow
```
┌─────────────────────────────────────────────────────────────────┐
│                    SUPPORT TICKET FLOW                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   1. Create Ticket                                              │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ POST /api/support/ticket                                │   │
│   │ { subject, message, category }                          │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   2. Ticket Created                                             │
│            │                                                    │
│            ▼                                                    │
│   3. View My Tickets                                            │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ GET /api/support/my-tickets                             │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Support Endpoints

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/support/ticket` | POST | Create support ticket | ✅ Yes |
| `/api/support/my-tickets` | GET | Get my tickets | ✅ Yes |

### Notification Endpoints

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/notifications/{id}/read` | PUT | Mark notification as read | ✅ Yes |
| `/api/notifications/read-all` | PUT | Mark all as read | ✅ Yes |

---

## Lawyer Verification Workflow

### Professional Verification Process
```
┌─────────────────────────────────────────────────────────────────┐
│                  LAWYER VERIFICATION FLOW                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   1. Lawyer Registers                                           │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ POST /api/lawyer/profile/{userId}                       │   │
│   │ { fullName, barCouncilNumber, experience, etc }         │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   2. Profile Created (verified = false)                         │
│            │                                                    │
│            ▼                                                    │
│   3. Upload Verification Document                               │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ POST /api/lawyer/profile/{userId}/upload-verification-  │   │
│   │ doc                                                     │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   4. Upload Selfie Video                                        │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │ POST /api/lawyer/profile/{userId}/upload-verification-  │   │
│   │ video                                                   │   │
│   └─────────────────────────────────────────────────────────┘   │
│            │                                                    │
│            ▼                                                    │
│   5. Company Reviews & Approves (External)                      │
│            │                                                    │
│            ▼                                                    │
│   6. Lawyer Marked as Verified (verified = true)                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Lawyer Profile Endpoints

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/lawyer/profile/{userId}` | POST | Create profile | ✅ Yes |
| `/api/lawyer/profile/{profileId}` | GET | Get profile | ✅ Yes |
| `/api/lawyer/profile/{userId}/upload-image` | POST | Upload profile image | ✅ Yes |
| `/api/lawyer/profile/{userId}/update-image` | PUT | Update profile image | ✅ Yes |
| `/api/lawyer/profile/{userId}/delete-image` | DELETE | Delete profile image | ✅ Yes |
| `/api/lawyer/profile/{userId}/upload-verification-doc` | POST | Upload verification document | ✅ Yes |
| `/api/lawyer/profile/{userId}/upload-verification-video` | POST | Upload selfie video | ✅ Yes |

---

## Complete API Summary

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Initiate login (OTP sent) |
| POST | `/api/auth/verify-otp` | Verify OTP & get token |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Logout |
| POST | `/api/auth/logout-all` | Logout from all devices |
| POST | `/api/auth/forgot-password` | Request password reset |
| POST | `/api/auth/reset-password` | Reset password |

### User Profiles
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/user/profile` | Get current user profile |
| PUT | `/api/user/profile` | Update profile |
| PUT | `/api/user/settings` | Update settings |

### Lawyer Profiles
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/lawyer/profile/{userId}` | Create lawyer profile |
| GET | `/api/lawyer/profile/{profileId}` | Get profile |
| POST | `/api/lawyer/profile/{userId}/upload-image` | Upload image |
| POST | `/api/lawyer/profile/{userId}/upload-verification-doc` | Upload verification document |
| POST | `/api/lawyer/profile/{userId}/upload-verification-video` | Upload selfie video |

### Lawyer Search
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/lawyers/search` | Search lawyers |
| GET | `/api/lawyers/{id}` | Get lawyer details |

### Appointments (Client)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/client/appointments` | Book appointment |
| GET | `/api/client/appointments` | Get my appointments |
| PUT | `/api/client/appointments/{id}/cancel` | Cancel appointment |
| PUT | `/api/client/appointments/{id}/reschedule` | Reschedule |

### Appointments (Lawyer)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/lawyer/appointments` | Get my appointments |
| PUT | `/api/lawyer/appointments/{id}/approve` | Approve |
| PUT | `/api/lawyer/appointments/{id}/reject` | Reject |

### Payments
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/create-order` | Create payment order |
| POST | `/api/payments/verify` | Verify payment |
| PUT | `/api/appointments/{id}/mark-paid` | Mark as paid |
| GET | `/api/appointments/{id}/payment-status` | Check status |

### Subscriptions
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/subscriptions/create` | Create subscription |
| POST | `/api/subscriptions/{id}/cancel` | Cancel subscription |
| POST | `/api/subscriptions/{id}/refund` | Request refund |
| GET | `/api/subscriptions/active` | Get active subscriptions |
| GET | `/api/subscriptions/check-active` | Check active status |

### Video Calls
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/calls/start` | Start call |
| PUT | `/api/calls/{id}/connect` | Connect call |
| PUT | `/api/calls/{id}/end` | End call |

### Chat
| Method | Endpoint | Description |
|--------|----------|-------------|
| WebSocket | `/ws/chat` | Real-time messaging |
| MessageMapping | `/app/chat` | Send message |
| Subscribe | `/topic/messages` | Receive messages |

### Reviews
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/reviews` | Submit review |
| GET | `/api/reviews/lawyer/{lawyerId}` | Get reviews |
| GET | `/api/reviews/lawyer/{lawyerId}/rating` | Get rating |

### Support
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/support/ticket` | Create ticket |
| GET | `/api/support/my-tickets` | Get my tickets |

### Notifications
| Method | Endpoint | Description |
|--------|----------|-------------|
| PUT | `/api/notifications/{id}/read` | Mark as read |
| PUT | `/api/notifications/read-all` | Mark all read |

---

## Error Handling

### Standard Error Response
```json
{
  "message": "Error description",
  "success": false,
  "data": null
}
```

### Common HTTP Status Codes
| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 500 | Internal Server Error |

---

## Security

### JWT Authentication
- All protected endpoints require: `Authorization: Bearer <JWT_TOKEN>`
- Tokens expire after 24 hours
- Refresh tokens allow getting new access tokens

### Role-Based Access
| Role | Access |
|------|--------|
| CLIENT | Book appointments, make payments, chat, video calls |
| LAWYER | Manage appointments, video calls, reviews |
| ADMIN | Full access |

### Payment Verification
- All payments verified via Razorpay signature
- Chat and video calls require PAID appointment status
- Subscription required for some premium features
