# ゲームサーバー管理OSSについて

## 目的

コマンド操作が苦手な人でも、ブラウザからゲーム専用サーバーを安全に管理できるOSSを作る。

主な利用場面は、自宅のWindowsまたはLinuxマシンで稼働するPalworldやARKなどの管理とする。

## 基本方針

- ブラウザからサーバーを起動・停止・再起動する
- SteamCMDによるインストールと更新を行う
- 稼働状態、接続人数、ログを確認する
- セーブデータをバックアップ・復元する
- 定刻起動、自動停止、更新予約に対応する
- 任意のコマンド実行機能は設けず、安全な操作だけを提供する
- 最初は単一のWindowsマシンに対応し、後からLinuxや複数マシンへ拡張する

## 想定構成

```text
iPhone・PCのブラウザ
        │
        │ Tailscale
        ▼
ゲームサーバーPC
├─ ゲームサーバー管理OSS
├─ Palworld Dedicated Server
└─ ARK Dedicated Server
```

管理OSSはゲームサーバーと同じマシンで動かし、ローカルのPowerShell、SteamCMD、RCONなどを利用する。

この構成ではSSH、WinRM、リモート管理者パスワードなどを管理OSSに保存する必要がない。

## 接続方法

管理画面にはTailscale経由で接続する。

```text
http://Tailscale-IP:8080
```

例：

```text
http://100.x.x.x:8080
```

基本的なセキュリティ方針は以下とする。

- ルーターのポート開放は行わない
- Spring BootはTailscaleのIPアドレスで待ち受ける
- WindowsファイアウォールはTailscale経由の通信だけを許可する
- Tailscaleを利用していても、管理画面のログイン認証は残す
- 将来的にはTailscale ServeによるHTTPS化も検討する

Spring Bootの設定例：

```yaml
server:
  address: 100.x.x.x
  port: 8080
```

## 対応ゲーム

### Palworld

- サーバーの起動・停止
- SteamCMDによる更新
- 設定ファイルの編集
- セーブデータのバックアップ・復元
- RCONによる告知や安全な停止

### ARK

- マップやポートの設定
- SteamCMDによる更新
- RCONによる告知、ワールド保存、停止
- `ShooterGame/Saved`のバックアップ・復元
- MODと設定ファイルの管理

ARKには次の違いがある。

- ARK: Survival EvolvedはWindowsとLinuxの専用サーバーに対応
- ARK: Survival AscendedはWindowsでの運用を基本とする
- ARK: Survival AscendedはPalworldよりメモリ消費が多い

更新時には以下の処理を一連の操作として実行する。

```text
プレイヤーへ停止予告
→ ワールド保存
→ サーバー停止
→ バックアップ
→ SteamCMDで更新
→ 再起動
→ 起動確認
```

## 技術スタック

| 分類 | 採用候補 |
|---|---|
| バックエンド | Kotlin + Spring Boot |
| Web画面 | Thymeleaf + HTMX |
| 認証・認可 | Spring Security |
| DB | SQLite |
| DB操作 | Spring JDBCまたはExposed |
| DBマイグレーション | Flyway |
| Windows操作 | PowerShell、Windowsサービス |
| Linux操作 | systemd、シェル |
| ゲーム連携 | SteamCMD、RCON |
| 初期の配布形式 | 単体JARと設定ファイル |

Reactなどの独立したフロントエンドを最初から設けず、Spring Boot内で画面も生成する。これにより、開発構成と配布物を小さく保つ。

## データ管理

DBサーバーは用意せず、SQLiteを1ファイルで使用する。

SQLiteに保存する情報：

- ユーザー
- パスワードハッシュ
- 管理者・閲覧者などの権限
- 登録されたゲームサーバー
- ゲームごとの設定
- スケジュール
- 操作履歴

セーブデータやログ本体はDBへ格納せず、ファイルシステムで管理する。

```text
data/
├─ app.db
├─ backups/
├─ logs/
└─ config/
```

パスワードやトークンは平文の設定ファイルへ保存しない。

- WindowsではCredential ManagerまたはDPAPIを利用する
- Linuxではアクセス権を制限した秘密情報ファイルを利用する
- DockerではSecretsまたは環境変数を利用する

初期版では、秘密情報を保存しなくて済むローカル実行方式を優先する。

## OSと実行方式

Linuxのゲームサーバーは、必ずしもDockerで動かす必要はない。WindowsとLinuxのどちらでも、ネイティブ実行とDocker実行を別の方式として扱う。

```text
ゲーム
├─ Palworld
└─ ARK

実行方式
├─ Windowsネイティブ
├─ Linuxネイティブ
└─ Docker
```

各方式で利用する操作：

- Windowsネイティブ：PowerShellまたはWindowsサービス
- Linuxネイティブ：systemdまたはシェル
- Docker：Docker Compose

WindowsのPowerShellからDockerを操作することにも意味がある。PowerShellは操作窓口であり、ゲームサーバーはDocker DesktopとWSL2上のLinuxコンテナで動作する。

ただし、初期版はDockerを必須にしない。Windows専用サーバーや保存先、ポート設定を考慮し、まずWindowsネイティブ版を完成させる。

## 拡張可能な設計

ゲーム固有処理とOS固有処理を分離する。

```text
管理画面・共通処理
├─ ゲームアダプター
│  ├─ Palworld
│  └─ ARK
└─ 実行アダプター
   ├─ Windows
   ├─ Linux
   └─ Docker
```

共通処理：

- 起動・停止・再起動
- 状態確認
- 更新
- バックアップ
- スケジュール
- 通知
- 操作履歴

ゲーム固有処理：

- Steam App ID
- 実行ファイルと起動引数
- セーブデータの保存場所
- 設定ファイル
- RCONコマンド
- 正常起動の判定方法

OS固有処理：

- プロセスまたはサービスの操作
- コマンド実行
- ファイルパス
- 権限管理

## 複数マシン対応

将来的には各サーバーマシンへ小さな管理エージェントを導入する。

```text
中央管理画面
├─ Windows管理エージェント
└─ Linux管理エージェント
```

エージェント側から中央管理画面へ接続する方式とし、利用者がSSH接続文字列や管理者パスワードを登録する構成を避ける。

初回登録用トークンでマシンを登録し、その後はマシン固有の鍵または証明書で認証する。

## 開発順

1. 単一ユーザー・単一Windowsマシン対応
2. SQLiteとSpring Securityによる認証
3. Palworldの起動・停止・更新・バックアップ
4. ARK対応
5. 権限管理と操作履歴
6. Linuxネイティブ対応
7. Docker対応
8. 複数マシン用エージェント

## 初期版の完成条件

- Windowsマシンへ簡単に導入できる
- Tailscale経由で管理画面へ接続できる
- ログイン認証が機能する
- Palworldを安全に起動・停止・更新できる
- セーブデータをバックアップ・復元できる
- 操作結果とエラーをブラウザで確認できる
- 任意コマンドを実行できない

最初の目標は「1台へ導入すれば、コマンドを使わずブラウザからゲームサーバーを管理できるOSS」とする。
