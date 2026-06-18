import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/catalog/explorer').then((m) => m.Explorer) },
  { path: 'producto/:id', loadComponent: () => import('./features/catalog/producto').then((m) => m.Producto) },
  { path: 'login', loadComponent: () => import('./features/auth/login').then((m) => m.Login) },
  { path: 'registro', loadComponent: () => import('./features/auth/registro').then((m) => m.Registro) },
  {
    path: 'perfil',
    canActivate: [authGuard],
    loadComponent: () => import('./features/perfil/perfil').then((m) => m.Perfil),
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
  { path: '**', redirectTo: '' },
];
