import { Bell, LogOut, Menu } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ThemeToggle } from '@/components/layout/ThemeToggle';
import { HealthStatusWidget } from '@/components/health/HealthStatusWidget';
import { useToast } from '@/components/ui/toast';
import { useAuth } from '@/context/AuthProvider';
import { useCurrentUser } from '@/hooks/useCurrentUser';
import { getInitials } from '@/lib/utils';

interface TopbarProps {
  onMenuClick: () => void;
}

function getGreeting(date: Date): string {
  const hour = date.getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
}

export function Topbar({ onMenuClick }: TopbarProps) {
  const { logout } = useAuth();
  const { data: user } = useCurrentUser();
  const navigate = useNavigate();
  const { toast } = useToast();

  const handleLogout = async () => {
    await logout();
    toast({ title: 'Signed out', tone: 'default' });
    navigate('/login', { replace: true });
  };

  const displayName = user?.fullName ?? user?.email ?? 'there';
  const firstName = displayName.split(/\s+/)[0] || 'there';
  const greeting = getGreeting(new Date());
  const initials = getInitials(user?.fullName, user?.email ?? '');

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-border glass px-4 sm:px-6">
      <Button
        variant="ghost"
        size="icon"
        className="md:hidden"
        onClick={onMenuClick}
        aria-label="Open navigation menu"
      >
        <Menu className="h-5 w-5" aria-hidden="true" />
      </Button>

      <div className="hidden min-w-0 flex-col sm:flex">
        <h1 className="truncate text-base font-semibold leading-tight">
          {greeting}, {firstName}
        </h1>
        <p className="text-xs text-muted-foreground">Here's your financial overview</p>
      </div>

      <div className="ml-auto flex items-center gap-2">
        <HealthStatusWidget />
        <Button variant="ghost" size="icon" aria-label="Notifications">
          <Bell className="h-5 w-5" aria-hidden="true" />
        </Button>
        <ThemeToggle />
        <div className="hidden items-center gap-2.5 lg:flex">
          <div className="text-right leading-tight">
            <p className="text-sm font-semibold">{displayName}</p>
            <p className="max-w-[14rem] truncate text-xs text-muted-foreground">{user?.email}</p>
          </div>
        </div>
        <div
          className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-primary to-accent text-sm font-semibold text-primary-foreground ring-2 ring-white/10"
          aria-label={displayName}
          role="img"
        >
          {initials}
        </div>
        <Button variant="outline" size="sm" onClick={handleLogout} className="hidden sm:inline-flex">
          <LogOut className="h-4 w-4" aria-hidden="true" />
          Sign out
        </Button>
        <Button
          variant="ghost"
          size="icon"
          onClick={handleLogout}
          aria-label="Sign out"
          className="sm:hidden"
        >
          <LogOut className="h-5 w-5" aria-hidden="true" />
        </Button>
      </div>
    </header>
  );
}
