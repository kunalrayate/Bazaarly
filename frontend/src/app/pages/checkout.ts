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
  <h2>Checkout</h2>
  @if (c && c.lines.length) {
  <div class="two"><div>
    <section class="card mb"><h3>1. Delivery address</h3>
      @for (a of addrs; track a.id) {<label class="addr" [class.on]="sel===a.id"><input type="radio" name="a" [checked]="sel===a.id" (change)="sel=a.id"><b>{{a.fullName}}</b> · {{a.phone}}<div class="small muted">{{a.line1}}, {{a.line2}} {{a.city}}, {{a.state}} - {{a.pincode}}</div></label>}
      <button class="btn btn-ghost btn-sm" (click)="adding=!adding">+ Add new address</button>
      @if (adding) {<div class="grid g2 mt"><div><label class="f">Full name</label><input [(ngModel)]="na.fullName"></div><div><label class="f">Phone</label><input [(ngModel)]="na.phone"></div>
        <div><label class="f">Address line 1</label><input [(ngModel)]="na.line1"></div><div><label class="f">Line 2 / landmark</label><input [(ngModel)]="na.line2"></div>
        <div><label class="f">City</label><input [(ngModel)]="na.city"></div><div><label class="f">State</label><input [(ngModel)]="na.state"></div><div><label class="f">Pincode</label><input [(ngModel)]="na.pincode"></div></div>
        <button class="btn btn-primary mt" (click)="saveAddr()">Save address</button>}</section>
    <section class="card mb"><h3>2. Payment method</h3>
      <label class="addr" [class.on]="method==='ONLINE'"><input type="radio" name="m" [checked]="method==='ONLINE'" (change)="method='ONLINE'"><b>Pay online</b> <span class="badge">TEST MODE</span><div class="small muted">Cards, UPI, netbanking (simulated checkout - swap for Razorpay)</div></label>
      <label class="addr" [class.on]="method==='COD'"><input type="radio" name="m" [checked]="method==='COD'" (change)="method='COD'"><b>Cash on delivery</b></label></section>
    <section class="card"><h3>3. Review items</h3>@for (l of c.lines; track l.cartItemId) {<div class="row mb"><img [src]="api.img(l.product.images[0])" style="width:56px;height:56px;object-fit:cover;border-radius:6px" alt="">
      <div class="grow">{{l.product.name}}<div class="small muted">{{l.variantLabel}} Qty {{l.quantity}}</div></div><b>{{l.lineTotal | currency:'INR':'symbol':'1.0-0'}}</b></div>}</section>
  </div>
  <aside class="card"><h3>Order total</h3><div class="sum"><div><span>Items</span><span>{{c.subtotal | currency:'INR':'symbol':'1.0-0'}}</span></div>
    @if (+c.discount>0) {<div class="ok"><span>Discount ({{c.couponCode}})</span><span>−{{c.discount | currency:'INR':'symbol':'1.0-0'}}</span></div>}
    <div><span>Delivery</span><span>{{+c.shipping ? (c.shipping | currency:'INR':'symbol':'1.0-0') : 'FREE'}}</span></div><div class="tot"><span>Total</span><span>{{c.total | currency:'INR':'symbol':'1.0-0'}}</span></div></div>
    <button class="btn btn-accent btn-block" [disabled]="busy || !sel" (click)="place()">{{method==='ONLINE' ? 'Place order & pay' : 'Place order'}}</button>
    <a routerLink="/cart" class="small">← Back to cart</a></aside></div>
  } @else if (c) {<div class="card">Nothing to checkout. <a routerLink="/products">Browse products</a></div>}

  @if (pay) {<div class="modal-bg"><div class="modal"><h3>Test payment</h3><p class="muted small">This is a simulated gateway. No real money is charged. Replace with Razorpay in production.</p>
    <div class="card" style="background:#faf6ee"><div class="small muted">Order {{pay.order.orderNumber}}</div><div class="bigprice">{{pay.order.total | currency:'INR':'symbol':'1.0-2'}}</div><div class="small">Test card: 4111 1111 1111 1111 · any future date · any CVV</div></div>
    <button class="btn btn-primary btn-block" [disabled]="busy" (click)="complete('SUCCESS')">Pay now (success)</button>
    <button class="btn btn-danger btn-block" [disabled]="busy" (click)="complete('FAILURE')">Simulate failed payment</button>
    <button class="btn btn-ghost btn-block" (click)="later()">Pay later from My Orders</button></div></div>}`
})
export class CheckoutComponent implements OnInit {
  c: any; addrs: any[] = []; sel: number | null = null; method = 'ONLINE'; adding = false; busy = false; pay: any; na: any = {};
  constructor(public api: Api, private auth: AuthService, private toast: Toast, private router: Router) {}
  ngOnInit() {
    this.api.get('/cart', { coupon: this.auth.coupon }).subscribe(r => this.c = r);
    this.loadAddrs();
  }
  loadAddrs(select?: number) { this.api.get('/addresses').subscribe(a => { this.addrs = a; this.sel = select || this.sel || a[0]?.id || null; this.adding = !a.length; }); }
  saveAddr() { this.api.post('/addresses', this.na).subscribe({ next: (a: any) => { this.na = {}; this.loadAddrs(a.id); this.adding = false; }, error: e => this.toast.error(e) }); }
  place() {
    this.busy = true;
    this.api.post('/orders', { addressId: this.sel, couponCode: this.c.couponCode, paymentMethod: this.method }).subscribe({
      next: (o: any) => { this.busy = false; this.auth.setCoupon(''); this.auth.refresh();
        if (this.method === 'ONLINE') { this.api.post('/payments/' + o.id + '/initiate').subscribe({ next: init => this.pay = { order: o, init }, error: e => this.toast.error(e) }); }
        else { this.toast.ok('Order placed!'); this.router.navigate(['/orders', o.id]); } },
      error: e => { this.busy = false; this.toast.error(e); } });
  }
  complete(status: string) {
    this.busy = true;
    this.api.post('/payments/' + this.pay.order.id + '/confirm', { paymentRef: this.pay.init.paymentRef, status }).subscribe({
      next: (o: any) => { this.busy = false; const id = this.pay.order.id; this.pay = null; o.paymentStatus === 'PAID' ? this.toast.ok('Payment successful!') : this.toast.error('Payment failed. You can retry from My Orders.'); this.auth.refresh(); this.router.navigate(['/orders', id]); },
      error: e => { this.busy = false; this.toast.error(e); } });
  }
  later() { const id = this.pay.order.id; this.pay = null; this.router.navigate(['/orders', id]); }
}
