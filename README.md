# GameServerManager

ブラウザからゲーム専用サーバーを安全に管理するためのOSSです。

## 必要環境

- JDK 21
- Node.js 24
- npm

## 構成

```text
GameServerManager/
├─ backend/
│  └─ src/
│     ├─ main/
│     │  ├─ kotlin/gameservermanager/
│     │  │  ├─ web/
│     │  │  │  ├─ authentication/
│     │  │  │  ├─ palworld/
│     │  │  │  ├─ construction/
│     │  │  │  ├─ configuration/
│     │  │  │  ├─ preflight/
│     │  │  │  ├─ server/
│     │  │  │  ├─ steamcmd/
│     │  │  │  └─ error/
│     │  │  ├─ application/
│     │  │  │  ├─ authentication/
│     │  │  │  ├─ palworld/
│     │  │  │  ├─ construction/
│     │  │  │  ├─ preflight/
│     │  │  │  ├─ server/
│     │  │  │  └─ steamcmd/
│     │  │  ├─ domain/
│     │  │  │  ├─ authentication/
│     │  │  │  ├─ palworld/
│     │  │  │  ├─ construction/
│     │  │  │  ├─ preflight/
│     │  │  │  ├─ server/
│     │  │  │  └─ steamcmd/
│     │  │  └─ infrastructure/
│     │  │     ├─ authentication/
│     │  │     ├─ palworld/
│     │  │     ├─ server/
│     │  │     ├─ steamcmd/
│     │  │     ├─ windows/
│     │  │     └─ demo/
│     │  └─ resources/
│     └─ test/kotlin/gameservermanager/
└─ frontend/
   └─ src/
      ├─ pages/
      │  └─ palworld/
      ├─ components/
      │  ├─ common/
      │  └─ palworld/
      ├─ api/
      ├─ types/
      ├─ styles/
      └─ App.tsx
```

バックエンドは`web`、`application`、`domain`、`infrastructure`の役割で分け、その中を機能単位で整理します。Palworld専用処理は各レイヤーの`palworld`へまとめ、SteamCMDなど他ゲームでも利用できる処理は共通機能として分離します。

- `web`: REST API、リクエスト、APIエラー
- `application`: ユースケースと処理手順
- `domain`: 構築計画や検証結果などの業務モデル
- `infrastructure`: Windows、ファイル、ポートなど外部環境へのアクセス

フロントエンドは`App.tsx`をルーティングの入口とし、`pages`、`components`、`api`、`types`、`styles`の役割単位で整理します。

## 管理データ

既定の管理ルートは`C:\GameServerManager`です。

```text
C:\GameServerManager\
├─ tools\
│  └─ steamcmd\
├─ servers\
│  └─ palworld\
│     └─ main\
│        └─ runtime\
├─ backups\
└─ logs\
```

ゲーム本体は`servers`、SteamCMDなどの共用ツールは`tools`配下だけを許可します。ワールド・設定・ゲームログはゲーム本体配下、バックアップは`backups`へ分離します。

画面から行うデモ構築では実SteamCMDを起動せず、`%TEMP%\GameServerManagerDemo`へ同じ構成の小さな模擬ファイルを作ります。このデータはアプリ終了時に自動削除されません。デモ管理画面で削除できます。

## ネットワーク公開

GameServerManagerは特定のVPN製品へ依存しないWebアプリとして実装します。LAN、Tailscale、その他のVPNからのアクセスは、Spring Bootにとってすべて通常のHTTP通信として扱います。

既定では安全のため`127.0.0.1:8080`だけで待ち受けます。単一管理者認証、Cookieセッション、CSRF対策は実装済みです。LANやVPNから接続する場合だけ、次の環境変数を明示的に設定します。

```powershell
$env:GAME_SERVER_MANAGER_ADDRESS = "0.0.0.0"
$env:GAME_SERVER_MANAGER_PORT = "8080"
```

想定するアクセス例です。

```text
自宅LAN:   http://192.168.x.x:8080
Tailscale: http://100.x.x.x:8080
```

Tailscaleは任意の利用例であり、GameServerManagerの必須依存ではありません。Tailscaleのインストール、Tailnetへの端末追加、アクセス制御は利用者が行います。管理ポート`8080`をルーターのポート転送などで一般インターネットへ直接公開しないでください。

## 開発時の起動

ターミナル1でバックエンドを起動します。

```powershell
$env:GAME_SERVER_MANAGER_ROOT = "$env:LOCALAPPDATA\GameServerManagerDev"
cd backend
.\gradlew.bat bootRun
```

初回起動時はサーバーPC自身から管理者パスワードを設定します。認証情報は`$env:GAME_SERVER_MANAGER_ROOT\config\authentication.json`へ保存され、パスワード本体ではなくbcryptハッシュだけが残ります。パスワードを忘れた場合はバックエンドを停止し、このファイルを削除または退避してからlocalhostで再設定します。PalworldやSteamCMDのデータには影響しません。

ターミナル2でフロントエンドを起動します。

```powershell
cd frontend
npm.cmd run dev
```

起動後、`http://localhost:5173`を開きます。

管理APIはSpring SecurityのCookieセッションで保護され、状態を変更する通信にはCSRFトークンが必要です。認証完成後も、`GAME_SERVER_MANAGER_ADDRESS=0.0.0.0`への変更は利用者が明示的に行います。

## テスト

```powershell
cd backend
.\gradlew.bat test
```

Valve公式SteamCMDを一時フォルダーへ実際にダウンロードして検証する場合は、明示的に外部テストを有効化します。

```powershell
cd backend
.\gradlew.bat test --tests "gameservermanager.infrastructure.steamcmd.OfficialSteamCmdDownloadExternalTests" -PsteamcmdExternalTest=true
```

通常のバックエンドテストではネットワークへ接続しません。

Palworld Dedicated Serverを一時フォルダーへ実際にインストールする外部テストは以下です。数GB規模の通信が発生し、Steamネットワークへ直接接続できる環境が必要です。

```powershell
cd backend
.\gradlew.bat test --tests "gameservermanager.infrastructure.steamcmd.PalworldInstallationExternalTests" -PpalworldExternalTest=true
```

```powershell
cd frontend
npm.cmd test
```

## ビルド

```powershell
cd backend
.\gradlew.bat build
```

```powershell
cd frontend
npm.cmd run build
```

## 現在の実装状況

- 構築フォーム、構築計画、Windows環境の事前検証
- 初回管理者設定、ログイン・ログアウト、CSRF保護
- 一時領域を使ったPalworldデモ作成とデモ管理
- 1タイトルにつき1サーバーのJSON登録
- SteamCMD準備、Palworld導入・設定・起動・停止・更新・バックアップのバックエンド処理
- 毎日の指定時刻停止・起動
- Windows Firewallと自動起動タスクを設定する配布用PowerShell

実Palworldを画面から一括作成するフローを実装済みです。事前検証成功後に、SteamCMD準備、Palworld導入、設定保存、自動運転保存、ゲーム用Firewall規則の登録、起動確認、管理登録を順番に実行します。起動または登録に失敗した場合は追加したFirewall規則を解除します。SteamCMD以降とFirewall補助タスクの実機確認は、ゲームサーバー予定マシンで行います。ARK、Minecraft、既存サーバー取り込みも今後の対応です。

初期版ではDBを使用せず、認証、サーバー登録、自動運転、実行状態、操作履歴を管理ルート内のJSONまたはJSON Linesへ保存します。

## Palworldサーバーの自動運転

Palworldサーバーの新規作成画面で、毎日の停止時刻と起動時刻をまとめて設定できます。既定値は停止 `04:00`、起動 `09:00` です。構築計画では自動運転の有効状態、停止・起動時刻、バックアップ設定、保持数を確認できます。

構築計画の「デモサーバーを作成」では、本物のSteamCMDを使用せず、一時領域へPalworldのフォルダー構成、パスワードを伏せたデモ設定、自動運転設定を保存します。本番用の自動運転は有効化しません。

初期版は1タイトルにつき1サーバーだけ管理します。Palworldを作成すると`config/servers.json`へ登録され、ゲーム選択画面は新規作成ではなく管理画面へのリンクを表示します。ARKとMinecraftはそれぞれ未作成であれば、対応実装後に1サーバーずつ作成できます。

デモ管理画面では、実プロセスを起動せずに起動・停止・再起動の状態遷移を確認できます。`PALWORLD`と入力して削除すると、`GameServerManagerDemo`一時領域とPalworldの登録だけを削除し、再びPalworldを作成できる状態へ戻します。

自動運転を有効にすると、停止時刻にワールドを保存してサーバーを停止します。指定した停止時間帯だけ停止を維持し、起動時刻になるとサーバーを起動します。

Palworld本体の自動バックアップは常に有効化し、頻度と世代管理はPalworldに任せます。Game Server Manager独自のZIPバックアップは作成しません。

自動運転設定は `config/palworld-main-automation.json`、実行状態は `config/palworld-main-automation-runtime.json` に保存します。管理者パスワードは自動運転設定へ保存せず、停止処理の直前にPalworld本体の `PalWorldSettings.ini` から読み取ります。

## ゲームポートの接続範囲

Palworld作成時に、プレイヤー接続用UDPポートの接続元を選択できます。既定では同一LANとTailscaleを許可します。

- 同一LAN: `LocalSubnet`
- Tailscale: `100.64.0.0/10`
- 手動指定: IPv4アドレスまたはCIDRを複数指定可能
- すべての接続元: `Any`。選択時はほかの範囲を無効化

選択内容は構築計画、デモ設定、`config/servers.json`のサーバー登録へ保存します。実構築では管理者権限のFirewall補助タスクがゲーム用UDP受信規則へ反映し、デモ作成ではWindows Firewallを変更しません。管理画面、RCON、Palworld REST APIの公開範囲とは分離して扱います。

## Windows Firewallと自動起動

React画面を含む実行可能JARと、管理者権限が必要な限定操作だけを行うセットアップスクリプトを生成できます。

```powershell
cd backend
.\gradlew.bat windowsDistribution
cd ..\windows\publish\GameServerManager
```

`windows\publish\GameServerManager`へ`GameServerManager.exe`、Java 21ランタイム、React画面を内包したJAR、セットアップスクリプトをまとめて生成します。Javaを別途インストールせず、`GameServerManager.exe`から起動できます。

変更内容だけを確認する場合は`Plan`を使用します。この操作はFirewallやタスクスケジューラを変更しません。

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\GameServerManager.WindowsSetup.ps1 `
  -Action Plan `
  -StorageRoot C:\GameServerManager `
  -ManagementPort 8080
```

登録は管理者として開いたPowerShellで実行します。

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\GameServerManager.WindowsSetup.ps1 `
  -Action Install `
  -StorageRoot C:\GameServerManager `
  -ManagementPort 8080
```

`Install`は次の限定操作だけを行います。

- 管理画面用TCPポートをLANとTailscaleアドレス範囲から許可する
- ゲーム構築時のUDP規則だけを操作するFirewall補助タスクを登録する
- 実行可能JARを`C:\GameServerManager\app`へ配置する
- 制限付きユーザーでWindows起動時に実行する固定名タスクを登録する

RCONポートとPalworld REST APIポートは外部へ公開しません。状態確認は`-Action Status`、解除は管理者PowerShellで`-Action Uninstall`を使用します。解除してもSteamCMD、ゲーム本体、ワールド、バックアップ、認証・自動運転設定は削除しません。

実Palworldの構築時には、選択したゲームポートと接続元でUDP受信規則を追加します。起動または管理登録に失敗した場合は追加した規則を解除します。デモ操作ではFirewallとタスクスケジューラを変更しません。
