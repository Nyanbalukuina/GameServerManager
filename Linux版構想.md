# GameServerManager Linux版構想

## 1. 目的

GUIのないLinuxサーバーでもGameServerManagerを簡単に導入し、ゲーム用Windows PCなど、同じネットワーク上にある別PCのブラウザからゲームサーバーを構築・管理できるようにする。

Linuxサーバー上で日常的なコマンド操作を要求せず、コマンドは初期導入と障害対応に限定する。

## 2. 初期対応範囲

- Ubuntu LTS
- x86-64
- 単一マシン
- GUIなしのUbuntu Server
- `.deb`パッケージによる配布
- Javaランタイム同梱
- systemdによる常駐・自動起動
- UFWによるFirewall設定
- 別PCのブラウザから管理

Debian、Fedora・RHEL系、ARM64、Docker、複数マシン管理は初期Linux版の対象外とする。

## 3. 利用者の操作

Linuxサーバー上で行う通常の作業は、インストールと初期設定だけとする。

```bash
sudo apt install ./game-server-manager_0.1.0_amd64.deb
sudo gsm setup
```

`gsm setup`は対話形式で最低限の情報を受け取る。

```text
GameServerManager 初期設定

管理画面の待受アドレス [0.0.0.0]:
管理画面のポート [8080]:
管理者パスワード:
管理者パスワード（確認）:
管理画面用ポートを許可しますか? [Y/n]:

セットアップが完了しました。
管理画面: http://192.168.1.50:8080
```

セットアップ完了後、ゲーム用Windows PCなどのブラウザから表示されたURLを開き、以降の操作を行う。

```text
Ubuntu Server
├─ GameServerManager
├─ SteamCMD
└─ ゲーム専用サーバー
        ▲
        │ LANまたは任意のVPN
        │
別PCのブラウザ
```

Ubuntu Server自身へデスクトップ環境やブラウザをインストールする必要はない。

## 4. 管理画面で行う操作

- 管理者ログイン
- PalworldやARK: Survival Ascendedの新規構築
- ゲーム設定とポートの入力
- 起動、停止、再起動
- SteamCMDによるインストールと更新
- 手動バックアップ
- 自動停止、バックアップ、自動起動
- 状態、ログ、操作履歴の確認
- ゲームサーバー用Firewall規則の管理

Windows版とLinux版で可能な限り同じ管理画面とApplication層の処理を共有する。

## 5. CLIの範囲

初期Linux版では、CLIを導入と障害対応に必要な操作へ限定する。

```bash
sudo gsm setup
gsm status
gsm logs
sudo gsm service start
sudo gsm service stop
sudo gsm service restart
sudo gsm uninstall
```

ゲームサーバーの構築や日常管理は、初期版では管理画面から行う。需要が確認できた場合は、将来同じApplication層またはローカルAPIを利用するゲーム管理CLIを追加する。

## 6. 配布形式

Ubuntu版は`.deb`パッケージとして配布する。

```bash
sudo apt install ./game-server-manager_0.1.0_amd64.deb
```

利用者にJava、Node.js、npmを個別インストールさせない。

- Reactは配布前にビルドし、Spring Bootへ内包する
- GameServerManager専用Javaランタイムをパッケージへ同梱する
- Node.jsとnpmは開発・ビルド環境でのみ使用する
- SteamCMDとゲーム本体は同梱せず、利用時に公式配布元から取得する

将来はGitHub Releasesから`.deb`を取得するインストールスクリプトも検討する。

```bash
curl -fsSL https://example.invalid/game-server-manager/install.sh | sudo bash
```

正式な配布URLが決まるまで、文書や実装へ仮URLを組み込まない。

## 7. ファイル配置

Linuxの標準的な配置に合わせて、アプリ本体と変更されるデータを分離する。

```text
/opt/game-server-manager/
├─ app/
├─ runtime/
└─ bin/

/var/lib/game-server-manager/
├─ tools/
│  └─ steamcmd/
├─ servers/
├─ backups/
├─ config/
└─ logs/

/etc/game-server-manager/
└─ application.conf
```

- `/opt/game-server-manager`にはアプリ本体と専用Javaランタイムを配置する
- `/var/lib/game-server-manager`にはSteamCMD、ゲーム本体、設定、ワールド、バックアップ、ログを配置する
- `/etc/game-server-manager`にはOS側の起動設定を配置する
- GameServerManagerが管理対象外のパスへ書き込まないよう制限する

## 8. サービスと権限

GameServerManager本体をrootで常時実行しない。

`.deb`のインストールまたは`gsm setup`で専用OSユーザーを作成し、systemdからそのユーザーで起動する。

```text
管理者権限を使う処理
├─ パッケージのインストール・削除
├─ systemdサービスの登録
├─ UFW規則の登録・解除
└─ 専用ユーザーと管理フォルダーの作成

通常ユーザーで行う処理
├─ GameServerManager本体の実行
├─ SteamCMDの実行
├─ ゲームサーバーの実行
├─ 設定・ワールド・バックアップの管理
└─ ログの出力
```

Webアプリから任意のsudoコマンドを実行できる機能は設けない。管理者権限が必要な処理は、許可する操作を固定したヘルパーへ分離する。

## 9. ネットワークと認証

- 初期設定は`gsm setup`で行う
- 管理者パスワードは対話入力し、コマンド引数へ含めない
- パスワードはbcryptハッシュとして保存する
- CookieセッションとCSRF対策をWindows版と共通利用する
- 管理画面用ポートだけをLANまたは指定したVPN範囲へ許可する
- 管理画面を一般インターネットへ直接公開しない
- TailscaleなどのVPNを必須依存にしない
- RCONやゲーム管理APIを外部へ公開しない

## 10. SteamCMDとゲームサーバー

SteamCMDはLinux用のValve公式配布元からGameServerManagerが取得する。ゲーム本体はSteamCMDへアプリ側で固定したApp IDを渡してインストールする。

利用者へURLやApp IDを入力させない。

- HTTPSの公式配布元だけを許可する
- ダウンロードサイズを検証する
- アーカイブの不正な相対パスを拒否する
- 展開後に実行ファイルを確認する
- SteamCMDの終了コードとログを確認する
- ゲーム固有の実行ファイルを確認する
- インストール・更新の同時実行を防止する

起動、停止、再起動、バックアップではSteamCMDを使用しない。SteamCMDの排他制御が必要なのは、ゲームのインストールと更新だけである。

## 11. Windows版との共通化

共有する処理：

- 認証
- 入力検証
- 構築計画
- サーバー登録
- SteamCMD実行手順
- ゲーム固有設定
- バックアップ世代管理
- 自動運転
- 操作履歴

OSごとに分離する処理：

- ファイルパス
- プロセス起動・停止
- ポートとFirewall
- 自動起動
- パッケージとサービス管理
- SteamCMD実行ファイル名

```text
共通のWeb・Application・Domain
├─ Windows Infrastructure
└─ Linux Infrastructure
```

## 12. 将来の実装順

Linux版はWindows版とPalworld実機検証が完了した後に着手する。

1. Ubuntu LTS上でのパスとプロセス操作を実装する
2. Linux版SteamCMDとPalworldの手動検証を行う
3. systemdサービスを作成する
4. `gsm setup`を実装する
5. UFWの限定操作ヘルパーを実装する
6. Javaランタイム同梱のLinux配布物を作成する
7. `.deb`パッケージを作成する
8. GUIなしのUbuntu Serverへ新規導入して検証する
9. 別PCのブラウザから構築・管理できることを確認する
10. ARK: Survival Ascendedなど、Linux対応可能なゲームを個別に検討する

## 13. 完成イメージ

```text
利用者がUbuntu Serverへ.debをインストール
→ gsm setupで最低限の設定
→ 管理画面URLを表示
→ 別PCのブラウザでログイン
→ ゲームを選択して構築
→ SteamCMDとゲーム本体を自動導入
→ systemd管理下でゲームサーバーを運用
```

最初のLinux版の目標は「GUIのないUbuntu Serverへ導入し、初期設定後は別PCのブラウザだけでゲームサーバーを構築・運用できること」とする。
