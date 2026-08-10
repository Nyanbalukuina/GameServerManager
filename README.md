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
│     │  │  │  ├─ palworld/
│     │  │  │  ├─ construction/
│     │  │  │  ├─ preflight/
│     │  │  │  ├─ steamcmd/
│     │  │  │  └─ error/
│     │  │  ├─ application/
│     │  │  │  ├─ palworld/
│     │  │  │  ├─ construction/
│     │  │  │  ├─ preflight/
│     │  │  │  └─ steamcmd/
│     │  │  ├─ domain/
│     │  │  │  ├─ palworld/
│     │  │  │  ├─ construction/
│     │  │  │  ├─ preflight/
│     │  │  │  └─ steamcmd/
│     │  │  └─ infrastructure/
│     │  │     ├─ palworld/
│     │  │     ├─ steamcmd/
│     │  │     ├─ windows/
│     │  │     └─ demo/
│     │  └─ resources/
│     └─ test/kotlin/gameservermanager/
└─ frontend/
   └─ src/
      ├─ pages/
      ├─ components/
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

現在は実SteamCMDを起動せず、OSの一時領域へ同じ構成の小さな模擬ファイルを作るデモ構築に対応しています。

## ネットワーク公開

GameServerManagerは特定のVPN製品へ依存しないWebアプリとして実装します。LAN、Tailscale、その他のVPNからのアクセスは、Spring Bootにとってすべて通常のHTTP通信として扱います。

ログイン機能が完成するまでは、安全のため`127.0.0.1:8080`だけで待ち受けます。認証、Cookieセッション、CSRF対策の実装後は、次の環境変数でLANやVPNから接続できるようにします。

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
cd backend
.\gradlew.bat bootRun
```

ターミナル2でフロントエンドを起動します。

```powershell
cd frontend
npm.cmd run dev
```

起動後、`http://localhost:5173`を開きます。

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
