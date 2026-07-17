import { createBrowserRouter } from 'react-router-dom';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { Dashboard } from '@/pages/Dashboard';
import { Accounts } from '@/pages/Accounts';
import { Categories } from '@/pages/Categories';
import { Budgets } from '@/pages/Budgets';
import { Goals } from '@/pages/Goals';
import { Transactions } from '@/pages/Transactions';
import { TransactionFormPage } from '@/pages/TransactionFormPage';
import { Login } from '@/pages/Login';
import { Register } from '@/pages/Register';
import { ForgotPassword } from '@/pages/ForgotPassword';
import { Profile } from '@/pages/Profile';
import { NotFound } from '@/pages/NotFound';

export const router = createBrowserRouter([
  { path: '/login', element: <Login /> },
  { path: '/register', element: <Register /> },
  { path: '/forgot-password', element: <ForgotPassword /> },
  {
    path: '/',
    element: <ProtectedRoute />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'accounts', element: <Accounts /> },
      { path: 'categories', element: <Categories /> },
      { path: 'budgets', element: <Budgets /> },
      { path: 'goals', element: <Goals /> },
      { path: 'transactions', element: <Transactions /> },
      { path: 'transactions/new', element: <TransactionFormPage /> },
      { path: 'transactions/:id/edit', element: <TransactionFormPage /> },
      { path: 'settings/profile', element: <Profile /> },
      { path: '*', element: <NotFound /> },
    ],
  },
]);
