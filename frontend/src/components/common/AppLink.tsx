import type { MouseEvent, ReactNode } from 'react'

type AppLinkProps = {
  className?: string
  href: string
  children: ReactNode
}

export function AppLink({ className, href, children }: AppLinkProps) {
  const navigate = (event: MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault()
    window.history.pushState({}, '', href)
    window.dispatchEvent(new PopStateEvent('popstate'))
  }

  return (
    <a className={className} href={href} onClick={navigate}>
      {children}
    </a>
  )
}
