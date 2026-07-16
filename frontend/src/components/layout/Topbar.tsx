import { Bell, Menu } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ThemeToggle } from '@/components/layout/ThemeToggle';
import { HealthStatusWidget } from '@/components/health/HealthStatusWidget';

interface TopbarProps {
  onMenuClick: () => void;
}

export function Topbar({ onMenuClick }: TopbarProps) {
  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-border bg-background/80 px-4 backdrop-blur sm:px-6">
      <Button
        variant="ghost"
        size="icon"
        className="md:hidden"
        onClick={onMenuClick}
        aria-label="Open navigation menu"
      >
        <Menu className="h-5 w-5" aria-hidden="true" />
      </Button>

      <div className="hidden flex-col sm:flex">
        <h1 className="text-sm font-semibold leading-tight">Welcome back, Alex</h1>
        <p className="text-xs text-muted-foreground">Here's your financial overview</p>
      </div>

      <div className="ml-auto flex items-center gap-2">
        <HealthStatusWidget />
        <Button variant="ghost" size="icon" aria-label="Notifications">
          <Bell className="h-5 w-5" aria-hidden="true" />
        </Button>
        <ThemeToggle />
        <div
          className="ml-1 flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-primary to-accent text-sm font-semibold text-primary-foreground"
          aria-label="User menu"
          role="img"
        >
          AX
        </div>
      </div>
    </header>
  );
}
