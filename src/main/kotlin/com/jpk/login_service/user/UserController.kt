package com.jpk.login_service.user

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @GetMapping
    fun listUsers(): ResponseEntity<com.jpk.login_service.common.ApiResponse<List<UserResponse>>> =
            ResponseEntity.ok(
                    com.jpk.login_service.common.ApiResponse(
                            statusCode = 200,
                            statusMessage = com.jpk.login_service.common.Messages.USER_LIST,
                            data = userService.listUsers()
                    )
            )

    @PostMapping
    fun addUser(
            @Valid @RequestBody req: CreateUserRequest
    ): ResponseEntity<com.jpk.login_service.common.ApiResponse<UserResponse>> =
            ResponseEntity.ok(
                    com.jpk.login_service.common.ApiResponse(
                            statusCode = 200,
                            statusMessage = com.jpk.login_service.common.Messages.USER_CREATED,
                            data = userService.createUser(req)
                    )
            )

    @PostMapping("/update")
    fun updateUser(
            @Valid @RequestBody req: UpdateUserByEmailRequest
    ): ResponseEntity<com.jpk.login_service.common.ApiResponse<UserResponse>> =
            ResponseEntity.ok(
                    com.jpk.login_service.common.ApiResponse(
                            statusCode = 200,
                            statusMessage = com.jpk.login_service.common.Messages.USER_UPDATED,
                            data = userService.updateUserByEmail(req)
                    )
            )

    @PostMapping("/delete")
    fun deleteUser(
            @Valid @RequestBody req: DeleteUserRequest
    ): ResponseEntity<com.jpk.login_service.common.ApiResponse<Void>> {
        userService.deleteUserByEmail(req.email)
        return ResponseEntity.ok(
                com.jpk.login_service.common.ApiResponse(
                        statusCode = 200,
                        statusMessage = com.jpk.login_service.common.Messages.USER_DELETED,
                        data = null
                )
        )
    }
}
