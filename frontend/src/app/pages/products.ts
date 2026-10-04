import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api.service';
import { Toast } from '../core/toast.service';
import { PagerComponent, ProductCardComponent } from '../shared/ui';

@Component({
  standalone: true, imports: [FormsModule, ProductCardComponent, PagerComponent],
  template: `
  <div class="listing">
    <aside class="filters card"><h3>Filters</h3>
      <h4>Category</h4><label><input type="radio" name="c" [checked]="!f.category" (change)="set('category', null)">All</label>
      @for (c of cats; track c.id) {<label><input type="radio" name="c" [checked]="f.category==c.id" (change)="set('category', c.id)">{{c.name}}</label>}
      <h4>Brand</h4>@for (b of brands; track b) {<label><input type="checkbox" [checked]="f.brand.includes(b)" (change)="toggleBrand(b)">{{b}}</label>}
      <h4>Price (₹)</h4><div class="row"><input type="number" placeholder="Min" [(ngModel)]="f.minPrice" style="width:90px"><input type="number" placeholder="Max" [(ngModel)]="f.maxPrice" style="width:90px"></div>
      <button class="btn btn-ghost btn-sm mt" (click)="load(0)">Apply price</button>
      <h4>Customer rating</h4>@for (r of [4,3,2]; track r) {<label><input type="radio" name="r" [checked]="f.minRating==r" (change)="set('minRating', r)">{{r}}★ & up</label>}
      <label><input type="radio" name="r" [checked]="!f.minRating" (change)="set('minRating', null)">Any</label>
      <h4>Availability</h4><label><input type="checkbox" [checked]="f.inStock" (change)="set('inStock', !f.inStock)">In stock only</label>
      <button class="btn btn-ghost btn-block" (click)="reset()">Clear all</button></aside>
    <section>
      <div class="row between mb"><div><h2 style="margin:0">{{title}}</h2><span class="muted small">{{total}} results</span></div>
        <select [(ngModel)]="f.sort" (ngModelChange)="load(0)" style="width:auto"><option value="">Relevance</option><option value="popularity">Popularity</option><option value="rating">Avg. rating</option>
          <option value="price_asc">Price: low to high</option><option value="price_desc">Price: high to low</option><option value="newest">Newest arrivals</option></select></div>
      @if (items.length) {<div class="grid g4">@for (p of items; track p.id) {<app-product-card [p]="p"/>}</div>}
      @else if (!loading) {<div class="card">No products match your filters. Try clearing some filters or a different keyword.</div>}
      <app-pager [page]="page" [totalPages]="pages" (change)="load($event)"/>
    </section>
  </div>`
})
export class ProductsComponent implements OnInit {
  items: any[] = []; cats: any[] = []; brands: string[] = []; page = 0; pages = 0; total = 0; loading = true;
  f: any = { q: '', category: null, brand: [], minPrice: null, maxPrice: null, minRating: null, inStock: false, sort: '' };
  constructor(private api: Api, private route: ActivatedRoute, private router: Router, private toast: Toast) {}
  get title() { return this.f.q ? `Results for "${this.f.q}"` : (this.cats.find(c => c.id == this.f.category)?.name || 'All products'); }
  ngOnInit() {
    this.api.get('/categories').subscribe(c => this.cats = c);
    this.route.queryParamMap.subscribe(p => {
      this.f.q = p.get('q') || ''; this.f.category = p.get('category') ? +p.get('category')! : null; this.f.sort = p.get('sort') || '';
      this.api.get('/products/brands', { category: this.f.category }).subscribe(b => this.brands = b); this.f.brand = [];
      this.load(0);
    });
  }
  set(k: string, v: any) { this.f[k] = v; if (k === 'category') { this.router.navigate([], { queryParams: { category: v, q: this.f.q || null, sort: this.f.sort || null } }); } else this.load(0); }
  toggleBrand(b: string) { this.f.brand = this.f.brand.includes(b) ? this.f.brand.filter((x: string) => x !== b) : [...this.f.brand, b]; this.load(0); }
  reset() { this.router.navigate([], { queryParams: {} }); this.f = { q: '', category: null, brand: [], minPrice: null, maxPrice: null, minRating: null, inStock: false, sort: '' }; this.load(0); }
  load(page: number) {
    this.loading = true;
    this.api.get('/products', { ...this.f, inStock: this.f.inStock || null, page, size: 12 }).subscribe({
      next: r => { this.items = r.content; this.page = r.page; this.pages = r.totalPages; this.total = r.totalElements; this.loading = false; window.scrollTo({ top: 0 }); },
      error: e => { this.loading = false; this.toast.error(e); } });
  }
}
