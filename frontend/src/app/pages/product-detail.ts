import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe, KeyValuePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Toast } from '../core/toast.service';
import { ShelfComponent, StarsComponent, ProductCardComponent } from '../shared/ui';

@Component({
  standalone: true, imports: [CurrencyPipe, DatePipe, KeyValuePipe, FormsModule, RouterLink, StarsComponent, ShelfComponent, ProductCardComponent],
  template: `
  @if (p) {
  <div class="detail">
    <div class="gallery"><div class="main"><img [src]="api.img(p.images[sel])" [alt]="p.name"></div>
      <div class="thumbs">@for (im of p.images; track $index) {<img [src]="api.img(im)" [class.on]="$index===sel" (click)="sel=$index" alt="thumbnail">}</div></div>
    <div>
      <div class="muted small">{{p.category?.name}} · by {{p.brand}} · sold by {{p.sellerName}}</div>
      <h1>{{p.name}}</h1>
      <div class="row"><app-stars [value]="p.ratingAvg"/> <a href="#reviews">{{p.ratingAvg}} · {{p.ratingCount}} reviews</a></div>
      <div class="mt"><span class="bigprice">{{price | currency:'INR':'symbol':'1.0-0'}}</span>
        @if (p.discountPercent > 0) {<s class="muted">&nbsp;{{p.price | currency:'INR':'symbol':'1.0-0'}}</s> <span class="badge" style="background:#fde2d8;color:#c2410c">{{p.discountPercent}}% off</span>}</div>
      @if (p.discountEndsAt) {<div class="err small">⏳ Limited-time deal ends {{p.discountEndsAt | date:'medium'}}</div>}
      <p>{{p.description}}</p>
      @for (t of types; track t) {<div class="mt"><b>{{t}}:</b> <span class="muted">{{chosen[t]?.value}}</span><div class="chips mt">
        @for (v of optionsOf(t); track v.id) {<button class="chip" [class.on]="chosen[t]?.id===v.id" (click)="chosen[t]=v">{{v.value}}@if (+v.priceAdjustment) { (+{{v.priceAdjustment | currency:'INR':'symbol':'1.0-0'}})}</button>}</div></div>}
      <div class="mt">@if (p.stock === 0) {<b class="stock-out">Out of stock</b>} @else if (p.lowStock) {<b class="stock-low">Hurry, only {{p.stock}} left!</b>} @else {<b class="stock-ok">In stock</b>}</div>
      @if (!auth.loggedIn || auth.role === 'CUSTOMER') {
      <div class="row mt"><div class="qty"><button (click)="qty=max(1,qty-1)">−</button><span>{{qty}}</span><button (click)="qty=min(10,qty+1)">+</button></div>
        <button class="btn btn-primary" [disabled]="p.stock===0" (click)="add(false)">Add to cart</button>
        <button class="btn btn-accent" [disabled]="p.stock===0" (click)="add(true)">Buy now</button>
        <button class="btn btn-ghost" (click)="wish()">{{auth.isWished(p.id) ? '♥ Wishlisted' : '♡ Wishlist'}}</button></div>}
      <p class="small muted mt">🚚 Free delivery on orders above ₹500 · ↩ 7-day easy returns · 🔒 Secure payments</p>
      <h3 class="mt">Specifications</h3><table class="spec">@for (s of p.specifications | keyvalue; track s.key) {<tr><td>{{s.key}}</td><td>{{s.value}}</td></tr>}</table>
    </div>
  </div>
  @if (fbt.length) {<section class="card mt"><h3>Frequently bought together</h3><div class="grid g4">@for (x of fbt; track x.id) {<app-product-card [p]="x"/>}</div></section>}
  <section class="card mt" id="reviews"><h2>Customer reviews</h2>
    @if (rv) {<div class="grid g2"><div><div class="bigprice">{{rv.average}} <small class="muted" style="font-size:1rem">/ 5</small></div><app-stars [value]="rv.average"/> <span class="muted">{{rv.count}} ratings</span></div>
      <div>@for (n of [5,4,3,2,1]; track n) {<div class="dist"><span>{{n}}★</span><div class="track"><div class="fill" [style.width.%]="rv.count ? rv.distribution[n]/rv.count*100 : 0"></div></div><span>{{rv.distribution[n]}}</span></div>}</div></div>
      @if (canReview) {<div class="card mt"><h3>Write a review</h3><div class="chips">@for (n of [1,2,3,4,5]; track n) {<button class="chip" [class.on]="nr.rating===n" (click)="nr.rating=n">{{n}} ★</button>}</div>
        <label class="f">Title</label><input [(ngModel)]="nr.title"><label class="f">Your review</label><textarea [(ngModel)]="nr.comment"></textarea>
        <button class="btn btn-primary mt" (click)="submitReview()">Submit review</button></div>}
      @for (r of rv.reviews; track r.id) {<div class="review"><app-stars [value]="r.rating"/> <b>{{r.title}}</b><div class="muted small">{{r.userName}} · {{r.createdAt | date:'mediumDate'}} · Verified purchase</div><p>{{r.comment}}</p>
        @if (auth.loggedIn) {<button class="btn btn-ghost btn-sm" (click)="report(r)">Report</button>}</div>}
      @if (!rv.reviews.length) {<p class="muted mt">No reviews yet.</p>}}</section>
  <app-shelf title="Similar products" [items]="similar"/>
  } @else {<p class="muted">Loading...</p>}`
})
export class ProductDetailComponent implements OnInit {
  p: any; sel = 0; qty = 1; chosen: any = {}; fbt: any[] = []; similar: any[] = []; rv: any; canReview = false; nr = { rating: 5, title: '', comment: '' };
  constructor(public api: Api, public auth: AuthService, private route: ActivatedRoute, private router: Router, private toast: Toast) {}
  max = Math.max; min = Math.min;
  get types(): string[] { return [...new Set<string>((this.p?.variants || []).map((v: any) => v.type))]; }
  optionsOf(t: string) { return this.p.variants.filter((v: any) => v.type === t); }
  get price() { return Object.values(this.chosen).reduce((s: number, v: any) => s + (+v.priceAdjustment || 0), this.p.sellingPrice) as number; }
  ngOnInit() { this.route.paramMap.subscribe(m => this.load(+m.get('id')!)); }
  load(id: number) {
    this.p = null; this.sel = 0; this.qty = 1; this.chosen = {};
    this.api.get('/products/' + id).subscribe({ next: p => { this.p = p; this.types.forEach(t => this.chosen[t] = this.optionsOf(t)[0]); }, error: e => { this.toast.error(e); this.router.navigate(['/']); } });
    this.api.get('/products/' + id + '/frequently-bought').subscribe(r => this.fbt = r);
    this.api.get('/products/' + id + '/similar').subscribe(r => this.similar = r);
    this.loadReviews(id);
    if (this.auth.loggedIn) this.api.get('/products/' + id + '/can-review').subscribe(r => this.canReview = r.canReview);
  }
  loadReviews(id: number) { this.api.get('/products/' + id + '/reviews').subscribe(r => this.rv = r); }
  private guard() { if (!this.auth.loggedIn) { this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } }); return false; } return true; }
  add(buy: boolean) {
    if (!this.guard()) return;
    this.api.post('/cart', { productId: this.p.id, quantity: this.qty, variantIds: Object.values(this.chosen).map((v: any) => v.id) }).subscribe({
      next: () => { this.auth.refreshCart(); if (buy) this.router.navigate(['/cart']); else this.toast.ok('Added to cart'); }, error: e => this.toast.error(e) });
  }
  wish() { if (!this.guard()) return; this.auth.toggleWish(this.p.id).subscribe({ next: () => this.toast.ok(this.auth.isWished(this.p.id) ? 'Added to wishlist' : 'Removed'), error: e => this.toast.error(e) }); }
  submitReview() { this.api.post('/products/' + this.p.id + '/reviews', this.nr).subscribe({ next: () => { this.toast.ok('Thanks for your review!'); this.canReview = false; this.loadReviews(this.p.id); }, error: e => this.toast.error(e) }); }
  report(r: any) { const reason = prompt('Why are you reporting this review?'); if (reason) this.api.post('/reviews/' + r.id + '/report', { reason }).subscribe({ next: x => this.toast.ok(x.message), error: e => this.toast.error(e) }); }
}
