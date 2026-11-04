# JWT Authentication Flow: MERN vs Spring Boot

Yes! The flow is almost the same in both. The main difference is how Express handles the authenticated user versus how Spring Security stores it.

Let's use the same example in both: a user sends a request to `GET /profile` with a JWT.

## 1. The complete flow

1\. Frontend sends request

`Authorization: Bearer <token>`

2\. Extract the token

Remove the `Bearer ` prefix

3\. Verify the JWT

Check signature and expiration

4\. Find the user in the database

Use the email or user ID from the verified token

5\. Store authentication information

MERN: `req.user` · Spring: `SecurityContextHolder`

6\. Execute the route/controller

Return the profile or reject the request

If token verification fails, the request is rejected. If the user doesn't exist, the request is also rejected.

## 2. MERN implementation (Node.js + Express + MongoDB)

In Express, you usually create an authentication middleware that runs before your route handler.

### Step A: Authentication middleware

```
const jwt = require("jsonwebtoken");
const User = require("./models/User");

async function authMiddleware(req, res, next) {
  try {
    // 1. Get token from Authorization header
    const header = req.headers.authorization;

    if (!header || !header.startsWith("Bearer ")) {
      return res.status(401).json({ message: "No token provided" });
    }

    // 2. Extract token
    const token = header.split(" ")[1];

    // 3. Verify token
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    // 4. Find user in MongoDB
    const user = await User.findById(decoded.userId);

    if (!user) {
      return res.status(401).json({ message: "User not found" });
    }

    // 5. Attach user to request
    req.user = user;

    // 6. Continue to route
    next();

  } catch (error) {
    return res.status(401).json({ message: "Invalid or expired token" });
  }
}

module.exports = authMiddleware;
```

### Step B: Protected route

```
const express = require("express");
const authMiddleware = require("./authMiddleware");

const app = express();

app.get("/profile", authMiddleware, (req, res) => {
  // req.user was set by the middleware
  res.json({
    email: req.user.email,
    role: req.user.role
  });
});
```

Notice this line:

```
req.user = user;
```

This is the important part. The middleware attaches the database user to the request, and the route can access it through `req.user`.

The `next()` function tells Express to continue to the next middleware or route handler.

## 3. Spring Boot implementation (Java + Spring Security)

Now let's implement the same logic in Spring Boot. We'll assume `JwtUtil` can extract the email and validate the token, and `CustomUserDetailsService` loads the user from the database.

### Step A: JWT authentication filter

Spring commonly uses `OncePerRequestFilter` for this job.

```
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtFilter(
            JwtUtil jwtUtil,
            CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Get Authorization header
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract token
        String token = header.substring(7);

        try {
            // 3. Extract email from verified token
            String email = jwtUtil.extractEmail(token);

            // 4. Load user from database
            UserDetails userDetails =
                userDetailsService.loadUserByUsername(email);

            // 5. Validate token and create authentication
            if (jwtUtil.validateToken(token, userDetails)
                    && SecurityContextHolder.getContext()
                       .getAuthentication() == null) {

                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                // 6. Store authentication for this request
                SecurityContextHolder.getContext()
                                     .setAuthentication(auth);
            }

        } catch (JwtException | IllegalArgumentException e) {
            // In a real project, handle invalid tokens
            // according to the configured security policy.
        }

        // 7. Continue through Spring Security's filter chain
        filterChain.doFilter(request, response);
    }
}
```

This is a simplified illustrative filter. It assumes `JwtUtil` verifies the signature and expiration, and the filter is registered in the Spring Security chain. Production error handling should ensure invalid bearer tokens are rejected rather than accidentally treated as anonymous requests.

### Step B: Protected controller

```
@RestController
public class ProfileController {

    @GetMapping("/profile")
    public Map<String, Object> getProfile(
            @AuthenticationPrincipal UserDetails user) {

        return Map.of(
            "email", user.getUsername(),
            "authorities", user.getAuthorities()
        );
    }
}
```

`@AuthenticationPrincipal` obtains the principal from the authentication already stored in Spring Security's context.

In your project, the filter uses:

```
SecurityContextHolder.getContext()
                     .setAuthentication(auth);
```

This is the key difference from Express. You generally don't manually set `req.user`; Spring Security manages the authentication object in its security context.

## 4. Side-by-side: remember these lines

| What happens             | Express / MERN              | Spring Boot                                                  |
| ------------------------ | --------------------------- | ------------------------------------------------------------ |
| Read header              | `req.headers.authorization` | `request.getHeader("Authorization")`                         |
| Extract token            | `header.split(" ")[1]`      | `header.substring(7)`                                        |
| Verify token             | `jwt.verify(token, secret)` | `jwtUtil.validateToken(...)`                                 |
| Load database user       | `User.findById(...)`        | `userDetailsService.loadUserByUsername(...)`                 |
| Store authenticated user | `req.user = user`           | `SecurityContextHolder.getContext().setAuthentication(auth)` |
| Continue request         | `next()`                    | `filterChain.doFilter(request, response)`                    |
| Handle protected route   | `req.user`                  | `@AuthenticationPrincipal`                                   |

## 5. What happens when the request comes out?

For a valid request:

1. The middleware or filter authenticates the user.
2. The route handler or controller executes.
3. The application returns a response, such as the user's profile.
4. Express or Spring sends the response to the frontend.

For an invalid or expired token, the application should return HTTP 401 Unauthorized. If the user is authenticated but lacks the required permissions, it should return HTTP 403 Forbidden.

One final detail: in Spring Boot, make sure `JwtUtil` verifies the token's signature and expiration before trusting its email claim. The database lookup identifies the current user; the token alone should not be treated as proof until its signature and validity have been checked.

The easiest way to remember it: MERN attaches the user to `req.user`; Spring Boot puts authentication into `SecurityContextHolder`. Both then let the protected route use the authenticated identity.
