import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/catalog/explorer').then((m) => m.Explorer) },
  { path: 'producto/:id', loadComponent: () => import('./features/catalog/producto').then((m) => m.Producto) },
  { path: 'carrito', loadComponent: () => import('./features/cart/cart').then((m) => m.Cart) },
  {
    path: 'pedidos',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/orders').then((m) => m.Orders),
  },
  {
    path: 'favoritos',
    canActivate: [authGuard],
    loadComponent: () => import('./features/wishlist/favoritos').then((m) => m.Favoritos),
  },
  {
    path: 'mis-ventas',
    canActivate: [roleGuard('SELLER')],
    loadComponent: () => import('./features/seller/mis-ventas').then((m) => m.MisVentas),
  },
  { path: 'login', loadComponent: () => import('./features/auth/login').then((m) => m.Login) },
  { path: 'register', loadComponent: () => import('./features/auth/register').then((m) => m.Register) },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/profile').then((m) => m.Profile),
  },
  {
    path: 'vender',
    canActivate: [authGuard],
    loadComponent: () => import('./features/seller/vender').then((m) => m.Vender),
  },
  {
    path: 'mi-tienda',
    canActivate: [roleGuard('SELLER')],
    loadComponent: () => import('./features/seller/mi-tienda').then((m) => m.MiTienda),
  },
  {
    path: 'admin',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: () => import('./features/admin/admin-panel').then((m) => m.AdminPanel),
  },
  { path: '**', redirectTo: '' },
];
