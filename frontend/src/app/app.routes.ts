import { Routes } from '@angular/router';
import { roleGuard } from './core/auth.interceptor';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/home').then(m => m.HomeComponent) },
  { path: 'products', loadComponent: () => import('./pages/products').then(m => m.ProductsComponent) },
  { path: 'product/:id', loadComponent: () => import('./pages/product-detail').then(m => m.ProductDetailComponent) },
  { path: 'login', loadComponent: () => import('./pages/auth').then(m => m.AuthComponent) },
  { path: 'register', loadComponent: () => import('./pages/auth').then(m => m.AuthComponent), data: { mode: 'register' } },
  { path: 'sell', loadComponent: () => import('./pages/auth').then(m => m.AuthComponent), data: { mode: 'seller' } },
  { path: 'cart', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./pages/cart').then(m => m.CartComponent) },
  { path: 'checkout', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./pages/checkout').then(m => m.CheckoutComponent) },
  { path: 'orders', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./pages/orders').then(m => m.OrdersComponent) },
  { path: 'orders/:id', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./pages/order-detail').then(m => m.OrderDetailComponent) },
  { path: 'wishlist', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./pages/wishlist').then(m => m.WishlistComponent) },
  { path: 'account', canActivate: [roleGuard()], loadComponent: () => import('./pages/account').then(m => m.AccountComponent) },
  { path: 'notifications', canActivate: [roleGuard()], loadComponent: () => import('./pages/account').then(m => m.NotificationsComponent) },
  { path: 'seller', canActivate: [roleGuard('SELLER')], loadComponent: () => import('./pages/seller').then(m => m.SellerComponent) },
  { path: 'admin', canActivate: [roleGuard('ADMIN')], loadComponent: () => import('./pages/admin').then(m => m.AdminComponent) },
  { path: '**', redirectTo: '' }
];
