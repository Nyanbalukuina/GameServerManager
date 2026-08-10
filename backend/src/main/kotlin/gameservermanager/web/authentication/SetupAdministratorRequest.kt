package gameservermanager.web.authentication

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SetupAdministratorRequest(
    @field:NotBlank(message = "パスワードを入力してください")
    @field:Size(min = 12, max = 128, message = "パスワードは12文字以上128文字以内で入力してください")
    val password: String,
    @field:NotBlank(message = "確認用パスワードを入力してください")
    val passwordConfirmation: String,
)
