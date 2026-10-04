import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Toast } from '../core/toast.service';

@Component({
  standalone: true, imports: [CurrencyPipe, FormsModule, RouterLink],
  template: `
  <h2>Shopping cart</h2>
  @if (c && c.lines.length) {
  <div class="two"><div>
    @for (l of c.lines; track l.cartItemId) {<div class="card mb row">
      <a [routerLink]="['/product', l.product.id]"><img [src]="api.img(l.product.images[0])" style="width:96px;height:96px;object-fit:cover;border-radius:8px" alt=""></a>
      <div class="grow"><a [routerLink]="['/product', l.product.id]"><b>{{l.product.name}}</b></a>
        @if (l.variantLabel) {<div class="small muted">{{l.variantLabel}}</div>}
        @if (!l.available) {<div class="err small">Not enough stock - reduce quantity</div>}
        <div class="row mt"><div class="qty"><button (click)="qty(l, l.quantity-1)" [disabled]="l.quantity<=1">−</button><span>{{l.quantity}}</span><button (click)="qty(l, l.quantity+1)">+</button></div>
          <button class="btn btn-ghost btn-sm" (click)="save(l)">Save for later</button><button class="btn btn-danger btn-sm" (click)="remove(l)">Remove</button></div></div>
      <div class="right"><b>{{l.lineTotal | currency:'INR':'symbol':'1.0-0'}}</b><div class="small muted">{{l.unitPrice | currency:'INR':'symbol':'1.0-0'}} each</div></div></div>}
  </div>
  <aside class="card"><h3>Order summary</h3>
    <div class="row"><input placeholder="Coupon code" [(ngModel)]="code" style="flex:1"><button class="btn btn-ghost" (click)="apply()">Apply</button></div>
    @if (c.couponError) {<div class="err small mt">{{c.couponError}}</div>}
    @if (c.couponCode) {<div class="ok small mt">✔ {{c.couponCode}} applied <a href="javascript:void(0)" (click)="clear()">remove</a></div>}
    @if (coupons.length && !c.couponCode) {<div class="chips mt">@for (x of coupons; track x.id) {<button class="chip small" [title]="x.description" (click)="code=x.code; apply()">{{x.code}}</button>}</div>}
    <div class="sum mt"><div><span>Subtotal ({{c.itemCount}} items)</span><span>{{c.subtotal | currency:'INR':'symbol':'1.0-0'}}</span></div>
      @if (+c.discount > 0) {<div class="ok"><span>Coupon discount</span><span>−{{c.discount | currency:'INR':'symbol':'1.0-0'}}</span></div>}
      <div><span>Delivery</span><span>{{+c.shipping ? (c.shipping | currency:'INR':'symbol':'1.0-0') : 'FREE'}}</span></div>
      <div class="tot"><span>Total</span><span>{{c.total | currency:'INR':'symbol':'1.0-0'}}</span></div></div>
    @if (+c.shipping > 0) {<p class="small muted">Add {{500 - (+c.subtotal - +c.discount) | currency:'INR':'symbol':'1.0-0'}} more for free delivery.</p>}
    <button class="btn btn-accent btn-block" [disabled]="blocked" (click)="checkout()">Proceed to checkout</button></aside></div>
  } @else if (c) {<div class="card">Your cart is empty. <a routerLink="/products">Continue shopping</a></div>}`
})
export class CartComponent implements OnInit {
  c: any; code = ''; coupons: any[] = [];
  constructor(public api: Api, private auth: AuthService, private toast: Toast, private router: Router) {}
  get blocked() { return this.c.lines.some((l: any) => !l.available); }
  ngOnInit() { this.code = this.auth.coupon; this.load(); this.api.get('/coupons/active').subscribe(r => this.coupons = r); }
  load() { this.api.get('/cart', { coupon: this.code }).subscribe({ next: r => { this.c = r; this.auth.setCoupon(r.couponCode || ''); if (!r.couponCode && r.couponError) this.code = ''; this.auth.refreshCart(); }, error: e => this.toast.error(e) }); }
  qty(l: any, q: number) { this.api.put('/cart/' + l.cartItemId, { quantity: q }).subscribe({ next: () => this.load(), error: e => this.toast.error(e) }); }
  remove(l: any) { this.api.del('/cart/' + l.cartItemId).subscribe(() => this.load()); }
  save(l: any) { this.api.post('/cart/' + l.cartItemId + '/move-to-wishlist').subscribe(() => { this.toast.ok('Moved to wishlist'); this.auth.refresh(); this.load(); }); }
  apply() { this.load(); }
  clear() { this.code = ''; this.auth.setCoupon(''); this.load(); }
  checkout() { this.router.navigate(['/checkout']); }
}
