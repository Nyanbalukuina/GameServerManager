package gameservermanager.shared.preflight

import gameservermanager.shared.preflight.PreflightCheck
import gameservermanager.shared.preflight.PreflightStatus
import gameservermanager.shared.preflight.ServerPreflightReport
import org.springframework.stereotype.Service

@Service
class RunServerPreflight(
    private val environmentInspector: ServerEnvironmentInspector,
    private val managedPathPolicy: ManagedPathPolicy,
) {
    fun execute(command: Command): ServerPreflightReport {
        val installPath = environmentInspector.inspectPath(command.installPath)
        val steamCmdPath = environmentInspector.inspectPath(command.steamCmdPath, "steamcmd.exe")
        val checks = buildList {
            addPathChecks("installPath", "インストール先", installPath)
            addPathChecks("steamCmdPath", "SteamCMD保存先", steamCmdPath)
            add(serverPathAllowed(command.installPath))
            add(toolPathAllowed(command.steamCmdPath))
            add(pathsDistinct(command))
            add(storageSpace(installPath))
            add(steamCmdInstallation(steamCmdPath))
            add(gamePort(command.gamePort))
            add(rconPort(command.rconPort))
        }

        return ServerPreflightReport(
            canProceed = checks.none { it.status == PreflightStatus.ERROR },
            checks = checks,
        )
    }

    private fun serverPathAllowed(path: String): PreflightCheck {
        return if (managedPathPolicy.isServerPathAllowed(path)) {
            pass("installPath.managed", "インストール先の管理範囲", "servers配下のパスです")
        } else {
            error(
                "installPath.managed",
                "インストール先の管理範囲",
                "${managedPathPolicy.managedRoot()}\\servers 配下を指定してください",
            )
        }
    }

    private fun toolPathAllowed(path: String): PreflightCheck {
        return if (managedPathPolicy.isToolPathAllowed(path)) {
            pass("steamCmdPath.managed", "SteamCMD保存先の管理範囲", "tools配下のパスです")
        } else {
            error(
                "steamCmdPath.managed",
                "SteamCMD保存先の管理範囲",
                "${managedPathPolicy.managedRoot()}\\tools 配下を指定してください",
            )
        }
    }

    private fun MutableList<PreflightCheck>.addPathChecks(
        id: String,
        label: String,
        inspection: PathInspection,
    ) {
        when {
            !inspection.valid -> add(error("$id.valid", label, "Windowsパスとして解釈できません"))
            !inspection.absolute -> add(error("$id.absolute", label, "絶対パスを指定してください"))
            inspection.root -> add(error("$id.root", label, "ドライブのルートは指定できません"))
            inspection.exists && !inspection.directory ->
                add(error("$id.directory", label, "指定されたパスはフォルダーではありません"))
            else -> {
                add(
                    PreflightCheck(
                        id = "$id.exists",
                        label = label,
                        status = if (inspection.exists) PreflightStatus.PASS else PreflightStatus.WARNING,
                        message = if (inspection.exists) {
                            "フォルダーが存在します"
                        } else {
                            "フォルダーは構築時に作成されます"
                        },
                    ),
                )
                add(
                    if (inspection.writable) {
                        pass("$id.writable", "${label}の書き込み権限", "作成先へ書き込み可能です")
                    } else {
                        error("$id.writable", "${label}の書き込み権限", "作成先へ書き込みできません")
                    },
                )
            }
        }
    }

    private fun pathsDistinct(command: Command): PreflightCheck {
        return if (normalize(command.installPath) != normalize(command.steamCmdPath)) {
            pass("paths.distinct", "保存先の分離", "インストール先とSteamCMD保存先は分離されています")
        } else {
            error("paths.distinct", "保存先の分離", "インストール先とSteamCMD保存先を分けてください")
        }
    }

    private fun storageSpace(inspection: PathInspection): PreflightCheck {
        val usableSpace = inspection.usableSpaceBytes
            ?: return warning("storage.space", "ディスク空き容量", "空き容量を確認できませんでした")
        val usableGiB = usableSpace / GIBIBYTE
        return if (usableSpace >= MINIMUM_FREE_SPACE_BYTES) {
            pass("storage.space", "ディスク空き容量", "利用可能: ${usableGiB} GiB")
        } else {
            error(
                "storage.space",
                "ディスク空き容量",
                "利用可能: ${usableGiB} GiB。10 GiB以上の空き容量を確保してください",
            )
        }
    }

    private fun steamCmdInstallation(inspection: PathInspection): PreflightCheck {
        return if (inspection.executableExists) {
            pass("steamcmd.installed", "SteamCMD", "steamcmd.exeが見つかりました")
        } else {
            warning("steamcmd.installed", "SteamCMD", "SteamCMDは構築時にダウンロードされます")
        }
    }

    private fun gamePort(port: Int): PreflightCheck {
        return if (environmentInspector.isUdpPortAvailable(port)) {
            pass("port.game", "ゲームポート UDP $port", "使用できます")
        } else {
            error("port.game", "ゲームポート UDP $port", "既に使用されています")
        }
    }

    private fun rconPort(port: Int): PreflightCheck {
        return if (environmentInspector.isTcpPortAvailable(port)) {
            pass("port.rcon", "RCONポート TCP $port", "使用できます")
        } else {
            error("port.rcon", "RCONポート TCP $port", "既に使用されています")
        }
    }

    private fun normalize(path: String): String {
        return path.trim().trimEnd('\\', '/').lowercase()
    }

    private fun pass(id: String, label: String, message: String): PreflightCheck {
        return PreflightCheck(id, label, PreflightStatus.PASS, message)
    }

    private fun warning(id: String, label: String, message: String): PreflightCheck {
        return PreflightCheck(id, label, PreflightStatus.WARNING, message)
    }

    private fun error(id: String, label: String, message: String): PreflightCheck {
        return PreflightCheck(id, label, PreflightStatus.ERROR, message)
    }

    data class Command(
        val installPath: String,
        val steamCmdPath: String,
        val gamePort: Int,
        val rconPort: Int,
    )

    companion object {
        private const val GIBIBYTE = 1024L * 1024L * 1024L
        private const val MINIMUM_FREE_SPACE_BYTES = 10L * GIBIBYTE
    }
}
