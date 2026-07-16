import { BarChart3, CreditCard, LayoutDashboard, PieChart, Receipt, Wallet } from 'lucide-react';
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
  { label: 'Transactions', icon: Receipt, soon: true },
  { label: 'Budgets', icon: Wallet, soon: true },
  { label: 'Categories', icon: PieChart, soon: true },
  { label: 'Analytics', icon: BarChart3, soon: true },
  { label: 'Accounts', icon: CreditCard, soon: true },
];

interface SidebarProps {
  onNavigate?: () => void;
}

export function Sidebar({ onNavigate }: SidebarProps) {
  const { toast } = useToast();

  return (
    <div className="flex h-full flex-col gap-2 bg-surface px-3 py-5">
      <div className="mb-4 flex items-center gap-2 px-2">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-primary to-accent text-primary-foreground">
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
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary/15 text-primary'
                    : 'text-muted-foreground hover:bg-surface-2 hover:text-foreground',
                )
              }
            >
              <item.icon className="h-5 w-5" aria-hidden="true" />
              {item.label}
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
              className="flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground/70 transition-colors hover:bg-surface-2"
            >
              <item.icon className="h-5 w-5" aria-hidden="true" />
              {item.label}
              <Badge variant="outline" className="ml-auto">
                Soon
              </Badge>
            </button>
          ),
        )}
      </nav>

      <div className="rounded-md bg-surface-2 px-3 py-3 text-xs text-muted-foreground">
        <p className="font-medium text-foreground">Nova Foundation</p>
        <p>Phase 1 · v0.1.0</p>
      </div>
    </div>
  );
}
