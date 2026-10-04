import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Toast } from '../core/toast.service';
import { ShelfComponent } from '../shared/ui';

@Component({
  standalone: true, imports: [RouterLink, ShelfComponent],
  template: `
  @if (d) {
  <section class="hero"><div><h1>Everything you need.<br>One cart away.</h1><p>Electronics, fashion, groceries, books and furniture from trusted sellers - with easy returns and doorstep delivery.</p>
      <a class="btn btn-accent" routerLink="/products">Start shopping</a>@if (!auth.loggedIn) {&nbsp;<a class="btn btn-ghost" routerLink="/register">Create account</a>}</div>
    <div class="coupon-stack">@for (c of d.promotions.slice(0,3); track c.id) {<div class="promo"><b>{{c.code}}</b><div class="small">{{c.description}}</div></div>}</div></section>
  <div class="cats">@for (c of d.categories; track c.id) {<a class="cat" routerLink="/products" [queryParams]="{category: c.id}"><span>{{c.icon}}</span>{{c.name}}</a>}</div>
  <app-shelf title="Recommended for you" [items]="d.recommended"/>
  <app-shelf title="Today's deals" [items]="d.deals" link="/products" [params]="{sort:'popularity'}"/>
  <app-shelf title="Featured picks" [items]="d.featured"/>
  <app-shelf title="Trending now" [items]="d.trending"/>
  <app-shelf title="Best sellers" [items]="d.bestSellers" link="/products" [params]="{sort:'popularity'}"/>
  <app-shelf title="New arrivals" [items]="d.newArrivals" link="/products" [params]="{sort:'newest'}"/>
  @if (recent.length) {<app-shelf title="Recently viewed" [items]="recent"/>}
  } @else {<p class="muted">Loading the bazaar...</p>}`
})
export class HomeComponent implements OnInit {
  d: any; recent: any[] = [];
  constructor(private api: Api, public auth: AuthService, private toast: Toast) {}
  ngOnInit() {
    this.api.get('/home').subscribe({ next: r => this.d = r, error: e => this.toast.error(e) });
    if (this.auth.role === 'CUSTOMER') this.api.get('/products/recently-viewed').subscribe(r => this.recent = r);
  }
}
