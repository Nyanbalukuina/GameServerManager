import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/servers/new/palworld')
    vi.stubGlobal('fetch', authenticatedFetch())
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('ゲーム選択画面からPalworld構築画面へ移動する', async () => {
    const user = userEvent.setup()
    window.history.pushState({}, '', '/servers/new')
    render(<App />)

    expect(await screen.findByRole('heading', { name: '新規ゲームサーバー構築' })).toBeInTheDocument()
    await user.click(await screen.findByRole('link', { name: /^Palworld未作成/ }))

    expect(screen.getByRole('heading', { name: '新規Palworldサーバー構築' })).toBeInTheDocument()
  })

  it('新規サーバー構築フォームを表示する', async () => {
    render(<App />)

    expect(await screen.findByRole('heading', { name: '新規Palworldサーバー構築' })).toBeInTheDocument()
    expect(screen.getByLabelText('サーバー名')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByLabelText('Palworldサーバーのインストール先')).toHaveValue(
        'C:\\GameServerManager\\servers\\palworld\\main\\runtime',
      )
      expect(screen.getByLabelText('SteamCMDの保存先')).toHaveValue(
        'C:\\GameServerManager\\tools\\steamcmd',
      )
    })
    expect(screen.getByRole('button', { name: '構築計画を確認' })).toBeInTheDocument()
    expect(screen.getByLabelText('自動運転を有効にする')).not.toBeChecked()
    expect(screen.getByLabelText('毎日の停止時刻')).toHaveValue('04:00')
    expect(screen.getByLabelText('毎日の起動時刻')).toHaveValue('09:00')
  })

  it('未設定の場合はlocalhost用の初回設定画面を表示する', async () => {
    vi.stubGlobal('fetch', authenticationFetch({
      configured: false,
      authenticated: false,
      username: null,
      setupAllowed: true,
    }))

    render(<App />)

    expect(await screen.findByRole('heading', { name: '初回管理者設定' })).toBeInTheDocument()
    expect(screen.getByLabelText('確認用パスワード')).toBeInTheDocument()
  })

  it('未ログインの場合はログイン画面を表示する', async () => {
    vi.stubGlobal('fetch', authenticationFetch({
      configured: true,
      authenticated: false,
      username: null,
      setupAllowed: false,
    }))

    render(<App />)

    expect(await screen.findByRole('heading', { name: 'ログイン' })).toBeInTheDocument()
    expect(screen.queryByLabelText('確認用パスワード')).not.toBeInTheDocument()
  })

  it('作成済みPalworldの管理画面を表示する', async () => {
    window.history.pushState({}, '', '/servers/palworld')
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Palworldサーバー管理' })).toBeInTheDocument()
    expect(await screen.findByText('Demo Palworld')).toBeInTheDocument()
    expect(screen.getByText('停止中')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '起動' })).toBeEnabled()
  })

  it('APIが成功した場合は構築計画を表示する', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', authenticatedFetch([
      response({
          serverName: 'Palworld Server',
          installPath: 'C:\\GameServerManager\\servers\\palworld\\main\\runtime',
          steamCmdPath: 'C:\\GameServerManager\\tools\\steamcmd',
          gamePort: 8211,
          rconPort: 25575,
          maxPlayers: 3,
          serverPasswordConfigured: false,
          adminPasswordConfigured: true,
          automationEnabled: true,
          shutdownTime: '04:00',
          startupTime: '09:00',
          backupAfterShutdown: true,
          backupRetentionCount: 3,
      }),
    ]))
    render(<App />)

    await user.type(await screen.findByLabelText('サーバー名'), 'Palworld Server')
    await user.type(screen.getByLabelText('管理者パスワード'), 'admin-password')
    await user.click(screen.getByRole('button', { name: '構築計画を確認' }))

    expect(await screen.findByRole('heading', { name: '構築計画' })).toBeInTheDocument()
    expect(
      screen.getByText('C:\\GameServerManager\\servers\\palworld\\main\\runtime'),
    ).toBeInTheDocument()
    expect(screen.queryByText('admin-password')).not.toBeInTheDocument()
  })

  it('APIの入力エラーをフォームへ表示する', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', authenticatedFetch([
      response(
          { errors: { serverName: 'サーバー名を入力してください' } },
          400,
      ),
    ]))
    render(<App />)

    await user.click(await screen.findByRole('button', { name: '構築計画を確認' }))

    expect(await screen.findByText('サーバー名を入力してください')).toBeInTheDocument()
  })

  it('事前検証後にパスワードを最終送信してデモ構築結果を表示する', async () => {
    const user = userEvent.setup()
    const fetchMock = authenticatedFetch([
        response({
          serverName: 'Palworld Server',
          installPath: 'C:\\GameServerManager\\servers\\palworld\\main\\runtime',
          steamCmdPath: 'C:\\GameServerManager\\tools\\steamcmd',
          gamePort: 8211,
          rconPort: 25575,
          maxPlayers: 3,
          serverPasswordConfigured: false,
          adminPasswordConfigured: true,
          automationEnabled: true,
          shutdownTime: '04:00',
          startupTime: '09:00',
          backupAfterShutdown: true,
          backupRetentionCount: 3,
        }),
        response({
          canProceed: false,
          checks: [
            {
              id: 'installPath.writable',
              label: 'インストール先の書き込み権限',
              status: 'ERROR',
              message: '作成先へ書き込みできません',
            },
            {
              id: 'port.game',
              label: 'ゲームポート UDP 8211',
              status: 'PASS',
              message: '使用できます',
            },
            {
              id: 'steamcmd.installed',
              label: 'SteamCMD',
              status: 'WARNING',
              message: 'SteamCMDは構築時にダウンロードされます',
            },
          ],
        }),
        response({
          completed: true,
          mode: 'DEMO',
          workspacePath: 'C:\\Temp\\game-server-manager-demo',
          steps: [
            {
              id: 'steamcmd',
              label: 'SteamCMDの準備',
              status: 'COMPLETED',
              message: 'デモSteamCMDを一時領域へ配置しました',
            },
          ],
        }),
    ])
    vi.stubGlobal(
      'fetch',
      fetchMock,
    )
    render(<App />)

    await user.type(await screen.findByLabelText('サーバー名'), 'Palworld Server')
    await user.type(screen.getByLabelText('管理者パスワード'), 'admin-password')
    await user.click(screen.getByRole('button', { name: '構築計画を確認' }))
    await screen.findByRole('heading', { name: '構築計画' })
    await user.click(screen.getByRole('button', { name: '事前検証を実行' }))

    expect(await screen.findByText('修正が必要な項目があります')).toBeInTheDocument()
    expect(screen.getByText('ゲームポート UDP 8211')).toBeInTheDocument()
    expect(screen.getByText('SteamCMDは構築時にダウンロードされます')).toBeInTheDocument()
    expect(
      screen.getByText('実構築には修正が必要ですが、デモ構築は一時領域で実行できます。'),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'デモサーバーを作成' }))

    expect(await screen.findByText('デモサーバーを作成しました')).toBeInTheDocument()
    expect(screen.getByText('デモSteamCMDを一時領域へ配置しました')).toBeInTheDocument()
    const finalCall = fetchMock.mock.calls.find(([input]) => String(input).includes('/demo'))
    const finalRequest = JSON.parse(String(finalCall?.[1]?.body))
    expect(finalRequest.adminPassword).toBe('admin-password')
    expect(screen.queryByText('admin-password')).not.toBeInTheDocument()
  })

  it('事前検証成功後に実Palworldサーバーを構築して起動する', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', authenticatedFetch([
      response({
        serverName: 'Palworld Server',
        installPath: 'C:\\GameServerManager\\servers\\palworld\\main\\runtime',
        steamCmdPath: 'C:\\GameServerManager\\tools\\steamcmd',
        gamePort: 8211,
        rconPort: 25575,
        maxPlayers: 3,
        serverPasswordConfigured: false,
        adminPasswordConfigured: true,
        automationEnabled: false,
        shutdownTime: '04:00',
        startupTime: '09:00',
        backupAfterShutdown: true,
        backupRetentionCount: 3,
      }),
      response({ canProceed: true, checks: [] }),
      response({
        completed: true,
        mode: 'REAL',
        installPath: 'C:\\GameServerManager\\servers\\palworld\\main\\runtime',
        steps: [{
          id: 'start',
          label: 'Palworldサーバーの起動',
          status: 'COMPLETED',
          message: 'ゲームポートの待受を確認しました',
        }],
      }),
    ]))
    render(<App />)

    await user.type(await screen.findByLabelText('サーバー名'), 'Palworld Server')
    await user.type(screen.getByLabelText('管理者パスワード'), 'admin-password')
    await user.click(screen.getByRole('button', { name: '構築計画を確認' }))
    await user.click(await screen.findByRole('button', { name: '事前検証を実行' }))
    await screen.findByText('構築を進められる環境です')
    await user.click(screen.getByRole('button', { name: 'Palworldサーバーを構築して起動' }))

    expect(await screen.findByText('Palworldサーバーを構築して起動しました')).toBeInTheDocument()
    expect(screen.getByText('ゲームポートの待受を確認しました')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Palworld管理画面を開く' })).toBeInTheDocument()
  })
})

function response(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function authenticatedFetch(applicationResponses: Response[] = []) {
  const queue = [...applicationResponses]
  return vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) => {
    const url = String(input)
    if (url === '/api/auth/status') {
      return response({
        configured: true,
        authenticated: true,
        username: 'admin',
        setupAllowed: false,
      })
    }
    if (url === '/api/auth/csrf') {
      return response({ token: 'test-csrf-token', headerName: 'X-CSRF-TOKEN' })
    }
    if (url === '/api/configuration/storage') {
      return response({
        root: 'C:\\GameServerManager',
        palworldInstallPath: 'C:\\GameServerManager\\servers\\palworld\\main\\runtime',
        steamCmdPath: 'C:\\GameServerManager\\tools\\steamcmd',
      })
    }
    if (url === '/api/servers') {
      return response([])
    }
    if (url === '/api/servers/palworld') {
      return response(demoPalworldServer())
    }
    if (url === '/api/palworld/automation') {
      return response({
        settings: {
          enabled: false,
          shutdownTime: '04:00',
          startupTime: '09:00',
          backupAfterShutdown: true,
          gamePort: 8211,
          maxPlayers: 3,
        },
        runtime: {
          stoppedBySchedule: false,
          lastShutdownCycle: null,
          lastBackupCycle: null,
          lastStartupCycle: null,
          lastError: null,
        },
        serverInDowntime: false,
        nextAction: '自動運転は無効です',
      })
    }
    return queue.shift() ?? response({})
  })
}

function demoPalworldServer() {
  return {
    game: 'PALWORLD',
    serverId: 'palworld-main',
    mode: 'DEMO',
    state: 'STOPPED',
    serverName: 'Demo Palworld',
    installPath: 'C:\\GameServerManager\\servers\\palworld\\main\\runtime',
    workspacePath: 'C:\\Temp\\GameServerManagerDemo',
    gamePort: 8211,
    rconPort: 25575,
    createdAt: '2026-08-10T00:00:00Z',
  }
}

function authenticationFetch(status: {
  configured: boolean
  authenticated: boolean
  username: string | null
  setupAllowed: boolean
}) {
  return vi.fn(async (input: RequestInfo | URL) => {
    if (String(input) === '/api/auth/status') {
      return response(status)
    }
    return response({ token: 'test-csrf-token', headerName: 'X-CSRF-TOKEN' })
  })
}
