# User API Implementation

## Overview

The User API provides functionality for user registration and retrieval. It follows the same architectural patterns as the Itinerary API with proper separation of concerns.

## User Stories Implemented

### Register User
- **As a traveller I can register to the site and create a profile**
- **Acceptance Criteria:**
  - No credential checking is implemented (password, etc.)
  - Traveller information contains email address and name

## API Endpoints

### Register User
```
POST /user/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john.doe@example.com"
}
```

**Response:**
- **200 OK**: User successfully registered
- **400 Bad Request**: User already exists or invalid data
- **500 Internal Server Error**: Server error

### Get User by Email
```
GET /user/get?email=john.doe@example.com
```

**Response:**
- **200 OK**: User found
- **400 Bad Request**: Email parameter missing
- **404 Not Found**: User not found
- **500 Internal Server Error**: Server error

## Architecture

### Components Created

1. **UserDto** (`de.htwg.api.user.model.UserDto`)
   - Data transfer object for API communication
   - Contains `name` and `email` fields

2. **UserMapper** (`de.htwg.api.user.mapper.UserMapper`)
   - Converts between DTO and Entity
   - Handles bidirectional mapping

3. **UserService** (`de.htwg.api.user.service.UserService`)
   - Interface defining user operations
   - Methods: `registerUser()`, `getUserByEmail()`

4. **UserServiceImpl** (`de.htwg.api.user.service.UserServiceImpl`)
   - Implementation using existing UserRepository
   - Handles business logic and validation
   - Prevents duplicate email registration

5. **UserApi** (`de.htwg.api.user.UserApi`)
   - REST endpoint controller
   - Handles HTTP requests and responses
   - Proper error handling and status codes

## Database Integration

The implementation uses the existing `UserRepository` from the persistence package:

```java
@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {
    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
}
```

## Business Logic

### User Registration
1. Check if user with email already exists
2. If exists, throw `IllegalArgumentException`
3. If not exists, create new user entity
4. Persist to database
5. Return user DTO

### User Retrieval
1. Search for user by email
2. If not found, throw `IllegalArgumentException`
3. If found, return user DTO

## Testing

A comprehensive test suite `UserServiceTest` has been created using Mockito:

- **testRegisterUser()** - Tests successful user registration
- **testRegisterUserWithExistingEmail()** - Tests duplicate email handling
- **testGetUserByEmail()** - Tests user retrieval
- **testGetUserByEmailNotFound()** - Tests user not found scenario

## Error Handling

The API provides proper HTTP status codes and error messages:

- **400 Bad Request**: Invalid input or duplicate email
- **404 Not Found**: User not found
- **500 Internal Server Error**: Unexpected server errors

## Usage Examples

### Register a New User
```bash
curl -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john.doe@example.com"}'
```

### Get User by Email
```bash
curl http://localhost:8080/user/get?email=john.doe@example.com
```

## Integration with Itinerary API

The User API works seamlessly with the existing Itinerary API:

1. Users register via `/user/register`
2. Users create itineraries via `/itinerary/create?userId={userId}`
3. Users retrieve their itineraries via `/itinerary/get?userId={userId}`

This creates a complete user journey from registration to itinerary management.
