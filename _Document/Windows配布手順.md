# Windows配布手順

GameServerManagerの配布用インストーラーを作成し、公開前に確認するための手順です。

## 1. 必要な環境

- Windows
- JDK 21（`jpackage.exe`を含むもの）
- Node.js 24 / npm
- WiX Toolset 3.14

次のコマンドで確認します。

```powershell
java -version
node --version
npm --version
Test-Path 'C:\Program Files (x86)\WiX Toolset v3.14\bin\candle.exe'
Test-Path 'C:\Program Files (x86)\WiX Toolset v3.14\bin\light.exe'
```

WiXがインストール済みでもコマンドとして認識されない場合は、ビルドするPowerShellでPATHへ追加します。

```powershell
$wixPath = 'C:\Program Files (x86)\WiX Toolset v3.14\bin'
$env:Path = "$wixPath;$env:Path"
candle.exe -?
light.exe -?
```

## 2. バージョンを決める

`backend/build.gradle.kts` の `version` を更新します。

```kotlin
version = "0.1.0"
```

`jpackage`で使用できる数値形式にします。配布版では `-SNAPSHOT` を付けない値を推奨します。

## 3. テストする

プロジェクトルートから実行します。

```powershell
cd D:\_GitHub\GameServerManager\backend
.\gradlew.bat test

cd ..\frontend
npm.cmd run test
npm.cmd run lint
npm.cmd run build
```

## 4. インストーラーを作成する

```powershell
cd D:\_GitHub\GameServerManager\backend
.\gradlew.bat windowsInstaller
```

このタスクは、Reactのビルド、Spring Boot JARの生成、Java 21ランタイム同梱アプリの生成、EXEインストーラーの生成を順番に行います。

生成先：

```text
D:\_GitHub\GameServerManager\windows\installer
```

インストーラーを使わない展開型アプリは、次のタスクで生成できます。

```powershell
cd D:\_GitHub\GameServerManager\backend
.\gradlew.bat windowsDistribution
```

生成先は `windows\publish\GameServerManager` です。

## 5. 配布前に確認する

新しいインストーラーで次を確認します。

- インストールが完了する
- スタートメニューとデスクトップのショートカットが作成される
- `GameServerManager.exe` の起動後にターミナルが表示されない
- 既定ブラウザで `http://localhost:8080` が開く
- 管理画面にログインなしでアクセスできる
- 初期保存先が `C:\GameServerManager` 配下になっている
- 配布版にデモ構築が表示されない
- ARKとPalworldの構築画面を表示できる
- Windowsの「インストールされているアプリ」からアンインストールできる

以前の版が起動中の場合は終了してから更新・アンインストールします。ファイルが使用中だと、Windowsから再起動を求められる場合があります。

## 6. GitHubで公開する

1. Gitへ変更をコミットする
2. バージョンと同じGitタグを作成する（例：`v0.1.0`）
3. GitHubのReleasesから新しいリリースを作成する
4. `windows\installer` に生成されたEXEを添付する
5. 対応ゲーム、主な変更、既知の制限をリリースノートへ記載する

## 配布前チェックリスト

- [ ] `backend/build.gradle.kts` のバージョンを更新した
- [ ] バックエンドテストが成功した
- [ ] フロントエンドのテスト・Lint・ビルドが成功した
- [ ] `windowsInstaller` が成功した
- [ ] 新規インストールと起動を確認した
- [ ] 更新または再インストールを確認した
- [ ] アンインストールを確認した
- [ ] GitHub Releaseへ正しいEXEを添付した
