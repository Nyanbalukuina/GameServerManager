# GameServerManager

ブラウザからゲーム専用サーバーを安全に管理するためのOSSです。

## 必要環境

- JDK 21
- Node.js 24
- npm

## 構成

```text
backend/   Kotlin + Spring Boot
frontend/  TypeScript + React
```

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
