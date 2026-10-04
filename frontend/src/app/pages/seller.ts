import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api.service';
import { Toast } from '../core/toast.service';
import { BarsComponent, PagerComponent } from '../shared/ui';

const FLOW = ['PLACED', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED'];

@Component({
  standalone: true, imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink, BarsComponent, PagerComponent],
  template: `
  <h2>Seller Hub</h2>
  <div class="tabs"><button [class.on]="tab==='d'" (click)="tab='d'">Dashboard</button><button [class.on]="tab==='p'" (click)="tab='p'; loadProducts(0)">Products</button>
    <button [class.on]="tab==='o'" (click)="tab='o'; loadOrders(0)">Orders</button><a routerLink="/account" style="padding:10px 16px">Store profile</a></div>

  @if (tab==='d' && s) {
    <div class="grid g-stat"><div class="card stat"><small>Revenue</small><b>{{s.revenue | currency:'INR':'symbol':'1.0-0'}}</b></div><div class="card stat"><small>Orders</small><b>{{s.totalOrders}}</b></div>
      <div class="card stat"><small>Units sold</small><b>{{s.unitsSold}}</b></div><div class="card stat"><small>Products</small><b>{{s.products}}</b></div></div>
    <div class="grid g2 mt"><div class="card"><h3>Sales trend (14 days)</h3><app-bars [data]="s.salesTrend" unit="units"/></div>
      <div class="card"><h3>Orders by status</h3>@for (k of keys(s.statusCounts); track k) {<div class="row between small"><span>{{k.replaceAll('_',' ')}}</span><b>{{s.statusCounts[k]}}</b></div>}</div>
      <div class="card"><h3>Best-selling products</h3>@for (p of s.bestSellers; track p.id) {<div class="row between small"><span>{{p.name}}</span><b>{{p.sold}} sold</b></div>}</div>
      <div class="card"><h3>⚠ Low stock</h3>@for (p of s.lowStock; track p.id) {<div class="row between small"><span>{{p.name}}</span><b class="err">{{p.stock}} left</b></div>}@if (!s.lowStock.length) {<span class="muted">All good</span>}</div></div>
  }

  @if (tab==='p') {
    @if (!form) {<div class="row between mb"><input placeholder="Search your products" [(ngModel)]="q" (keyup.enter)="loadProducts(0)" style="max-width:300px"><button class="btn btn-primary" (click)="newProduct()">+ Add product</button></div>
      <div class="card tbl"><table><tr><th></th><th>Product</th><th>Price</th><th>Disc.</th><th>Stock</th><th>Sold</th><th>Status</th><th></th></tr>
        @for (p of products; track p.id) {<tr><td><img [src]="api.img(p.images[0])" alt=""></td><td><b>{{p.name}}</b><div class="small muted">{{p.category?.name}} · {{p.brand}}</div></td><td>{{p.sellingPrice | currency:'INR':'symbol':'1.0-0'}}</td><td>{{p.discountPercent}}%</td>
          <td><input type="number" [value]="p.stock" #st style="width:80px" (change)="setStock(p, +st.value)"> @if (p.lowStock) {<span class="err small">low</span>}</td><td>{{p.soldCount}}</td><td><span class="badge" [class]="p.active ? 'ACTIVE' : 'BLOCKED'">{{p.active ? 'Listed' : 'Unlisted'}}</span></td>
          <td><button class="btn btn-ghost btn-sm" (click)="edit(p)">Edit</button> @if (p.active) {<button class="btn btn-danger btn-sm" (click)="unlist(p)">Unlist</button>}</td></tr>}</table></div>
      <app-pager [page]="pp" [totalPages]="ptotal" (change)="loadProducts($event)"/>}
    @else {<div class="card"><h3>{{form.id ? 'Edit product' : 'New product'}}</h3>
      <div class="grid g2"><div><label class="f">Name</label><input [(ngModel)]="form.name"></div><div><label class="f">Brand</label><input [(ngModel)]="form.brand"></div>
        <div><label class="f">Category</label><select [(ngModel)]="form.categoryId">@for (c of cats; track c.id) {<option [ngValue]="c.id">{{c.name}}</option>}</select></div>
        <div><label class="f">MRP (₹)</label><input type="number" [(ngModel)]="form.price"></div><div><label class="f">Discount %</label><input type="number" [(ngModel)]="form.discountPercent"></div>
        <div><label class="f">Discount ends (optional - limited-time deal)</label><input type="datetime-local" [(ngModel)]="form.discountEndsAt"></div>
        <div><label class="f">Stock</label><input type="number" [(ngModel)]="form.stock"></div><div><label class="f">Low-stock alert at</label><input type="number" [(ngModel)]="form.lowStockThreshold"></div></div>
      <label class="f">Description</label><textarea [(ngModel)]="form.description"></textarea>
      <label class="f">Images (upload or paste URL)</label><div class="row">@for (im of form.images; track $index) {<div style="position:relative"><img [src]="api.img(im)" style="width:70px;height:70px;object-fit:cover;border-radius:8px" alt=""><button class="btn btn-danger btn-sm" style="position:absolute;top:-6px;right:-6px;padding:0 6px" (click)="form.images.splice($index,1)">×</button></div>}</div>
      <div class="row mt"><input type="file" accept="image/*" (change)="upload($event)" style="max-width:260px"><input placeholder="https://..." #u style="max-width:300px"><button class="btn btn-ghost btn-sm" (click)="form.images.push(u.value); u.value=''">Add URL</button></div>
      <label class="f">Specifications</label>@for (r of form.specs; track $index) {<div class="row mb"><input placeholder="Name (e.g. RAM)" [(ngModel)]="r.k" style="max-width:220px"><input placeholder="Value" [(ngModel)]="r.v" style="max-width:260px"><button class="btn btn-danger btn-sm" (click)="form.specs.splice($index,1)">×</button></div>}
      <button class="btn btn-ghost btn-sm" (click)="form.specs.push({k:'',v:''})">+ Spec</button>
      <label class="f">Variants (e.g. Color / Black, Storage / 256 GB with price add-on)</label>@for (v of form.variants; track $index) {<div class="row mb"><input placeholder="Type" [(ngModel)]="v.type" style="max-width:160px"><input placeholder="Value" [(ngModel)]="v.value" style="max-width:200px"><input type="number" placeholder="+ price" [(ngModel)]="v.priceAdjustment" style="max-width:120px"><button class="btn btn-danger btn-sm" (click)="form.variants.splice($index,1)">×</button></div>}
      <button class="btn btn-ghost btn-sm" (click)="form.variants.push({type:'',value:'',priceAdjustment:0})">+ Variant</button>
      <div class="row mt"><button class="btn btn-primary" (click)="save()">Save product</button><button class="btn btn-ghost" (click)="form=null">Cancel</button></div></div>}
  }

  @if (tab==='o') {
    <div class="row mb"><select [(ngModel)]="ostatus" (ngModelChange)="loadOrders(0)" style="width:auto"><option value="">All statuses</option>@for (st of allStatuses; track st) {<option [value]="st">{{st.replaceAll('_',' ')}}</option>}</select></div>
    @for (o of orders; track o.id) {<div class="card mb"><div class="row between"><div><b>{{o.orderNumber}}</b> <span class="small muted">{{o.createdAt | date:'medium'}} · {{o.user.name}}</span></div>
        <div class="row"><span class="badge" [class]="o.status">{{o.status.replaceAll('_',' ')}}</span><span class="badge" [class]="o.paymentStatus">{{o.paymentMethod}} · {{o.paymentStatus}}</span>@if (o.returnStatus!=='NONE') {<span class="badge" [class]="o.returnStatus">Return {{o.returnStatus}}</span>}</div></div>
      @for (i of o.items; track i.id) {@if (i.product.sellerId === me) {<div class="small">{{i.quantity}} × {{i.productName}} {{i.variantLabel}} — {{i.lineTotal | currency:'INR':'symbol':'1.0-0'}}</div>}}
      <div class="small muted mt">Ship to: {{o.shippingAddress}}</div>
      <div class="row mt">@if (next(o).length) {<select #ns style="width:auto">@for (n of next(o); track n) {<option [value]="n">{{n.replaceAll('_',' ')}}</option>}</select><button class="btn btn-primary btn-sm" (click)="setStatus(o, ns.value)">Update status</button>}
        @if (o.returnStatus==='REQUESTED') {<span class="small">Reason: {{o.returnReason}}</span><button class="btn btn-primary btn-sm" (click)="decide(o, true)">Approve & refund</button><button class="btn btn-danger btn-sm" (click)="decide(o, false)">Reject</button>}</div></div>}
    @if (!orders.length) {<div class="card">No orders found.</div>}<app-pager [page]="op" [totalPages]="ototal" (change)="loadOrders($event)"/>
  }`
})
export class SellerComponent implements OnInit {
  tab = 'd'; s: any; products: any[] = []; pp = 0; ptotal = 0; q = ''; cats: any[] = []; form: any = null; orders: any[] = []; op = 0; ototal = 0; ostatus = '';
  me = JSON.parse(localStorage.getItem('user') || '{}').id; allStatuses = [...FLOW, 'CANCELLED', 'RETURNED'];
  constructor(public api: Api, private toast: Toast) {}
  keys = Object.keys;
  ngOnInit() { this.api.get('/seller/stats').subscribe({ next: r => this.s = r, error: e => this.toast.error(e) }); this.api.get('/categories').subscribe(c => this.cats = c); }
  loadProducts(p: number) { this.form = null; this.api.get('/seller/products', { q: this.q, page: p }).subscribe(r => { this.products = r.content; this.pp = r.page; this.ptotal = r.totalPages; }); }
  newProduct() { this.form = { name: '', brand: '', categoryId: this.cats[0]?.id, price: 0, discountPercent: 0, discountEndsAt: '', stock: 0, lowStockThreshold: 10, description: '', images: [], specs: [], variants: [] }; }
  edit(p: any) { this.form = { ...p, categoryId: p.category?.id, discountEndsAt: p.discountEndsAt ? p.discountEndsAt.slice(0, 16) : '', images: [...p.images], specs: Object.entries(p.specifications || {}).map(([k, v]) => ({ k, v })), variants: p.variants.map((v: any) => ({ type: v.type, value: v.value, priceAdjustment: v.priceAdjustment })) }; }
  upload(ev: any) { const f = ev.target.files[0]; if (f) this.api.upload(f).subscribe({ next: r => this.form.images.push(r.url), error: e => this.toast.error(e) }); }
  save() {
    const f = this.form, specifications: any = {}; f.specs.forEach((r: any) => { if (r.k) specifications[r.k] = r.v; });
    const body = { name: f.name, brand: f.brand, categoryId: f.categoryId, price: f.price, discountPercent: f.discountPercent, discountEndsAt: f.discountEndsAt || null, stock: f.stock, lowStockThreshold: f.lowStockThreshold, description: f.description, images: f.images, specifications, variants: f.variants, active: true };
    (f.id ? this.api.put('/seller/products/' + f.id, body) : this.api.post('/seller/products', body)).subscribe({ next: () => { this.toast.ok('Product saved'); this.loadProducts(0); }, error: e => this.toast.error(e) });
  }
  setStock(p: any, stock: number) { this.api.put('/seller/products/' + p.id + '/stock', { stock }).subscribe({ next: (r: any) => { p.stock = r.stock; p.lowStock = r.lowStock; this.toast.ok('Stock updated'); }, error: e => this.toast.error(e) }); }
  unlist(p: any) { if (confirm('Unlist this product?')) this.api.del('/seller/products/' + p.id).subscribe(() => this.loadProducts(this.pp)); }
  loadOrders(p: number) { this.api.get('/seller/orders', { status: this.ostatus, page: p }).subscribe(r => { this.orders = r.content; this.op = r.page; this.ototal = r.totalPages; }); }
  next(o: any): string[] { const i = FLOW.indexOf(o.status); if (i < 0 || i === FLOW.length - 1) return []; const n = FLOW.slice(i + 1); return i <= 2 ? [...n, 'CANCELLED'] : n; }
  setStatus(o: any, status: string) { this.api.put('/seller/orders/' + o.id + '/status', { status }).subscribe({ next: (r: any) => { Object.assign(o, r); this.toast.ok('Order updated'); }, error: e => this.toast.error(e) }); }
  decide(o: any, approve: boolean) { this.api.post('/seller/orders/' + o.id + '/return', { approve }).subscribe({ next: (r: any) => { Object.assign(o, r); this.toast.ok('Return ' + (approve ? 'approved' : 'rejected')); }, error: e => this.toast.error(e) }); }
}
