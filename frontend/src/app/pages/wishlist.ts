import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Toast } from '../core/toast.service';
import { ProductCardComponent } from '../shared/ui';

@Component({
  standalone: true, imports: [ProductCardComponent, RouterLink],
  template: `<h2>My wishlist</h2>@if (items.length) {<div class="grid g4">@for (p of shown; track p.id) {<app-product-card [p]="p"/>}</div>}
    @else {<div class="card">Your wishlist is empty. <a routerLink="/products">Discover products</a></div>}`
})
export class WishlistComponent implements OnInit {
  items: any[] = [];
  constructor(private api: Api, private auth: AuthService, private toast: Toast) {}
  get shown() { return this.items.filter(p => this.auth.isWished(p.id) || true).filter(p => this.auth.wishIds().includes(p.id)); }
  ngOnInit() { this.api.get('/wishlist').subscribe({ next: r => this.items = r, error: e => this.toast.error(e) }); }
}
