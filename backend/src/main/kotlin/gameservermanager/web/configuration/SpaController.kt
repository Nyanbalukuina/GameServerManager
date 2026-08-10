package gameservermanager.web.configuration

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class SpaController {
    @GetMapping(
        "/servers/new",
        "/servers/new/palworld",
        "/servers/palworld",
    )
    fun application(): String {
        return "forward:/index.html"
    }
}
