import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Api } from '../core/api.service';
import { Toast } from '../core/toast.service';

@Component({
  standalone: true, imports: [CurrencyPipe, DatePipe, RouterLink],
  template: `
  <h2>My orders</h2>
  @for (o of orders; track o.id) {<div class="card mb"><div class="row between"><div><b>{{o.orderNumber}}</b> <span class="muted small">placed {{o.createdAt | date:'mediumDate'}}</span></div>
      <div class="row"><span class="badge" [class]="o.status">{{o.status.replaceAll('_',' ')}}</span><span class="badge" [class]="o.paymentStatus">Payment {{o.paymentStatus}}</span></div></div>
    <div class="row mt">@for (i of o.items.slice(0,4); track i.id) {<img [src]="api.img(i.image)" style="width:56px;height:56px;object-fit:cover;border-radius:6px" [title]="i.productName" alt="">}
      @if (o.items.length > 4) {<span class="muted">+{{o.items.length-4}} more</span>}<div class="grow"></div><div class="right"><b>{{o.total | currency:'INR':'symbol':'1.0-0'}}</b><br><a [routerLink]="['/orders', o.id]">Track / details ›</a></div></div></div>}
  @if (loaded && !orders.length) {<div class="card">No orders yet. <a routerLink="/products">Start shopping</a></div>}`
})
export class OrdersComponent implements OnInit {
  orders: any[] = []; loaded = false;
  constructor(public api: Api, private toast: Toast) {}
  ngOnInit() { this.api.get('/orders').subscribe({ next: r => { this.orders = r; this.loaded = true; }, error: e => this.toast.error(e) }); }
}
