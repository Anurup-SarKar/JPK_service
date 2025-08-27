# Test script for create user endpoint

# First, generate a SHA-256 hash (this would normally be done by frontend)
# For "TestPassword123!" the SHA-256 hash is: a2c7d4f8a4c8c7f8d8c9c0c1c2c3c4c5c6c7c8c9c0c1c2c3c4c5c6c7c8c9c0c1
# Let's use a simpler example: "password" = 5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8

echo "Testing Create User API..."

# Test with valid SHA-256 hash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser123",
    "email": "test@example.com",
    "passwordHash": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
    "fullName": "Test User",
    "mobile": "1234567890",
    "isCctvVisible": false,
    "isCctvStorageVisible": false,
    "isAdmin": false,
    "isActive": true
  }'

echo ""
echo ""

# Test with invalid hash (too short) - should fail validation
echo "Testing with invalid hash (too short)..."
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser456",
    "email": "test2@example.com",
    "passwordHash": "tooshort",
    "fullName": "Test User 2"
  }'

echo ""
echo ""

# Test with invalid hash (not hex) - should fail validation
echo "Testing with invalid hash (not hex)..."
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser789",
    "email": "test3@example.com", 
    "passwordHash": "PlainPassword123!",
    "fullName": "Test User 3"
  }'
