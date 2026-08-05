`package gameservermanager.web

import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(exception: MethodArgumentNotValidException): ApiValidationError =
        ApiValidationError(
            exception.bindingResult.fieldErrors
                .associate { it.field to (it.defaultMessage ?: "入力内容を確認してください") },
        )

    @ExceptionHandler(InvalidConstructionPlanException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidPlan(exception: InvalidConstructionPlanException): ApiValidationError =
        ApiValidationError(exception.errors)
}

class InvalidConstructionPlanException(
    val errors: Map<String, String>,
) : RuntimeException()

