# Controller Tests Documentation

## CardControllerTest

### Overview
The `CardControllerTest` class provides comprehensive unit tests for the `CardController` class. These tests focus on testing the controller logic without requiring a full Spring context, making them fast and reliable.

### Test Approach
- **Unit Testing**: Tests the controller methods directly without Spring Boot test slices
- **Mocking**: Uses Mockito to mock dependencies (`CardService`, `UserService`, `Authentication`)
- **Static Method Mocking**: Uses `MockedStatic` to mock `SecurityUtils` static methods
- **No Spring Context**: Avoids complex Spring Security configuration issues

### Test Coverage
The test suite covers all major controller endpoints:

#### GET Endpoints (4 tests)
- `GET /api/v1/cards/{id}` - Success scenario
- `GET /api/v1/cards` - Success with pagination
- `GET /api/v1/cards` - Empty result
- `GET /api/v1/cards/{id}/balance` - Success scenario

#### POST Endpoints (3 tests)
- `POST /api/v1/cards` - Success scenario
- `POST /api/v1/cards/admin/create-for-user/{userId}` - Admin creates card for user
- `POST /api/v1/cards/{id}/request-block` - User requests card blocking

#### PUT Endpoints (3 tests)
- `PUT /api/v1/cards/{id}` - Success scenario
- `PUT /api/v1/cards/admin/{id}/block` - Admin blocks card
- `PUT /api/v1/cards/admin/{id}/activate` - Admin activates card

#### DELETE Endpoints (1 test)
- `DELETE /api/v1/cards/{id}` - Success scenario

#### Admin Endpoints (1 test)
- `GET /api/v1/cards/admin/user/{userId}` - Admin gets user's cards

### Key Features
1. **Security Mocking**: Properly mocks `SecurityUtils` static methods for authentication and authorization
2. **Service Mocking**: Mocks `CardService` and `UserService` to isolate controller logic
3. **Response Validation**: Verifies HTTP status codes, response bodies, and method calls
4. **Clean Test Data**: Uses `@BeforeEach` to set up consistent test data

### Test Structure
Each test follows the Given-When-Then pattern:
- **Given**: Set up test data and mock behaviors
- **When**: Execute the controller method
- **Then**: Verify the response and mock interactions

### Benefits
- **Fast Execution**: No Spring context loading
- **Isolated Testing**: Tests only controller logic
- **Reliable**: No database or external dependencies
- **Maintainable**: Clear test structure and good coverage

### Total Tests: 12
All tests pass successfully and provide comprehensive coverage of the CardController functionality.