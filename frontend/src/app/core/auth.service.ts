import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { Api } from './api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  user = signal<any>(JSON.parse(localStorage.getItem('user') || 'null'));
  cartCount = signal(0);
  wishIds = signal<number[]>([]);
  unread = signal(0);
  coupon = localStorage.getItem('coupon') || '';

  constructor(private api: Api, private router: Router) { if (this.user()) this.refresh(); }

  get role(): string { return this.user()?.role; }
  get loggedIn() { return !!this.user(); }

  login(email: string, password: string) { return this.api.post('/auth/login', { email, password }).pipe(tap(r => this.setSession(r))); }
  registerCustomer(body: any) { return this.api.post('/auth/register', body).pipe(tap(r => this.setSession(r))); }

  setSession(r: any) { localStorage.setItem('token', r.token); localStorage.setItem('user', JSON.stringify(r.user)); this.user.set(r.user); this.refresh(); }
  updateUser(u: any) { localStorage.setItem('user', JSON.stringify(u)); this.user.set(u); }
  logout() { localStorage.removeItem('token'); localStorage.removeItem('user'); this.user.set(null); this.cartCount.set(0); this.wishIds.set([]); this.unread.set(0); this.setCoupon(''); this.router.navigate(['/']); }
  setCoupon(c: string) { this.coupon = c; c ? localStorage.setItem('coupon', c) : localStorage.removeItem('coupon'); }

  refresh() {
    if (!this.user()) return;
    this.api.get('/notifications/unread-count').subscribe({ next: r => this.unread.set(r.count), error: () => {} });
    if (this.role === 'CUSTOMER') {
      this.refreshCart();
      this.api.get('/wishlist/ids').subscribe({ next: r => this.wishIds.set(r), error: () => {} });
    }
  }
  refreshCart() { this.api.get('/cart/count').subscribe({ next: r => this.cartCount.set(r.count), error: () => {} }); }

  isWished(id: number) { return this.wishIds().includes(id); }
  toggleWish(id: number) {
    const on = this.isWished(id);
    return (on ? this.api.del('/wishlist/' + id) : this.api.post('/wishlist/' + id)).pipe(tap(() => this.wishIds.update(l => on ? l.filter(x => x !== id) : [...l, id])));
  }
}
