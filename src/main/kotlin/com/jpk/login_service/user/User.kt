package com.jpk.login_service.user

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "users")
class User(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
        @Column(nullable = false, unique = true, length = 50) var username: String,
        @Column(nullable = false, unique = true, length = 100) var email: String,
        @Column(unique = true, length = 15) var mobile: String? = null,
        @Column(name = "password_hash", nullable = false, length = 512) var passwordHash: String,
        @Column(name = "full_name", length = 100) var fullName: String? = null,
        @Column(name = "cctv_link", length = 255) var cctvLink: String? = null,
        @Column(name = "is_cctv_visible") var isCctvVisible: Boolean = false,
        @Column(name = "is_cctv_storage_visible") var isCctvStorageVisible: Boolean = false,
        @Column(name = "is_admin") var isAdmin: Boolean = false,
        @Column(name = "is_active") var isActive: Boolean = true,
        @Column(name = "created_at", updatable = false) var createdAt: Instant? = null,
        @Column(name = "updated_at") var updatedAt: Instant? = null,
) {
    @PrePersist
    fun prePersist() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
}
