import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Api } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Toast } from '../core/toast.service';

@Component({
  selector: 'app-stars', standalone: true,
  template: `<span class="stars" [title]="(value||0) + ' out of 5'"><span class="stars-fill" [style.width.%]="(value||0)/5*100">★★★★★</span>★★★★★</span>`
})
export class StarsComponent { @Input() value = 0; }

@Component({
  selector: 'app-pager', standalone: true,
  template: `@if (totalPages > 1) {<div class="pager"><button class="btn btn-ghost" [disabled]="page===0" (click)="change.emit(page-1)">‹ Prev</button>
    <span>Page {{page+1}} of {{totalPages}}</span><button class="btn btn-ghost" [disabled]="page+1>=totalPages" (click)="change.emit(page+1)">Next ›</button></div>}`
})
export class PagerComponent { @Input() page = 0; @Input() totalPages = 0; @Output() change = new EventEmitter<number>(); }

@Component({
  selector: 'app-bars', standalone: true, imports: [CurrencyPipe],
  template: `<div class="bars">@for (d of data; track d.date) {<div class="bar-col" [title]="d.date + ': ' + (d.revenue | currency:'INR':'symbol':'1.0-0') + ' · ' + d.orders + ' ' + unit">
      <div class="bar" [style.height.%]="max ? (d.revenue / max * 100) : 0"></div><span>{{d.date.slice(8)}}</span></div>}</div>`
})
export class BarsComponent {
  @Input() data: any[] = []; @Input() unit = 'orders';
  get max() { return Math.max(0, ...this.data.map(d => +d.revenue)); }
}

@Component({
  selector: 'app-product-card', standalone: true, imports: [RouterLink, CurrencyPipe, StarsComponent],
  template: `
  <article class="pcard">
    <a [routerLink]="['/product', p.id]" class="pimg"><img [src]="api.img(p.images?.[0])" [alt]="p.name" loading="lazy">
      @if (p.discountPercent > 0) {<span class="badge-off">{{p.discountPercent}}% off</span>}</a>
    @if (!auth.loggedIn || auth.role === 'CUSTOMER') {<button class="heart" [class.on]="auth.isWished(p.id)" (click)="wish($event)" title="Wishlist" aria-label="Toggle wishlist">♥</button>}
    <div class="pbody">
      <div class="brand">{{p.brand}}</div>
      <a [routerLink]="['/product', p.id]" class="pname">{{p.name}}</a>
      <div class="rating"><app-stars [value]="p.ratingAvg"/> <small>({{p.ratingCount}})</small></div>
      <div class="price"><b>{{p.sellingPrice | currency:'INR':'symbol':'1.0-0'}}</b> @if (p.discountPercent > 0) {<s>{{p.price | currency:'INR':'symbol':'1.0-0'}}</s>}</div>
      @if (p.stock === 0) {<div class="stock-out">Out of stock</div>} @else if (p.lowStock) {<div class="stock-low">Only {{p.stock}} left</div>} @else {<div class="stock-ok">In stock</div>}
      @if (!auth.loggedIn || auth.role === 'CUSTOMER') {
        @if (p.variants?.length) {<a class="btn btn-ghost btn-block" [routerLink]="['/product', p.id]">Select options</a>}
        @else {<button class="btn btn-primary btn-block" [disabled]="p.stock === 0" (click)="add()">Add to cart</button>}
      }
    </div>
  </article>`
})
export class ProductCardComponent {
  @Input() p: any;
  constructor(public api: Api, public auth: AuthService, private toast: Toast, private router: Router) {}
  private needLogin() { if (!this.auth.loggedIn) { this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } }); return true; } return false; }
  wish(e: Event) { e.preventDefault(); if (this.needLogin()) return; this.auth.toggleWish(this.p.id).subscribe({ next: () => this.toast.ok(this.auth.isWished(this.p.id) ? 'Added to wishlist' : 'Removed from wishlist'), error: e => this.toast.error(e) }); }
  add() { if (this.needLogin()) return; this.api.post('/cart', { productId: this.p.id, quantity: 1 }).subscribe({ next: () => { this.toast.ok('Added to cart'); this.auth.refreshCart(); }, error: e => this.toast.error(e) }); }
}

@Component({
  selector: 'app-shelf', standalone: true, imports: [RouterLink, ProductCardComponent],
  template: `@if (items?.length) {<section class="shelf"><div class="shelf-head"><h2>{{title}}</h2>@if (link) {<a [routerLink]="link" [queryParams]="params">See all ›</a>}</div>
    <div class="shelf-row">@for (p of items; track p.id) {<app-product-card [p]="p"/>}</div></section>}`
})
export class ShelfComponent { @Input() title = ''; @Input() items: any[] = []; @Input() link: any; @Input() params: any; }
