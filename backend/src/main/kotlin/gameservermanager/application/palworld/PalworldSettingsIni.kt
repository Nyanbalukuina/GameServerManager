package gameservermanager.application.palworld

object PalworldSettingsIni {
    fun read(content: String, keys: Collection<String>): LinkedHashMap<String, String> {
        require(content.contains("OptionSettings=(")) {
            "PalWorldSettings.iniのOptionSettingsが見つかりません"
        }
        return keys.associateTo(linkedMapOf()) { key -> key to decode(findRawValue(content, key)) }
    }

    fun update(content: String, values: Map<String, String>): String {
        require(content.contains("OptionSettings=(")) {
            "PalWorldSettings.iniのOptionSettingsが見つかりません"
        }
        var updated = content
        values.forEach { (key, value) ->
            val pattern = valuePattern(key)
            require(pattern.containsMatchIn(updated)) {
                "Palworld設定キー${key}が見つかりません"
            }
            updated = pattern.replace(updated) { "$key=$value" }
        }
        return updated
    }

    fun quoted(value: String): String {
        require(!value.contains('\n') && !value.contains('\r')) {
            "設定値に改行は使用できません"
        }
        return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }

    private fun findRawValue(content: String, key: String): String {
        val match = valuePattern(key).find(content)
        return requireNotNull(match) { "Palworld設定キー${key}が見つかりません" }
            .value.substringAfter('=')
    }

    private fun valuePattern(key: String): Regex {
        return Regex("(?<=[(,])${Regex.escape(key)}=(?:\"(?:\\\\.|[^\"])*\"|[^,)]*)")
    }

    private fun decode(value: String): String {
        if (!value.startsWith('"') || !value.endsWith('"')) return value
        val content = value.substring(1, value.length - 1)
        val result = StringBuilder()
        var escaped = false
        content.forEach { character ->
            if (escaped) {
                result.append(character)
                escaped = false
            } else if (character == '\\') {
                escaped = true
            } else {
                result.append(character)
            }
        }
        if (escaped) result.append('\\')
        return result.toString()
    }
}
