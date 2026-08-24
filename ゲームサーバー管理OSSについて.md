# ゲームサーバー管理OSSについて

## 目的

GameServerManagerは、コマンド操作に不慣れな利用者でも、ブラウザから自宅のゲーム専用サーバーを安全に構築・運用できるようにするOSSである。

初期版は、1台のWindowsマシンへPalworld Dedicated Serverを新規構築し、同じマシンまたは同一ネットワーク内の別端末から管理することを目標とする。

## 初期版の範囲

- Windowsネイティブ環境
- 単一管理者
- 1タイトルにつき1サーバー
- Palworldの新規構築と管理
- 起動、停止、再起動、更新、バックアップ
- 毎日の停止、バックアップ、指定時刻起動
- LANまたは任意のVPN経由のブラウザ操作
- Windows FirewallとWindows起動時タスクの限定的な設定

ARK、Minecraft、Linux、Docker、複数マシン、複数ユーザー、既存サーバー取り込みは初期版の後に対応する。

## 想定構成

```text
管理用PC・スマートフォンのブラウザ
              │
              │ LANまたは任意のVPN
              ▼
WindowsゲームサーバーPC
├─ GameServerManager
├─ SteamCMD
└─ Palworld Dedicated Server
```

GameServerManagerはゲームサーバーと同じマシンで動作し、ローカルのファイル、プロセス、SteamCMD、Palworld REST APIを操作する。別マシンの管理者パスワードやSSH接続情報は保存しない。

## ネットワークと認証

GameServerManagerはTailscaleを含む特定のVPN製品へ依存しない。LANでは`192.168.x.x:8080`、Tailscaleを利用する場合は`100.x.x.x:8080`のように、到達可能なサーバーPCのアドレスへ接続する。

既定では`127.0.0.1:8080`だけで待ち受ける。LANやVPNから操作するときだけ`GAME_SERVER_MANAGER_ADDRESS=0.0.0.0`を設定する。管理ポートをルーターのポート転送などで一般インターネットへ直接公開しない。

管理画面はSpring Securityで保護する。

- 管理者名は`admin`固定
- 初回設定はlocalhostからだけ許可
- パスワードはbcryptハッシュで保存
- CookieセッションとCSRF対策を使用
- 認証ファイルを削除した場合も、再設定はlocalhostからだけ許可

Tailscaleの導入、Tailnet参加、アクセス制御は利用者が行う。VPNを利用していてもGameServerManagerのログイン認証は無効にしない。

## 技術構成

| 分類 | 採用技術 |
|---|---|
| バックエンド | Kotlin、Spring Boot、Java 21 |
| フロントエンド | React、TypeScript、Vite |
| 認証 | Spring Security |
| 永続化 | JSON、JSON Lines、ゲーム固有設定ファイル |
| Windows連携 | PowerShell、Windows Firewall、タスクスケジューラ |
| ゲーム導入・更新 | SteamCMD |
| Palworld管理 | ローカルREST API、プロセス管理 |
| テスト | JUnit 5、MockMvc、Vitest、Testing Library |

初期版ではSQLiteを導入しない。単一マシン・単一管理者・1タイトル1サーバーの規模では、管理ルート内のファイルで必要な状態を管理できるためである。複数ユーザー、権限管理、複雑な検索などが必要になった時点でDB導入を再検討する。

## データ配置

```text
C:\GameServerManager\
├─ app\
├─ tools\
│  └─ steamcmd\
├─ servers\
│  └─ palworld\
│     └─ main\
│        └─ runtime\
├─ backups\
├─ logs\
└─ config\
```

- SteamCMDは`tools`へ配置する
- `force_install_dir`でゲーム本体を`servers`へ配置する
- ワールド、ゲーム設定、ゲームログはゲーム本体配下で管理する
- バックアップはゲーム本体の外にある`backups`へ保存する
- 認証、登録サーバー、自動運転状態は`config`へ保存する
- 操作履歴は`logs`へ保存する
- FirewallやタスクスケジューラはOS側の設定として扱う

既に別の場所へSteamCMDが存在していても競合させず、GameServerManagerは自身の管理ルート内に専用のSteamCMDを準備して使用する。

## Palworldの構築と管理

新規構築は次の単位に分けて実行する。

```text
入力
→ 構築計画の確認
→ パス・容量・ポートの事前検証
→ SteamCMDの準備
→ Palworldのインストール
→ 初期設定の生成
→ 起動確認
→ 管理対象への登録
```

管理対象へ登録した後は、状態確認、起動、保存後停止、再起動、更新、操作履歴を扱う。Palworldの保存と正常停止には、外部公開しないローカルREST APIを使用する。バックアップはPalworld内蔵機能を常時有効化して任せる。

初期版は1タイトルにつき1サーバーとし、Palworldを登録した後は2件目を作成できない。ARKやMinecraftの対応後は、各タイトルをそれぞれ1件ずつ登録できるようにする。

## 自動運転

利用者はPalworld新規作成時に、毎日の停止時刻と起動時刻を設定できる。標準的な運用は次のとおりである。

```text
停止時刻
→ ワールド保存
→ サーバー停止
→ バックアップ
→ 起動時刻まで待機
→ サーバー起動
```

バックアップはサーバー停止中に取得し、最新3個を保持する。手動で停止されたサーバーは勝手に起動せず、自動運転が停止した場合だけ指定時刻に起動する。

## Windows連携

Windows連携は、通常のWebアプリから任意の管理者コマンドを実行する設計にしない。配布用PowerShellは`Plan`、`Install`、`Status`、`Uninstall`の限定された操作だけを提供する。

- GameServerManager導入時に管理画面用TCPポートを許可する
- Windows起動時に制限付きユーザーでGameServerManagerを起動する
- 実ゲームサーバー作成時にゲーム用UDPポートを許可する
- 実ゲームサーバー削除時に、そのサーバー用として作成した規則だけを解除する
- RCONポートとPalworld REST APIポートは外部へ公開しない
- デモ操作ではFirewallやタスクスケジューラを変更しない

配布物とセットアップヘルパーは実装済みであり、開発PCでFirewall規則、起動タスク、実行可能JARの起動を確認済みである。実ゲームサーバーの作成・削除とのFirewall連動は今後接続する。

## デモ開発

SteamCMDやゲームを導入できない開発PCでも、`%TEMP%\GameServerManagerDemo`へ模擬構成を作成して画面と管理フローを確認できる。

デモでは実プロセスやOS設定を変更せず、起動、停止、再起動の状態遷移を模擬する。デモデータは自動削除されないため、管理画面から明示的に削除する。

## 現在地と今後

実装済み：

- 構築フォーム、計画確認、事前検証
- デモ作成、デモ管理、デモ削除
- 初回管理者設定、ログイン、ログアウト、CSRF保護
- 1タイトル1サーバーのJSON登録
- SteamCMD準備とPalworldの構築・管理に必要なバックエンド処理
- 自動停止、停止後バックアップ、3世代保持、自動起動
- Windows配布物、Firewall・自動起動セットアップヘルパー

次に実装・確認する内容：

1. 画面から実Palworldサーバーを一括作成するフロー
2. 作成・削除に連動するゲーム用Firewall規則
3. 実サーバーマシンでのSteamCMD、Palworld、REST API、自動運転の確認
4. Windows設定を確認・制御する管理画面
5. バックアップ復元
6. ARK、Minecraft対応
7. 既存ゲームサーバーの取り込み

最初の完成目標は「1台へ導入すれば、任意コマンドを公開せず、ブラウザからPalworldサーバーを構築・運用できるOSS」とする。
