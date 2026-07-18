import {
  ArrowLeftRight,
  BarChart3,
  Camera,
  CreditCard,
  LayoutDashboard,
  PieChart,
  Settings,
  Sparkles,
  Target,
  Wallet,
} from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { useToast } from '@/components/ui/toast';

interface NavItem {
  label: string;
  icon: typeof LayoutDashboard;
  to?: string;
  soon?: boolean;
}

const navItems: NavItem[] = [
  { label: 'Dashboard', icon: LayoutDashboard, to: '/' },
  { label: 'Transactions', icon: ArrowLeftRight, to: '/transactions' },
  { label: 'Receipts', icon: Camera, to: '/receipts' },
  { label: 'Accounts', icon: CreditCard, to: '/accounts' },
  { label: 'Categories', icon: PieChart, to: '/categories' },
  { label: 'Budgets', icon: Wallet, to: '/budgets' },
  { label: 'Goals', icon: Target, to: '/goals' },
  { label: 'Analytics', icon: BarChart3, to: '/analytics' },
  { label: 'Ask Nova AI', icon: Sparkles, to: '/copilot' },
  { label: 'Profile', icon: Settings, to: '/settings/profile' },
];

interface SidebarProps {
  onNavigate?: () => void;
}

export function Sidebar({ onNavigate }: SidebarProps) {
  const { toast } = useToast();

  return (
    <div className="glass flex h-full flex-col gap-2 px-3 py-5">
      <div className="mb-4 flex items-center gap-2.5 px-2">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-accent text-primary-foreground shadow-[0_6px_18px_-6px_rgb(var(--primary)/0.7)]">
          <span className="text-lg font-extrabold">N</span>
        </div>
        <div className="leading-tight">
          <p className="text-base font-bold tracking-tight">Nova</p>
          <p className="text-[11px] uppercase tracking-wider text-muted-foreground">Finance</p>
        </div>
      </div>

      <nav aria-label="Primary" className="flex flex-1 flex-col gap-1">
        {navItems.map((item) =>
          item.to ? (
            <NavLink
              key={item.label}
              to={item.to}
              end
              onClick={onNavigate}
              className={({ isActive }) =>
                cn(
                  'group relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all duration-200 ease-premium',
                  isActive
                    ? 'bg-primary/10 text-primary shadow-[inset_0_0_0_1px_rgb(var(--primary)/0.18)]'
                    : 'text-muted-foreground hover:translate-x-0.5 hover:bg-surface-2/60 hover:text-foreground',
                )
              }
            >
              {({ isActive }) => (
                <>
                  {isActive && (
                    <span
                      className="absolute left-0 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-r-full bg-primary"
                      aria-hidden="true"
                    />
                  )}
                  <item.icon
                    className={cn(
                      'h-5 w-5 shrink-0 transition-transform duration-200 ease-premium',
                      !isActive && 'group-hover:scale-110',
                    )}
                    aria-hidden="true"
                  />
                  {item.label}
                </>
              )}
            </NavLink>
          ) : (
            <button
              key={item.label}
              type="button"
              onClick={() => {
                toast({
                  title: `${item.label} coming soon`,
                  description: 'This area arrives in a future Nova release.',
                  tone: 'default',
                });
                onNavigate?.();
              }}
              className="group relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-muted-foreground/70 transition-all duration-200 ease-premium hover:translate-x-0.5 hover:bg-surface-2/60 hover:text-foreground"
            >
              <item.icon className="h-5 w-5 shrink-0 transition-transform duration-200 ease-premium group-hover:scale-110" aria-hidden="true" />
              {item.label}
              <Badge variant="outline" className="ml-auto">
                Soon
              </Badge>
            </button>
          ),
        )}
      </nav>

      <div className="rounded-xl border border-border/60 bg-surface-2/50 px-3 py-3 text-xs text-muted-foreground">
        <p className="font-medium text-foreground">Nova Finance</p>
        <p>Phase 6 · v0.8.0</p>
      </div>
    </div>
  );
}
