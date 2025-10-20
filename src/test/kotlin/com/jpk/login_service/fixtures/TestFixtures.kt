package com.jpk.login_service.fixtures

import com.jpk.login_service.auth.OtpEntry
import com.jpk.login_service.auth.PasswordResetToken
import com.jpk.login_service.user.User
import java.time.Instant

object TestFixtures {
    
    fun createUser(
        id: Long? = 1L,
        username: String = "testuser",
        email: String = "test@example.com",
        mobile: String? = "+919999999999",
        passwordHash: String = "\$2a\$10\$abcdefghijklmnopqrstuvwxyz1234567890",
        fullName: String? = "Test User",
        cctvLink: String? = null,
        isCctvVisible: Boolean = false,
        isCctvStorageVisible: Boolean = false,
        isAdmin: Boolean = false,
        isActive: Boolean = true
    ): User {
        return User(
            id = id,
            username = username,
            email = email,
            mobile = mobile,
            passwordHash = passwordHash,
            fullName = fullName,
            cctvLink = cctvLink,
            isCctvVisible = isCctvVisible,
            isCctvStorageVisible = isCctvStorageVisible,
            isAdmin = isAdmin,
            isActive = isActive
        )
    }
    
    fun createOtpEntry(
        id: Long? = 1L,
        username: String = "testuser",
        otp: String = "123456",
        expiresAt: Instant = Instant.now().plusSeconds(300),
        consumed: Boolean = false
    ): OtpEntry {
        return OtpEntry(
            id = id,
            username = username,
            otp = otp,
            expiresAt = expiresAt,
            consumed = consumed
        )
    }
    
    fun createPasswordResetToken(
        id: Long? = 1L,
        email: String = "test@example.com",
        selector: String = "testSelector",
        verifierHash: String = "testHash",
        expiresAt: Instant = Instant.now().plusSeconds(900),
        consumed: Boolean = false
    ): PasswordResetToken {
        return PasswordResetToken(
            id = id,
            email = email,
            selector = selector,
            verifierHash = verifierHash,
            expiresAt = expiresAt,
            consumed = consumed
        )
    }
}
