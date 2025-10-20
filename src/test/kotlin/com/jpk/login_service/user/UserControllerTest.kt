package com.jpk.login_service.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.jpk.login_service.common.GlobalExceptionHandler
import com.jpk.login_service.common.Messages
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(UserController::class)
@Import(GlobalExceptionHandler::class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var userService: UserService

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `listUsers should return list of users`() {
        val users = listOf(
            UserResponse(
                id = 1L,
                username = "user1",
                email = "user1@example.com",
                mobile = "+919999999991",
                fullName = "User One",
                cctvLink = null,
                isCctvVisible = false,
                isCctvStorageVisible = false,
                isAdmin = false,
                isActive = true
            ),
            UserResponse(
                id = 2L,
                username = "user2",
                email = "user2@example.com",
                mobile = "+919999999992",
                fullName = "User Two",
                cctvLink = null,
                isCctvVisible = false,
                isCctvStorageVisible = false,
                isAdmin = false,
                isActive = true
            )
        )

        whenever(userService.listUsers()).thenReturn(users)

        mockMvc.perform(
            get("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.statusMessage").value(Messages.USER_LIST))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].username").value("user1"))
            .andExpect(jsonPath("$.data[1].username").value("user2"))

        verify(userService).listUsers()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `addUser should create new user successfully`() {
        val createRequest = CreateUserRequest(
            username = "newuser",
            email = "newuser@example.com",
            mobile = "+919999999999",
            passwordHash = "a".repeat(64), // Valid SHA-256 hash format
            fullName = "New User",
            cctvLink = null,
            isCctvVisible = false,
            isCctvStorageVisible = false,
            isAdmin = false,
            isActive = true
        )

        val userResponse = UserResponse(
            id = 1L,
            username = "newuser",
            email = "newuser@example.com",
            mobile = "+919999999999",
            fullName = "New User",
            cctvLink = null,
            isCctvVisible = false,
            isCctvStorageVisible = false,
            isAdmin = false,
            isActive = true
        )

        whenever(userService.createUser(any())).thenReturn(userResponse)

        mockMvc.perform(
            post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.statusMessage").value(Messages.USER_CREATED))
            .andExpect(jsonPath("$.data.username").value("newuser"))
            .andExpect(jsonPath("$.data.email").value("newuser@example.com"))

        verify(userService).createUser(any())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `addUser should return error for duplicate email`() {
        val createRequest = CreateUserRequest(
            username = "newuser",
            email = "existing@example.com",
            mobile = "+919999999999",
            passwordHash = "a".repeat(64), // Valid SHA-256 hash format
            fullName = "New User",
            cctvLink = null,
            isCctvVisible = false,
            isCctvStorageVisible = false,
            isAdmin = false,
            isActive = true
        )

        whenever(userService.createUser(any()))
            .thenThrow(IllegalArgumentException("Email already exists"))

        mockMvc.perform(
            post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.statusMessage").value("Email already exists"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updateUser should update existing user successfully`() {
        val updateRequest = UpdateUserByEmailRequest(
            email = "user@example.com",
            username = "updateduser",
            mobile = "+919999999998",
            passwordHash = "b".repeat(64), // Valid SHA-256 hash format
            fullName = "Updated User",
            cctvLink = "http://cctv.example.com",
            isCctvVisible = true,
            isCctvStorageVisible = true,
            isAdmin = false,
            isActive = true
        )

        val userResponse = UserResponse(
            id = 1L,
            username = "updateduser",
            email = "user@example.com",
            mobile = "+919999999998",
            fullName = "Updated User",
            cctvLink = "http://cctv.example.com",
            isCctvVisible = true,
            isCctvStorageVisible = true,
            isAdmin = false,
            isActive = true
        )

        whenever(userService.updateUserByEmail(any())).thenReturn(userResponse)

        mockMvc.perform(
            post("/api/users/update")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.statusMessage").value(Messages.USER_UPDATED))
            .andExpect(jsonPath("$.data.username").value("updateduser"))
            .andExpect(jsonPath("$.data.mobile").value("+919999999998"))

        verify(userService).updateUserByEmail(any())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updateUser should return error for non-existent user`() {
        val updateRequest = UpdateUserByEmailRequest(
            email = "nonexistent@example.com",
            username = "updateduser",
            mobile = "+919999999998",
            passwordHash = null,
            fullName = "Updated User",
            cctvLink = null,
            isCctvVisible = false,
            isCctvStorageVisible = false,
            isAdmin = false,
            isActive = true
        )

        whenever(userService.updateUserByEmail(any()))
            .thenThrow(IllegalArgumentException("User not found"))

        mockMvc.perform(
            post("/api/users/update")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.statusMessage").value("User not found"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `deleteUser should delete user successfully`() {
        val deleteRequest = DeleteUserRequest(email = "user@example.com")

        doNothing().whenever(userService).deleteUserByEmail(any())

        mockMvc.perform(
            post("/api/users/delete")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.statusMessage").value(Messages.USER_DELETED))

        verify(userService).deleteUserByEmail("user@example.com")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `deleteUser should return error for non-existent user`() {
        val deleteRequest = DeleteUserRequest(email = "nonexistent@example.com")

        whenever(userService.deleteUserByEmail(any()))
            .thenThrow(IllegalArgumentException("User not found"))

        mockMvc.perform(
            post("/api/users/delete")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.statusMessage").value("User not found"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `listUsers should return empty list when no users exist`() {
        whenever(userService.listUsers()).thenReturn(emptyList())

        mockMvc.perform(
            get("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(0))

        verify(userService).listUsers()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `addUser should create admin user when isAdmin is true`() {
        val createRequest = CreateUserRequest(
            username = "adminuser",
            email = "admin@example.com",
            mobile = "+919999999999",
            passwordHash = "c".repeat(64), // Valid SHA-256 hash format
            fullName = "Admin User",
            cctvLink = null,
            isCctvVisible = false,
            isCctvStorageVisible = false,
            isAdmin = true,
            isActive = true
        )

        val userResponse = UserResponse(
            id = 1L,
            username = "adminuser",
            email = "admin@example.com",
            mobile = "+919999999999",
            fullName = "Admin User",
            cctvLink = null,
            isCctvVisible = false,
            isCctvStorageVisible = false,
            isAdmin = true,
            isActive = true
        )

        whenever(userService.createUser(any())).thenReturn(userResponse)

        mockMvc.perform(
            post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isAdmin").value(true))

        verify(userService).createUser(any())
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updateUser should allow partial updates without password`() {
        val updateRequest = UpdateUserByEmailRequest(
            email = "user@example.com",
            username = "updateduser",
            mobile = "+919999999998",
            passwordHash = null, // Password not being updated
            fullName = "Updated User",
            cctvLink = null,
            isCctvVisible = false,
            isCctvStorageVisible = false,
            isAdmin = false,
            isActive = true
        )

        val userResponse = UserResponse(
            id = 1L,
            username = "updateduser",
            email = "user@example.com",
            mobile = "+919999999998",
            fullName = "Updated User",
            cctvLink = null,
            isCctvVisible = false,
            isCctvStorageVisible = false,
            isAdmin = false,
            isActive = true
        )

        whenever(userService.updateUserByEmail(any())).thenReturn(userResponse)

        mockMvc.perform(
            post("/api/users/update")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.data.username").value("updateduser"))

        verify(userService).updateUserByEmail(any())
    }
}
