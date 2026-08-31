package gameservermanager.shared.error

import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(GameServerAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleAlreadyExists(exception: GameServerAlreadyExistsException): ApiValidationError {
        return ApiValidationError(mapOf("request" to "${exception.game}サーバーは既に作成されています"))
    }
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(exception: MethodArgumentNotValidException): ApiValidationError {
        return ApiValidationError(
            exception.bindingResult.fieldErrors
                .associate { it.field to (it.defaultMessage ?: "入力内容を確認してください") },
        )
    }

    @ExceptionHandler(InvalidConstructionPlanException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidPlan(exception: InvalidConstructionPlanException): ApiValidationError {
        return ApiValidationError(exception.errors)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(exception: IllegalArgumentException): ApiValidationError {
        return ApiValidationError(mapOf("request" to (exception.message ?: "入力内容を確認してください")))
    }
}

class GameServerAlreadyExistsException(
    val game: String,
) : RuntimeException()

class InvalidConstructionPlanException(
    val errors: Map<String, String>,
) : RuntimeException()
