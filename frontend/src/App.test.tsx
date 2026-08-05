import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('新規サーバー構築フォームを表示する', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: '新規Palworldサーバー構築' })).toBeInTheDocument()
    expect(screen.getByLabelText('サーバー名')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '構築計画を確認' })).toBeInTheDocument()
  })

  it('APIが成功した場合は構築計画を表示する', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        response({
          serverName: 'Palworld Server',
          installPath: 'C:\\GameServers\\Palworld',
          steamCmdPath: 'C:\\GameServers\\SteamCMD',
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
    await user.type(screen.getByLabelText('インストール先'), 'C:\\GameServers\\Palworld')
    await user.type(screen.getByLabelText('SteamCMDの保存先'), 'C:\\GameServers\\SteamCMD')
    await user.type(screen.getByLabelText('管理者パスワード'), 'admin-password')
    await user.click(screen.getByRole('button', { name: '構築計画を確認' }))

    expect(await screen.findByRole('heading', { name: '構築計画' })).toBeInTheDocument()
    expect(screen.getByText('C:\\GameServers\\Palworld')).toBeInTheDocument()
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

  it('事前検証結果を一覧表示する', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn()
        .mockResolvedValueOnce(
          response({
            serverName: 'Palworld Server',
            installPath: 'C:\\GameServers\\Palworld',
            steamCmdPath: 'C:\\GameServers\\SteamCMD',
            gamePort: 8211,
            rconPort: 25575,
            maxPlayers: 3,
            serverPasswordConfigured: false,
            adminPasswordConfigured: true,
          }),
        )
        .mockResolvedValueOnce(
          response({
            canProceed: true,
            checks: [
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
        ),
    )
    render(<App />)

    await user.type(screen.getByLabelText('サーバー名'), 'Palworld Server')
    await user.type(screen.getByLabelText('インストール先'), 'C:\\GameServers\\Palworld')
    await user.type(screen.getByLabelText('SteamCMDの保存先'), 'C:\\GameServers\\SteamCMD')
    await user.type(screen.getByLabelText('管理者パスワード'), 'admin-password')
    await user.click(screen.getByRole('button', { name: '構築計画を確認' }))
    await screen.findByRole('heading', { name: '構築計画' })
    await user.click(screen.getByRole('button', { name: '事前検証を実行' }))

    expect(await screen.findByText('構築を進められる環境です')).toBeInTheDocument()
    expect(screen.getByText('ゲームポート UDP 8211')).toBeInTheDocument()
    expect(screen.getByText('SteamCMDは構築時にダウンロードされます')).toBeInTheDocument()
  })
})

function response(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
