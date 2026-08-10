import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/servers/new/palworld')
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('ゲーム選択画面からPalworld構築画面へ移動する', async () => {
    const user = userEvent.setup()
    window.history.pushState({}, '', '/servers/new')
    render(<App />)

    expect(screen.getByRole('heading', { name: '新規ゲームサーバー構築' })).toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: /Palworld/ }))

    expect(screen.getByRole('heading', { name: '新規Palworldサーバー構築' })).toBeInTheDocument()
  })

  it('新規サーバー構築フォームを表示する', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: '新規Palworldサーバー構築' })).toBeInTheDocument()
    expect(screen.getByLabelText('サーバー名')).toBeInTheDocument()
    expect(screen.getByLabelText('インストール先')).toHaveValue(
      'C:\\GameServerManager\\servers\\palworld\\main\\runtime',
    )
    expect(screen.getByLabelText('SteamCMDの保存先')).toHaveValue(
      'C:\\GameServerManager\\tools\\steamcmd',
    )
    expect(screen.getByRole('button', { name: '構築計画を確認' })).toBeInTheDocument()
  })

  it('APIが成功した場合は構築計画を表示する', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        response({
          serverName: 'Palworld Server',
          installPath: 'C:\\GameServerManager\\servers\\palworld\\main\\runtime',
          steamCmdPath: 'C:\\GameServerManager\\tools\\steamcmd',
          gamePort: 8211,
          rconPort: 25575,
          maxPlayers: 3,
          serverPasswordConfigured: false,
          adminPasswordConfigured: true,
        }),
      ),
    )
    render(<App />)

    await user.type(screen.getByLabelText('サーバー名'), 'Palworld Server')
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
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        response(
          { errors: { serverName: 'サーバー名を入力してください' } },
          400,
        ),
      ),
    )
    render(<App />)

    await user.click(screen.getByRole('button', { name: '構築計画を確認' }))

    expect(await screen.findByText('サーバー名を入力してください')).toBeInTheDocument()
  })

  it('事前検証後にパスワードを最終送信してデモ構築結果を表示する', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(
        response({
          serverName: 'Palworld Server',
          installPath: 'C:\\GameServerManager\\servers\\palworld\\main\\runtime',
          steamCmdPath: 'C:\\GameServerManager\\tools\\steamcmd',
          gamePort: 8211,
          rconPort: 25575,
          maxPlayers: 3,
          serverPasswordConfigured: false,
          adminPasswordConfigured: true,
        }),
      )
      .mockResolvedValueOnce(
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
      )
      .mockResolvedValueOnce(
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
      )
    vi.stubGlobal(
      'fetch',
      fetchMock,
    )
    render(<App />)

    await user.type(screen.getByLabelText('サーバー名'), 'Palworld Server')
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

    await user.click(screen.getByRole('button', { name: 'デモ構築を実行' }))

    expect(await screen.findByText('デモ構築が完了しました')).toBeInTheDocument()
    expect(screen.getByText('デモSteamCMDを一時領域へ配置しました')).toBeInTheDocument()
    const finalRequest = JSON.parse(String(fetchMock.mock.calls[2][1]?.body))
    expect(finalRequest.adminPassword).toBe('admin-password')
    expect(screen.queryByText('admin-password')).not.toBeInTheDocument()
  })
})

function response(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
