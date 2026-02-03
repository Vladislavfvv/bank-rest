# Utilities

Utility classes: encryption, masking and other helper functions.

## Classes

### CardMaskingUtils
Provides secure masking of sensitive card information.
- `getMaskedNumber(Card card)` - Masks card number from encrypted card entity, showing only last 4 digits
- `getMaskedNumber(String cardNumber)` - Masks plain text card number, showing only last 4 digits
- Format: "**** **** **** 1234"

### EncryptionService
Handles encryption and decryption of sensitive data.

### SecurityUtils
Security-related utility functions for authentication and authorization.

### Mappers
- `CardMapper` - Maps between Card entities and DTOs
- `UserMapper` - Maps between User entities and DTOs  
- `TransferMapper` - Maps between Transfer entities and DTOs
