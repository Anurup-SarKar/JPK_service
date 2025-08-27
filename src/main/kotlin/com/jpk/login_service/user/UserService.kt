package com.jpk.login_service.user

import com.jpk.login_service.common.Messages
import com.jpk.login_service.common.PasswordUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val userRepository: UserRepository) {

    fun listUsers(): List<UserResponse> = userRepository.findAll().map { it.toResponse() }

    @Transactional
    fun createUser(req: CreateUserRequest): UserResponse {
        if (userRepository.findByEmail(req.email).isPresent) {
            throw IllegalArgumentException(Messages.USER_ALREADY_EXISTS)
        }
        if (userRepository.findByUsername(req.username).isPresent) {
            throw IllegalArgumentException(Messages.USER_ALREADY_EXISTS)
        }
        val user =
                User(
                        username = req.username,
                        email = req.email,
                        mobile = req.mobile,
                        passwordHash = PasswordUtils.hashPassword(req.passwordHash), // BCrypt hash of SHA-256 hash from frontend
                        fullName = req.fullName,
                        cctvLink = req.cctvLink,
                        isCctvVisible = req.isCctvVisible,
                        isCctvStorageVisible = req.isCctvStorageVisible,
                        isAdmin = req.isAdmin,
                        isActive = req.isActive
                )
        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun updateUser(id: Long, req: UpdateUserRequest): UserResponse {
        val user =
                userRepository.findById(id).orElseThrow {
                    IllegalArgumentException(Messages.USER_NOT_FOUND)
                }
        req.username?.let { user.username = it }
        req.email?.let { user.email = it }
        req.mobile?.let { user.mobile = it }
        req.passwordHash?.let { user.passwordHash = PasswordUtils.hashPassword(it) }
        req.fullName?.let { user.fullName = it }
        req.cctvLink?.let { user.cctvLink = it }
        req.isCctvVisible?.let { user.isCctvVisible = it }
        req.isCctvStorageVisible?.let { user.isCctvStorageVisible = it }
        req.isAdmin?.let { user.isAdmin = it }
        req.isActive?.let { user.isActive = it }
        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun deleteUser(id: Long) {
        if (!userRepository.existsById(id)) throw IllegalArgumentException(Messages.USER_NOT_FOUND)
        userRepository.deleteById(id)
    }

    @Transactional
    fun deleteUserByEmail(email: String) {
        val user =
                userRepository.findByEmail(email).orElseThrow {
                    IllegalArgumentException(Messages.USER_NOT_FOUND)
                }
        userRepository.delete(user)
    }

    @Transactional
    fun updateUserByEmail(req: UpdateUserByEmailRequest): UserResponse {
        val user =
                userRepository.findByEmail(req.email).orElseThrow {
                    IllegalArgumentException(Messages.USER_NOT_FOUND)
                }
        req.username?.let { user.username = it }
        req.mobile?.let { user.mobile = it }
        req.passwordHash?.let { user.passwordHash = PasswordUtils.hashPassword(it) }
        req.fullName?.let { user.fullName = it }
        req.cctvLink?.let { user.cctvLink = it }
        req.isCctvVisible?.let { user.isCctvVisible = it }
        req.isCctvStorageVisible?.let { user.isCctvStorageVisible = it }
        req.isAdmin?.let { user.isAdmin = it }
        req.isActive?.let { user.isActive = it }
        return userRepository.save(user).toResponse()
    }
}

fun User.toResponse(): UserResponse =
        UserResponse(
                id = id,
                username = username,
                email = email,
                mobile = mobile,
                fullName = fullName,
                cctvLink = cctvLink,
                isCctvVisible = isCctvVisible,
                isCctvStorageVisible = isCctvStorageVisible,
                isAdmin = isAdmin,
                isActive = isActive
        )
