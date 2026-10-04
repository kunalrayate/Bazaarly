import { Component, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api.service';
import { Toast } from '../core/toast.service';
import { BarsComponent, PagerComponent } from '../shared/ui';

const FLOW = ['PLACED', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED'];

@Component({
  standalone: true, imports: [CurrencyPipe, DatePipe, FormsModule, BarsComponent, PagerComponent],
  template: `
  <h2>Admin Console</h2>
  <div class="tabs">@for (t of tabs; track t[0]) {<button [class.on]="tab===t[0]" (click)="go(t[0])">{{t[1]}}@if (t[0]==='sellers' && st?.pendingSellers) { ({{st.pendingSellers}})}</button>}</div>

  @if (tab==='dash' && st) {
    <div class="grid g-stat"><div class="card stat"><small>Revenue</small><b>{{st.revenue | currency:'INR':'symbol':'1.0-0'}}</b></div><div class="card stat"><small>Orders</small><b>{{st.totalOrders}}</b></div><div class="card stat"><small>Customers</small><b>{{st.customers}}</b></div>
      <div class="card stat"><small>Active sellers</small><b>{{st.sellers}}</b></div><div class="card stat"><small>Products</small><b>{{st.products}}</b></div><div class="card stat"><small>Pending sellers</small><b>{{st.pendingSellers}}</b></div></div>
    <div class="grid g2 mt"><div class="card"><h3>Sales trend (14 days)</h3><app-bars [data]="st.salesTrend"/></div>
      <div class="card"><h3>Order statistics</h3>@for (k of keys(st.statusCounts); track k) {<div class="row between small"><span>{{k.replaceAll('_',' ')}}</span><b>{{st.statusCounts[k]}}</b></div>}</div>
      <div class="card"><h3>Most popular products</h3>@for (p of st.popularProducts; track p.id) {<div class="row between small"><span>{{p.name}}</span><b>{{p.sold}} sold</b></div>}</div>
      <div class="card"><h3>Popular categories</h3>@for (c of st.popularCategories; track c.name) {<div class="row between small"><span>{{c.name}}</span><b>{{c.units}} units</b></div>}</div>
      <div class="card"><h3>⚠ Low-stock products</h3>@for (p of st.lowStock; track p.id) {<div class="row between small"><span>{{p.name}}</span><b class="err">{{p.stock}}</b></div>}</div></div>
  }

  @if (tab==='customers' || tab==='sellers') {
    <div class="row mb"><input placeholder="Search name / email / store" [(ngModel)]="uq" (keyup.enter)="loadUsers(0)" style="max-width:300px"><button class="btn btn-ghost" (click)="loadUsers(0)">Search</button>
      @if (tab==='sellers') {<select [(ngModel)]="ustatus" (ngModelChange)="loadUsers(0)" style="width:auto"><option value="">All</option><option value="PENDING">Pending approval</option><option value="ACTIVE">Active</option><option value="REJECTED">Rejected</option><option value="BLOCKED">Blocked</option></select>}</div>
    <div class="card tbl"><table><tr><th>Name</th><th>Email</th><th>Phone</th>@if (tab==='sellers') {<th>Store</th>}<th>Joined</th><th>Status</th><th>Actions</th></tr>
      @for (u of users; track u.id) {<tr><td>{{u.name}}</td><td>{{u.email}}</td><td>{{u.phone}}</td>@if (tab==='sellers') {<td>{{u.storeName}}<div class="small muted">{{u.gstin}}</div></td>}<td>{{u.createdAt | date:'mediumDate'}}</td><td><span class="badge" [class]="u.status">{{u.status}}</span></td>
        <td>@if (u.status==='PENDING') {<button class="btn btn-primary btn-sm" (click)="setUser(u,'ACTIVE')">Approve</button> <button class="btn btn-danger btn-sm" (click)="setUser(u,'REJECTED')">Reject</button>}
          @if (u.status==='ACTIVE') {<button class="btn btn-danger btn-sm" (click)="setUser(u,'BLOCKED')">Block</button>}@if (u.status==='BLOCKED' || u.status==='REJECTED') {<button class="btn btn-ghost btn-sm" (click)="setUser(u,'ACTIVE')">Activate</button>}</td></tr>}</table></div>
    <app-pager [page]="pg" [totalPages]="total" (change)="loadUsers($event)"/>
  }

  @if (tab==='categories') {
    <div class="card mb row"><input placeholder="Icon (emoji)" [(ngModel)]="cat.icon" style="max-width:110px"><input placeholder="Category name" [(ngModel)]="cat.name" style="max-width:240px"><input placeholder="Description" [(ngModel)]="cat.description" style="max-width:320px">
      <button class="btn btn-primary" (click)="saveCat()">{{cat.id ? 'Update' : 'Add'}} category</button>@if (cat.id) {<button class="btn btn-ghost" (click)="cat={}">Cancel</button>}</div>
    <div class="card tbl"><table><tr><th>ID</th><th>Name</th><th>Description</th><th></th></tr>@for (c of cats; track c.id) {<tr><td>{{c.id}}</td><td>{{c.icon}} {{c.name}}</td><td>{{c.description}}</td><td><button class="btn btn-ghost btn-sm" (click)="editCat(c)">Edit</button> <button class="btn btn-danger btn-sm" (click)="delCat(c)">Delete</button></td></tr>}</table></div>
  }

  @if (tab==='products') {
    <div class="row mb"><input placeholder="Search products" [(ngModel)]="uq" (keyup.enter)="loadProducts(0)" style="max-width:300px"><button class="btn btn-ghost" (click)="loadProducts(0)">Search</button></div>
    <div class="card tbl"><table><tr><th></th><th>Product</th><th>Seller</th><th>Price</th><th>Stock</th><th>Listing</th><th>Featured</th></tr>
      @for (p of items; track p.id) {<tr><td><img [src]="api.img(p.images[0])" alt=""></td><td>{{p.name}}<div class="small muted">{{p.category?.name}} · {{p.brand}}</div></td><td>{{p.sellerName}}</td><td>{{p.sellingPrice | currency:'INR':'symbol':'1.0-0'}}</td><td [class.err]="p.lowStock">{{p.stock}}</td>
        <td><button class="btn btn-sm" [class.btn-ghost]="p.active" [class.btn-danger]="p.active" (click)="flag(p,{active:!p.active})">{{p.active ? 'Unlist' : 'Relist'}}</button></td>
        <td><button class="btn btn-ghost btn-sm" (click)="flag(p,{featured:!p.featured})">{{p.featured ? '★ Featured' : '☆ Feature'}}</button></td></tr>}</table></div>
    <app-pager [page]="pg" [totalPages]="total" (change)="loadProducts($event)"/>
  }

  @if (tab==='orders') {
    <div class="row mb"><select [(ngModel)]="ustatus" (ngModelChange)="loadOrders(0)" style="width:auto"><option value="">All statuses</option>@for (s of allStatuses; track s) {<option [value]="s">{{s.replaceAll('_',' ')}}</option>}</select></div>
    <div class="card tbl"><table><tr><th>Order</th><th>Customer</th><th>Date</th><th>Total</th><th>Payment</th><th>Status</th><th>Actions</th></tr>
      @for (o of items; track o.id) {<tr><td>{{o.orderNumber}}<div class="small muted">{{o.items.length}} item(s)</div></td><td>{{o.user.name}}</td><td>{{o.createdAt | date:'short'}}</td><td>{{o.total | currency:'INR':'symbol':'1.0-0'}}</td><td><span class="badge" [class]="o.paymentStatus">{{o.paymentMethod}} {{o.paymentStatus}}</span></td>
        <td><span class="badge" [class]="o.status">{{o.status.replaceAll('_',' ')}}</span>@if (o.returnStatus!=='NONE') {<br><span class="badge" [class]="o.returnStatus">Return {{o.returnStatus}}</span>}</td>
        <td>@if (next(o).length) {<select #ns style="width:130px">@for (n of next(o); track n) {<option [value]="n">{{n.replaceAll('_',' ')}}</option>}</select> <button class="btn btn-primary btn-sm" (click)="setOrder(o, ns.value)">Set</button>}
          @if (o.returnStatus==='REQUESTED') {<div class="small">{{o.returnReason}}</div><button class="btn btn-primary btn-sm" (click)="decide(o,true)">Refund</button> <button class="btn btn-danger btn-sm" (click)="decide(o,false)">Reject</button>}</td></tr>}</table></div>
    <app-pager [page]="pg" [totalPages]="total" (change)="loadOrders($event)"/>
  }

  @if (tab==='coupons') {
    <div class="card mb"><h3>{{cp.id ? 'Edit coupon' : 'New coupon / offer'}}</h3><div class="grid g4">
      <div><label class="f">Code</label><input [(ngModel)]="cp.code"></div><div><label class="f">Description</label><input [(ngModel)]="cp.description"></div>
      <div><label class="f">Type</label><select [(ngModel)]="cp.type"><option value="PERCENT">Percentage %</option><option value="FIXED">Fixed ₹</option></select></div><div><label class="f">Value</label><input type="number" [(ngModel)]="cp.value"></div>
      <div><label class="f">Min purchase ₹</label><input type="number" [(ngModel)]="cp.minPurchase"></div><div><label class="f">Max discount ₹ (for %)</label><input type="number" [(ngModel)]="cp.maxDiscount"></div>
      <div><label class="f">Applies to</label><select [(ngModel)]="cp.scope"><option value="ALL">Entire cart</option><option value="CATEGORY">A category</option><option value="PRODUCT">A product</option></select></div>
      <div><label class="f">{{cp.scope==='CATEGORY' ? 'Category' : 'Product ID'}}</label>@if (cp.scope==='CATEGORY') {<select [(ngModel)]="cp.scopeId">@for (c of cats; track c.id) {<option [ngValue]="c.id">{{c.name}}</option>}</select>} @else {<input type="number" [(ngModel)]="cp.scopeId" [disabled]="cp.scope==='ALL'">}</div>
      <div><label class="f">Starts</label><input type="datetime-local" [(ngModel)]="cp.startsAt"></div><div><label class="f">Expires</label><input type="datetime-local" [(ngModel)]="cp.expiresAt"></div>
      <div><label class="f">Usage limit</label><input type="number" [(ngModel)]="cp.usageLimit"></div><div><label class="f">Active</label><input type="checkbox" [(ngModel)]="cp.active"></div></div>
      <button class="btn btn-primary mt" (click)="saveCoupon()">Save coupon</button> @if (cp.id) {<button class="btn btn-ghost mt" (click)="newCoupon()">Cancel</button>}</div>
    <div class="card tbl"><table><tr><th>Code</th><th>Offer</th><th>Scope</th><th>Min</th><th>Used</th><th>Expires</th><th></th></tr>
      @for (c of coupons; track c.id) {<tr><td><b>{{c.code}}</b> @if (!c.active) {<span class="badge BLOCKED">off</span>}</td><td>{{c.type==='PERCENT' ? c.value + '%' : '₹' + c.value}}<div class="small muted">{{c.description}}</div></td><td>{{c.scope}} {{c.scopeId}}</td><td>₹{{c.minPurchase}}</td><td>{{c.usedCount}}/{{c.usageLimit || '∞'}}</td><td>{{c.expiresAt | date:'mediumDate'}}</td>
        <td><button class="btn btn-ghost btn-sm" (click)="editCoupon(c)">Edit</button> <button class="btn btn-danger btn-sm" (click)="delCoupon(c)">Delete</button></td></tr>}</table></div>
  }

  @if (tab==='reviews') {
    <h3>Reported reviews</h3>@for (r of items; track r.id) {<div class="card mb"><b>{{r.product.name}}</b> · {{r.rating}}★ by {{r.userName}}<p>{{r.title}} - {{r.comment}}</p><div class="err small">Reported: {{r.reportReason}}</div>
      <div class="row mt"><button class="btn btn-danger btn-sm" (click)="hide(r)">Hide review</button><button class="btn btn-ghost btn-sm" (click)="dismiss(r)">Dismiss report</button></div></div>}
    @if (!items.length) {<div class="card">No reported content. 🎉</div>}
  }

  @if (tab==='inventory') {
    <div class="card tbl"><table><tr><th>Product</th><th>Seller</th><th>Stock</th><th>Alert at</th></tr>@for (p of items; track p.id) {<tr><td>{{p.name}}</td><td>{{p.sellerName}}</td><td class="err"><b>{{p.stock}}</b></td><td>{{p.lowStockThreshold}}</td></tr>}</table></div>
    @if (!items.length) {<div class="card">All products are well stocked.</div>}
  }`
})
export class AdminComponent implements OnInit {
  tabs = [['dash', 'Dashboard'], ['customers', 'Customers'], ['sellers', 'Sellers'], ['categories', 'Categories'], ['products', 'Products'], ['orders', 'Orders'], ['coupons', 'Coupons & offers'], ['reviews', 'Reviews'], ['inventory', 'Inventory']];
  tab = 'dash'; st: any; users: any[] = []; items: any[] = []; cats: any[] = []; coupons: any[] = []; pg = 0; total = 0; uq = ''; ustatus = ''; cat: any = {}; cp: any = {};
  allStatuses = [...FLOW, 'CANCELLED', 'RETURNED']; keys = Object.keys;
  constructor(public api: Api, private toast: Toast) { this.newCoupon(); }
  ngOnInit() { this.api.get('/admin/stats').subscribe({ next: r => this.st = r, error: e => this.toast.error(e) }); }
  go(t: string) { this.tab = t; this.uq = ''; this.ustatus = t === 'sellers' ? 'PENDING' : ''; this.items = []; this.pg = 0;
    if (t === 'dash') this.ngOnInit(); if (t === 'customers' || t === 'sellers') this.loadUsers(0); if (t === 'categories') this.loadCats(); if (t === 'products') this.loadProducts(0); if (t === 'orders') this.loadOrders(0);
    if (t === 'coupons') { this.loadCoupons(); this.loadCats(); } if (t === 'reviews') this.api.get('/admin/reviews').subscribe(r => this.items = r.content); if (t === 'inventory') this.api.get('/admin/inventory/low-stock').subscribe(r => this.items = r); }
  private pageOf(r: any) { this.pg = r.page; this.total = r.totalPages; return r.content; }
  loadUsers(p: number) { this.api.get('/admin/users', { role: this.tab === 'sellers' ? 'SELLER' : 'CUSTOMER', status: this.ustatus, q: this.uq, page: p }).subscribe(r => this.users = this.pageOf(r)); }
  setUser(u: any, status: string) { this.api.put('/admin/users/' + u.id + '/status', { status }).subscribe({ next: (r: any) => { u.status = r.status; this.toast.ok('User ' + status.toLowerCase()); }, error: e => this.toast.error(e) }); }
  editCat(c: any) { this.cat = { ...c }; }
  loadCats() { this.api.get('/admin/categories').subscribe(c => this.cats = c); }
  saveCat() { (this.cat.id ? this.api.put('/admin/categories/' + this.cat.id, this.cat) : this.api.post('/admin/categories', this.cat)).subscribe({ next: () => { this.cat = {}; this.loadCats(); this.toast.ok('Saved'); }, error: e => this.toast.error(e) }); }
  delCat(c: any) { if (confirm('Delete ' + c.name + '?')) this.api.del('/admin/categories/' + c.id).subscribe({ next: () => this.loadCats(), error: e => this.toast.error(e) }); }
  loadProducts(p: number) { this.api.get('/admin/products', { q: this.uq, page: p }).subscribe(r => this.items = this.pageOf(r)); }
  flag(p: any, f: any) { this.api.put('/admin/products/' + p.id + '/flags', f).subscribe({ next: (r: any) => Object.assign(p, r), error: e => this.toast.error(e) }); }
  loadOrders(p: number) { this.api.get('/admin/orders', { status: this.ustatus, page: p }).subscribe(r => this.items = this.pageOf(r)); }
  next(o: any): string[] { const i = FLOW.indexOf(o.status); if (i < 0 || i === FLOW.length - 1) return []; const n = FLOW.slice(i + 1); return i <= 2 ? [...n, 'CANCELLED'] : n; }
  setOrder(o: any, status: string) { this.api.put('/admin/orders/' + o.id + '/status', { status }).subscribe({ next: (r: any) => { Object.assign(o, r); this.toast.ok('Order updated'); }, error: e => this.toast.error(e) }); }
  decide(o: any, approve: boolean) { this.api.post('/admin/orders/' + o.id + '/return', { approve }).subscribe({ next: (r: any) => { Object.assign(o, r); this.toast.ok('Done'); }, error: e => this.toast.error(e) }); }
  loadCoupons() { this.api.get('/admin/coupons').subscribe(c => this.coupons = c); }
  newCoupon() { this.cp = { code: '', description: '', type: 'PERCENT', value: 10, minPurchase: 0, maxDiscount: null, scope: 'ALL', scopeId: null, startsAt: '', expiresAt: '', usageLimit: 1000, active: true }; }
  editCoupon(c: any) { this.cp = { ...c, startsAt: c.startsAt?.slice(0, 16) || '', expiresAt: c.expiresAt?.slice(0, 16) || '' }; }
  saveCoupon() { const b = { ...this.cp, startsAt: this.cp.startsAt || null, expiresAt: this.cp.expiresAt || null, maxDiscount: this.cp.maxDiscount || null, scopeId: this.cp.scope === 'ALL' ? null : this.cp.scopeId };
    (b.id ? this.api.put('/admin/coupons/' + b.id, b) : this.api.post('/admin/coupons', b)).subscribe({ next: () => { this.newCoupon(); this.loadCoupons(); this.toast.ok('Coupon saved'); }, error: e => this.toast.error(e) }); }
  delCoupon(c: any) { if (confirm('Delete coupon ' + c.code + '?')) this.api.del('/admin/coupons/' + c.id).subscribe(() => this.loadCoupons()); }
  hide(r: any) { this.api.post('/admin/reviews/' + r.id + '/hide').subscribe(() => { this.items = this.items.filter(x => x.id !== r.id); this.toast.ok('Review hidden'); }); }
  dismiss(r: any) { this.api.post('/admin/reviews/' + r.id + '/dismiss').subscribe(() => this.items = this.items.filter(x => x.id !== r.id)); }
}
