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
│     │  │  │  ├─ construction/
│     │  │  │  ├─ preflight/
│     │  │  │  └─ error/
│     │  │  ├─ application/
│     │  │  │  ├─ construction/
│     │  │  │  └─ preflight/
│     │  │  ├─ domain/
│     │  │  │  ├─ construction/
│     │  │  │  └─ preflight/
│     │  │  └─ infrastructure/
│     │  │     └─ windows/
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

バックエンドは`web`、`application`、`domain`、`infrastructure`の役割で分け、その中を`construction`や`preflight`などの機能単位で整理します。

- `web`: REST API、リクエスト、APIエラー
- `application`: ユースケースと処理手順
- `domain`: 構築計画や検証結果などの業務モデル
- `infrastructure`: Windows、ファイル、ポートなど外部環境へのアクセス

フロントエンドは`App.tsx`をルーティングの入口とし、`pages`、`components`、`api`、`types`、`styles`の役割単位で整理します。

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
