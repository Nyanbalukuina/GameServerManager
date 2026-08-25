# ARK用設計仕様書

## 1. 文書の目的

GameServerManager（以下、GSM）でARK: Survival Ascended（以下、ASA）の専用サーバーを構築・管理する機能の設計を定義する。本書は現行実装を基準とし、ARK: Survival Evolvedは対象外とする。

## 2. 対象範囲

### 対象機能

- Windows上へのASA Dedicated Serverの新規構築
- SteamCMDの準備とサーバーインストール
- 初期設定ファイルの生成・更新
- Windows Firewall受信規則の登録
- サーバーの起動、状態確認、安全停止、再起動、削除
- 実サーバーを操作しないデモ構築・デモ操作
- GSM管理対象への登録とプロセス情報の復元

### 対象外

- ARK: Survival Evolved
- MOD、クラスタ、複数ASAサーバーの管理
- ルーターのポート転送、UPnP、CGNAT対策
- 全ゲーム設定の編集、プレイヤー管理、高度なログ分析
- GSM独自のワールドバックアップと自動運転

## 3. システム構成

```text
Reactフロントエンド
  └─ ASA構築画面・管理画面
       ↓ REST API
Spring Bootバックエンド
  ├─ 入力検証・構築処理
  ├─ SteamCMD共通処理
  ├─ ASA設定ファイル処理
  ├─ Windows Firewall共通処理
  ├─ ASAプロセス管理・Source RCON
  └─ サーバー登録情報の永続化
       ↓
Windows / SteamCMD / ArkAscendedServer.exe
```

管理データの既定ルートは `C:\GameServerManager` とし、環境変数 `GAME_SERVER_MANAGER_ROOT` で変更できる。Webサーバーは既定で `127.0.0.1:8080` にバインドする。

## 4. サーバー識別情報

| 項目 | 値 |
|---|---|
| ゲーム識別子 | `ASA` |
| サーバー識別子 | `asa-main` |
| Steam App ID | `2430930` |
| 対応マップ | `TheIsland_WP` |
| 実行ファイル | `ShooterGame/Binaries/Win64/ArkAscendedServer.exe` |
| 設定ファイル | `ShooterGame/Saved/Config/WindowsServer/GameUserSettings.ini` |
| ログ | `<管理ルート>/logs/asa-main.log` |

現行仕様では1ゲームにつき1サーバーだけを登録できる。

## 5. 構築入力仕様

| 項目 | 必須 | 制約・既定値 |
|---|---:|---|
| サーバー名 | 必須 | 1～100文字、改行不可 |
| インストール先 | 必須 | 500文字以内、GSM管理範囲内 |
| SteamCMD配置先 | 必須 | 500文字以内 |
| マップ | 必須 | `TheIsland_WP`のみ |
| ゲームポート | 必須 | 1～65534、画面既定値 `7777` |
| Peerポート | 自動 | ゲームポート + 1 |
| Queryポート | 必須 | 1～65535、画面既定値 `27015` |
| RCONポート | 必須 | 1～65535、画面既定値 `27020` |
| 最大プレイヤー数 | 必須 | 1～70、画面既定値 `20` |
| サーバーパスワード | 任意 | 64文字以内、改行不可 |
| 管理者パスワード | 必須 | 8～64文字、改行不可 |
| 接続元範囲 | 必須 | LAN、Tailscale、カスタム、全許可の組み合わせ |

ゲーム、Peer、Query、RCONの各ポートは重複を許可しない。管理者パスワードなどの秘密値をログや設定結果レスポンスへ含めない。

## 6. 構築処理

構築処理は同時実行を禁止し、次の順序で実行する。

1. 同一ゲームの登録有無、管理対象パス、ポート、空き容量などを事前検証する。
2. `steamcmd.exe` がなければSteamCMDをダウンロードして安全に展開する。既存配置があれば再利用する。
3. SteamCMDでApp ID `2430930`をインストールまたは更新する。
4. `GameUserSettings.ini`へ初期設定を保存する。
5. ゲーム、Peer、Queryの各UDPポートにWindows Firewall受信規則を登録する。
6. ASAプロセスを起動し、ゲームポートの待受を確認する。
7. GSMの管理対象へ `RUNNING` 状態で登録する。

Firewall登録後に起動または登録が失敗した場合、その構築中に追加した規則を逆順で解除する。デモ構築ではSteamCMD、Firewall、実プロセスを操作しない。

## 7. 起動仕様

起動引数は次の形式で生成する。

```text
TheIsland_WP?QueryPort=<Queryポート>
-port=<ゲームポート>
-WinLiveMaxPlayers=<最大プレイヤー数>
```

Peerポートはゲームポートの次の番号として管理する。起動直後にプロセスが終了した場合やゲームポートの待受を確認できない場合は成功扱いにしない。

## 8. 設定ファイル仕様

`GameUserSettings.ini`の次のキーを作成または更新する。

```ini
[SessionSettings]
SessionName=<サーバー名>

[ServerSettings]
ServerPassword=<サーバーパスワード>
ServerAdminPassword=<管理者パスワード>
RCONEnabled=True
RCONPort=<RCONポート>
```

既存ファイルは内容を維持したまま対象キーを更新する。既存ファイルがある場合は `<管理ルート>/backups/asa-main/config` に更新前バックアップを保存する。

## 9. 管理操作仕様

| 操作 | 挙動 |
|---|---|
| 状態確認 | 登録情報、プロセス生存、PID、ポート、ログパスを返す |
| 起動 | 登録済み設定からプロセスを起動し、待受を確認する |
| 停止 | RCONで `SaveWorld`、`DoExit` の順に実行し、最大30秒待機する |
| 強制停止 | 正常終了待機がタイムアウトした場合にプロセス管理機能で停止する |
| 再起動 | 安全停止後に同じ登録設定で起動する |
| 削除 | 確認文字列を要求し、管理登録とGSM管理資源を削除する |

停止と再起動には管理者パスワードが必要である。RCONはlocalhost上のASAに対してSource RCONプロトコルで接続し、外部公開を前提としない。

## 10. 主なAPI

| メソッド | パス | 用途 |
|---|---|---|
| POST | `/api/asa/preflight` | 構築前検証 |
| POST | `/api/asa/constructions` | 実サーバー一括構築 |
| POST | `/api/asa/constructions/demo` | デモ構築 |
| GET | `/api/asa/server` | 登録情報取得 |
| GET | `/api/asa/server/status` | 状態取得 |
| POST | `/api/asa/server/start` | 起動 |
| POST | `/api/asa/server/stop` | 安全停止 |
| POST | `/api/asa/server/restart` | 再起動 |
| DELETE | `/api/asa/server` | 削除 |

APIは初回管理者設定後のCookieセッション認証とCSRF保護の対象とする。

## 11. 完成条件

- GSM画面から事前検証、構築、起動、状態確認を一連で実行できる。
- ASAクライアントから接続し、ログイン、プレイ、保存、再接続できる。
- GSMから安全停止・再起動後も同じワールドを利用できる。
- 許可した接続元だけに必要なUDPポートを公開する。
- 構築失敗時に不要なFirewall規則が残らない。
- 既存のPalworld機能と管理データを壊さない。
- バックエンドとフロントエンドの自動テストが成功する。

## 12. 今後の拡張候補

- 対応マップの追加
- 詳細ゲーム設定と設定画面
- MOD・クラスタ管理
- ワールドバックアップ、復元、自動運転
- プレイヤー一覧と管理操作
- 実サーバーマシンおよびクライアントを使った継続的な受入検証
