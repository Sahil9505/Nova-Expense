import { RouterProvider } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider } from '@/context/ThemeProvider';
import { ToastProvider } from '@/components/ui/toast';
import { AuthProvider } from '@/context/AuthProvider';
import { Atmosphere } from '@/components/layout/Atmosphere';
import { queryClient } from '@/lib/queryClient';
import { router } from '@/routes/router';

export function App() {
  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <AuthProvider>
            <Atmosphere />
            <RouterProvider router={router} />
          </AuthProvider>
        </ToastProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}
