import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Api } from './core/api.service';
import { AuthService } from './core/auth.service';
import { Toast } from './core/toast.service';

@Component({
  selector: 'app-root', standalone: true, imports: [RouterOutlet, RouterLink, FormsModule],
  template: `
  <header class="topbar"><div class="container">
    <a routerLink="/" class="logo">bazaarly<i>.</i></a>
    @if (auth.role !== 'SELLER' && auth.role !== 'ADMIN') {
      <form class="search" (ngSubmit)="search()">
        <select [(ngModel)]="cat" name="cat"><option value="">All</option>@for (c of cats(); track c.id) {<option [value]="c.id">{{c.name}}</option>}</select>
        <input [(ngModel)]="q" name="q" placeholder="Search products, brands and more" aria-label="Search"><button type="submit">Search</button></form>
    } @else {<div class="grow"></div>}
    <nav class="nav-links">
      @if (!auth.loggedIn) {<a routerLink="/sell" class="hide-sm">Sell on Bazaarly</a><a routerLink="/login">Login</a>}
      @else {
        <a routerLink="/notifications" title="Notifications">🔔@if (auth.unread() > 0) {<span class="cnt">{{auth.unread()}}</span>}</a>
        @if (auth.role === 'SELLER') {<a routerLink="/seller">Seller Hub</a>}
        @if (auth.role === 'ADMIN') {<a routerLink="/admin">Admin Console</a>}
        @if (auth.role === 'CUSTOMER') {<a routerLink="/orders" class="hide-sm">Orders</a><a routerLink="/wishlist">♡<span class="cnt">{{auth.wishIds().length}}</span></a>}
        <a routerLink="/account" class="hide-sm">Hi, {{auth.user().name.split(' ')[0]}}</a><button class="lnk" (click)="auth.logout()">Logout</button>
      }
      @if (!auth.loggedIn || auth.role === 'CUSTOMER') {<a routerLink="/cart">🛒 Cart@if (auth.cartCount() > 0) {<span class="cnt">{{auth.cartCount()}}</span>}</a>}
    </nav></div></header>
  @if (auth.role !== 'SELLER' && auth.role !== 'ADMIN') {<div class="catbar"><div class="container"><a routerLink="/products">All products</a>
    @for (c of cats(); track c.id) {<a routerLink="/products" [queryParams]="{category: c.id}">{{c.name}}</a>}
    <a routerLink="/products" [queryParams]="{sort:'newest'}">New arrivals</a></div></div>}
  <main class="container page"><router-outlet/></main>
  <footer class="footer"><div class="container"><div><div class="logo">bazaarly<i>.</i></div><p>Everything you need, delivered to your door.</p></div>
    <div>Demo project · Spring Boot · JPA · MySQL · Angular<br>Payments run in test mode.</div></div></footer>
  <div class="toasts">@for (t of toast.items(); track t.id) {<div class="toast" [class.err]="t.type==='err'">{{t.msg}}</div>}</div>`
})
export class AppComponent implements OnInit {
  cats = signal<any[]>([]); q = ''; cat = '';
  constructor(public auth: AuthService, public toast: Toast, private api: Api, private router: Router) {}
  ngOnInit() { this.api.get('/categories').subscribe({ next: c => this.cats.set(c), error: e => this.toast.error(e) }); }
  search() { this.router.navigate(['/products'], { queryParams: { q: this.q || null, category: this.cat || null } }); }
}
