# GameServerManager

Windows上のゲーム専用サーバーを、ブラウザから構築・管理するためのアプリケーションです。

SteamCMDの準備、ゲームサーバーのインストール、設定ファイル生成、Windows Firewall設定、起動・停止までを一つの画面から実行できます。

> [!IMPORTANT]
> 現在は開発中です。主要機能は実装済みですが、実ゲームサーバーとクライアントを使用した受入検証を継続しています。

## 対応ゲーム

| ゲーム | 対応内容 |
|---|---|
| ARK: Survival Ascended | 構築、起動、状態確認、RCONによる保存・停止、再起動、削除、デモモード |
| Palworld | 構築、起動、状態確認、保存・停止、再起動、更新、バックアップ、自動運転、デモモード |

現行仕様では、1タイトルにつき1サーバーを管理します。

## 主な機能

- ブラウザからのゲームサーバー構築・管理
- 構築前のWindows環境、保存先、空き容量、ポート検証
- SteamCMDのダウンロード、再利用、ゲームサーバーのインストール
- ゲームごとの初期設定ファイル生成
- 接続元を限定したWindows Firewall受信規則
- サーバーの起動、状態確認、安全停止、再起動
- localhost限定公開とCSRF保護
- 開発時に実環境を変更せず操作を確認できるデモモード
- 管理データ、ログ、バックアップのローカル保存

## 技術スタック

- Kotlin 2.2 / Java 21 / Spring Boot 3.5
- React 19 / TypeScript 6 / Vite 8
- Gradle Kotlin DSL / npm
- JUnit 5 / Spring Boot Test / Vitest / Testing Library
- PowerShell / Windows Firewall / タスクスケジューラ

## システム構成

```text
Reactフロントエンド
  ↓ REST API
Spring Bootバックエンド
  ├─ 入力検証・構築処理
  ├─ SteamCMD・プロセス管理
  ├─ 設定・ログ・バックアップ
  └─ Windows Firewall連携
       ↓
Windows / SteamCMD / ゲーム専用サーバー
```

フロントエンドからPowerShellやSteamCMDを直接実行せず、すべてバックエンドAPIを経由します。

## 開発環境

### 必要なもの

- Windows
- JDK 21
- Node.js 24
- npm

Gradle本体のインストールは不要です。同梱のGradle Wrapperを使用します。

### 初回セットアップ

```powershell
cd frontend
npm.cmd install
```

バックエンドの依存関係は、初回のGradle実行時に自動取得されます。

### 開発サーバーの起動

ターミナル1でバックエンドを起動します。

```powershell
cd backend
$env:GAME_SERVER_MANAGER_ROOT = "$env:LOCALAPPDATA\GameServerManagerDev"
.\gradlew.bat bootRun
```

ターミナル2でフロントエンドを起動します。

```powershell
cd frontend
npm.cmd run dev
```

ブラウザで `http://localhost:5173` を開きます。ログイン設定は不要です。Viteは `/api` を `http://localhost:8080` へ転送します。

## テスト

```powershell
# バックエンド
cd backend
.\gradlew.bat test

# フロントエンド
cd ..\frontend
npm.cmd test
npm.cmd run lint
```

通常の自動テストは、外部ネットワークや実ゲームサーバーへ接続しません。

## ビルド

React画面を含む実行可能JARを生成します。

```powershell
cd backend
.\gradlew.bat bootJar
```

JARは `backend/build/libs` に生成されます。

## Windows配布物

配布用EXEインストーラーの作成、動作確認、GitHub Releasesへの公開手順は、[Windows配布手順](_Document/Windows配布手順.md)を参照してください。

Java 21ランタイム、実行可能JAR、Windows連携スクリプトを含む配布物を生成します。

```powershell
cd backend
.\gradlew.bat windowsDistribution
```

出力先は `windows/publish/GameServerManager` です。

セットアップ内容だけを確認する場合は、生成先で次を実行します。この操作はWindows設定を変更しません。

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\GameServerManager.WindowsSetup.ps1 `
  -Action Plan `
  -ManagementPort 8080
```

インストールは管理者として開いたPowerShellで実行します。

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\GameServerManager.WindowsSetup.ps1 `
  -Action Install `
  -ManagementPort 8080
```

セットアップでは、管理画面用Firewall規則、ゲーム用Firewall補助タスク、Windows起動時の自動起動タスクを登録します。状態確認は `-Action Status`、解除は `-Action Uninstall` を使用します。

管理データの既定保存先は `%LOCALAPPDATA%\GameServerManager` です。別ドライブなどへ変更する場合は、セットアップ時に `-StorageRoot D:\GameServerManager` のように指定します。

## 実行設定

| 環境変数 | 既定値 | 用途 |
|---|---|---|
| `GAME_SERVER_MANAGER_ROOT` | `%LOCALAPPDATA%\GameServerManager` | 管理データの保存先 |
| `GAME_SERVER_MANAGER_ADDRESS` | `127.0.0.1` | 管理画面の待受アドレス |
| `GAME_SERVER_MANAGER_PORT` | `8080` | 管理画面のポート |
| `GAME_SERVER_MANAGER_SECURE_COOKIE` | `false` | CookieのSecure属性 |
| `GAME_SERVER_MANAGER_DEMO_ENABLED` | `true` | 開発用デモ機能の有効化 |

管理データは管理ルート配下の `config`、`logs`、`backups`、`tools`、`servers` に保存します。

## ネットワークとセキュリティ

- 既定では `127.0.0.1:8080` だけで待ち受けます。
- LANやVPNから利用する場合のみ、`GAME_SERVER_MANAGER_ADDRESS=0.0.0.0` を明示的に設定します。
- 管理ポートを一般インターネットへ直接公開しないでください。
- ゲームポートは、構築時に選択したLAN、Tailscale、カスタムアドレスなどの接続元だけに許可します。
- RCONとゲーム管理APIは外部公開せず、localhostから使用します。
- パスワードはログ、操作履歴、通常のAPIレスポンスへ出力しません。
- デモモードではSteamCMD、実サーバー、Firewall、タスクスケジューラを変更しません。
- Windows配布版ではデモ画面とデモAPIを無効化します。

## ディレクトリ構成

```text
GameServerManager/
├─ backend/      Kotlin・Spring Bootバックエンド
├─ frontend/     Reactフロントエンド
├─ windows/      配布・Firewall連携用PowerShell
└─ _Document/    共通仕様・ゲーム別設計仕様
```

## ドキュメント

- [共通部分の仕様書](_Document/共通部分の仕様書.md)
- [ARK用設計仕様書](_Document/ARK用設計仕様書.md)
- [パルワールド用設計仕様書](_Document/パルワールド用設計仕様書.md)
- [Windows配布手順](_Document/Windows配布手順.md)
