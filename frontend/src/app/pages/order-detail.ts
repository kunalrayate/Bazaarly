import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Api } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Toast } from '../core/toast.service';

const STEPS = ['PLACED', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED'];
const LABELS: any = { PLACED: 'Order Placed', CONFIRMED: 'Confirmed', PROCESSING: 'Processing', SHIPPED: 'Shipped', OUT_FOR_DELIVERY: 'Out for Delivery', DELIVERED: 'Delivered' };

@Component({
  standalone: true, imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
  @if (o) {
  <div class="row between"><h2>Order {{o.orderNumber}}</h2><a routerLink="/orders">← All orders</a></div>
  <div class="card mb">
    @if (o.status === 'CANCELLED' || o.status === 'RETURNED') {<div class="badge CANCELLED" style="font-size:1rem">Order {{o.status.toLowerCase()}}</div>}
    @else {<div class="track-steps">@for (s of steps; track s; let i = $index) {<div class="tstep" [class.done]="i < idx" [class.cur]="i === idx"><div class="dot">{{i < idx ? '✓' : i+1}}</div>{{labels[s]}}</div>}</div>}
    <div class="row"><span class="badge" [class]="o.paymentStatus">Payment: {{o.paymentStatus}} ({{o.paymentMethod}})</span>
      @if (o.returnStatus !== 'NONE') {<span class="badge" [class]="o.returnStatus">Return: {{o.returnStatus}}</span>}
      <div class="grow"></div>
      @if (o.paymentMethod==='ONLINE' && o.paymentStatus!=='PAID' && o.status!=='CANCELLED') {<button class="btn btn-accent" (click)="payNow()">Pay now</button>}
      @if (canCancel) {<button class="btn btn-danger" (click)="cancel()">Cancel order</button>}
      @if (canReturn) {<button class="btn btn-ghost" (click)="ret()">Request return / refund</button>}</div></div>
  <div class="two"><div class="card"><h3>Items</h3>
    @for (i of o.items; track i.id) {<div class="row mb"><img [src]="api.img(i.image)" style="width:64px;height:64px;object-fit:cover;border-radius:8px" alt="">
      <div class="grow"><a [routerLink]="['/product', i.product.id]">{{i.productName}}</a><div class="small muted">{{i.variantLabel}} · Qty {{i.quantity}} × {{i.unitPrice | currency:'INR':'symbol':'1.0-0'}}</div>
        @if (o.status==='DELIVERED') {<a [routerLink]="['/product', i.product.id]" fragment="reviews" class="small">★ Rate & review this product</a>}</div><b>{{i.lineTotal | currency:'INR':'symbol':'1.0-0'}}</b></div>}
    <h3 class="mt">Tracking history</h3>@for (e of o.timeline.slice().reverse(); track e.id) {<div class="small"><b>{{e.note}}</b> <span class="muted">· {{e.time | date:'medium'}}</span></div>}</div>
  <div><div class="card mb"><h3>Shipping address</h3><b>{{o.shippingName}}</b><div class="small muted">{{o.shippingAddress}}</div></div>
    <div class="card sum"><h3>Payment summary</h3><div><span>Subtotal</span><span>{{o.subtotal | currency:'INR':'symbol':'1.0-0'}}</span></div>
      @if (+o.discount > 0) {<div class="ok"><span>Discount {{o.couponCode}}</span><span>−{{o.discount | currency:'INR':'symbol':'1.0-0'}}</span></div>}
      <div><span>Delivery</span><span>{{+o.shipping ? (o.shipping | currency:'INR':'symbol':'1.0-0') : 'FREE'}}</span></div><div class="tot"><span>Total</span><span>{{o.total | currency:'INR':'symbol':'1.0-0'}}</span></div></div></div></div>
  @if (pay) {<div class="modal-bg"><div class="modal"><h3>Test payment</h3><div class="bigprice">{{o.total | currency:'INR':'symbol':'1.0-2'}}</div>
    <button class="btn btn-primary btn-block" (click)="confirm('SUCCESS')">Pay now (success)</button><button class="btn btn-danger btn-block" (click)="confirm('FAILURE')">Simulate failure</button><button class="btn btn-ghost btn-block" (click)="pay=null">Close</button></div></div>}
  }`
})
export class OrderDetailComponent implements OnInit {
  o: any; pay: any; steps = STEPS; labels = LABELS;
  constructor(public api: Api, private route: ActivatedRoute, private toast: Toast, private auth: AuthService) {}
  get idx() { return STEPS.indexOf(this.o.status); }
  get canCancel() { return this.idx >= 0 && this.idx <= 2; }
  get canReturn() { return this.o.status === 'DELIVERED' && this.o.returnStatus === 'NONE' && (!this.o.deliveredAt || Date.now() - new Date(this.o.deliveredAt).getTime() < 7 * 864e5); }
  ngOnInit() { this.load(); }
  load() { this.api.get('/orders/' + this.route.snapshot.paramMap.get('id')).subscribe({ next: o => this.o = o, error: e => this.toast.error(e) }); }
  private upd(o: any, msg: string) { this.o = o; this.toast.ok(msg); this.auth.refresh(); }
  cancel() { if (confirm('Cancel this order?')) this.api.post('/orders/' + this.o.id + '/cancel').subscribe({ next: o => this.upd(o, 'Order cancelled'), error: e => this.toast.error(e) }); }
  ret() { const reason = prompt('Why do you want to return this order?'); if (reason) this.api.post('/orders/' + this.o.id + '/return', { reason }).subscribe({ next: o => this.upd(o, 'Return requested'), error: e => this.toast.error(e) }); }
  payNow() { this.api.post('/payments/' + this.o.id + '/initiate').subscribe({ next: i => this.pay = i, error: e => this.toast.error(e) }); }
  confirm(status: string) { this.api.post('/payments/' + this.o.id + '/confirm', { paymentRef: this.pay.paymentRef, status }).subscribe({ next: (o: any) => { this.pay = null; this.upd(o, o.paymentStatus === 'PAID' ? 'Payment successful' : 'Payment failed'); }, error: e => this.toast.error(e) }); }
}
