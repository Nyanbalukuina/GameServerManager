package gameservermanager.application.asa

// ASAのGameUserSettings.iniを更新する。
object AsaGameUserSettingsIni {
    // 指定されたセクションと設定値を更新し、存在しない設定は追加する。
    fun update(original: String, sections: Map<String, Map<String, String>>): String {
        val newline = if (original.contains("\r\n")) "\r\n" else "\n"
        val lines = original.replace("\r\n", "\n").split('\n').toMutableList()

        // 空文字から生成した不要な1行を削除する。
        if (lines.size == 1 && lines.first().isEmpty()) lines.clear()

        // セクションごとに既存設定を更新する。
        sections.forEach { (section, values) ->
            validateSection(section)
            values.forEach { (key, value) -> validateSetting(key, value) }
            updateSection(lines, section, values)
        }

        return lines.joinToString(newline).trimEnd() + newline
    }

    // 既存セクションを探し、設定値を更新または追加する。
    private fun updateSection(lines: MutableList<String>, section: String, values: Map<String, String>) {
        val header = "[$section]"
        val sectionStart = lines.indexOfFirst { it.trim().equals(header, ignoreCase = true) }

        // セクションがなければファイル末尾へ追加する。
        if (sectionStart == -1) {
            if (lines.isNotEmpty() && lines.last().isNotBlank()) lines += ""
            lines += header
            values.forEach { (key, value) -> lines += "$key=$value" }
            return
        }

        var sectionEnd = lines.indexOfFirstFrom(sectionStart + 1) { isSectionHeader(it) }
        if (sectionEnd == -1) sectionEnd = lines.size

        // 既存のキーは置き換え、存在しないキーはセクション末尾へ追加する。
        values.forEach { (key, value) ->
            val settingIndex = lines.indexOfFirstFrom(sectionStart + 1, sectionEnd) {
                it.substringBefore('=', "").trim().equals(key, ignoreCase = true)
            }

            if (settingIndex == -1) {
                lines.add(sectionEnd, "$key=$value")
                sectionEnd++
            } else {
                lines[settingIndex] = "$key=$value"
            }
        }
    }

    // セクション名として危険な文字が含まれていないことを確認する。
    private fun validateSection(section: String) {
        require(section.isNotBlank() && !section.contains('[') && !section.contains(']')) {
            "INIセクション名が不正です"
        }
        require(!section.contains('\n') && !section.contains('\r')) {
            "INIセクション名に改行は使用できません"
        }
    }

    // キーと値による不正な改行の挿入を防ぐ。
    private fun validateSetting(key: String, value: String) {
        require(key.isNotBlank() && !key.contains('=')) { "INI設定キーが不正です" }
        require(!key.contains('\n') && !key.contains('\r')) { "INI設定キーに改行は使用できません" }
        require(!value.contains('\n') && !value.contains('\r')) { "INI設定値に改行は使用できません" }
    }

    // 指定した開始位置以降から条件に合う行を探す。
    private fun List<String>.indexOfFirstFrom(start: Int, predicate: (String) -> Boolean): Int {
        for (index in start until size) {
            if (predicate(this[index])) return index
        }
        return -1
    }

    // 指定範囲から条件に合う行を探す。
    private fun List<String>.indexOfFirstFrom(
        start: Int,
        end: Int,
        predicate: (String) -> Boolean,
    ): Int {
        for (index in start until end) {
            if (predicate(this[index])) return index
        }
        return -1
    }

    // INIのセクション見出しか確認する。
    private fun isSectionHeader(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("[") && trimmed.endsWith("]")
    }
}