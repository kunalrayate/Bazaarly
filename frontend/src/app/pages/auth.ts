import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Toast } from '../core/toast.service';

@Component({
  standalone: true, imports: [FormsModule, RouterLink],
  template: `
  <div class="card" style="max-width:480px;margin:20px auto">
    <h2>{{mode==='login' ? 'Welcome back' : mode==='register' ? 'Create your account' : 'Sell on Bazaarly'}}</h2>
    <form (ngSubmit)="submit()">
      @if (mode !== 'login') {<label class="f">Full name</label><input name="name" [(ngModel)]="f.name" required>}
      <label class="f">Email</label><input name="email" type="email" [(ngModel)]="f.email" required>
      @if (mode !== 'login') {<label class="f">Phone</label><input name="phone" [(ngModel)]="f.phone">}
      <label class="f">Password</label><input name="password" type="password" [(ngModel)]="f.password" required>
      @if (mode === 'seller') {<label class="f">Store name</label><input name="sn" [(ngModel)]="f.storeName" required>
        <label class="f">About your store</label><textarea name="sd" [(ngModel)]="f.storeDescription"></textarea>
        <label class="f">Business address</label><input name="ba" [(ngModel)]="f.businessAddress"><label class="f">GSTIN (optional)</label><input name="g" [(ngModel)]="f.gstin">}
      <button class="btn btn-primary btn-block" [disabled]="busy">{{mode==='login' ? 'Login' : mode==='register' ? 'Sign up' : 'Submit for approval'}}</button>
    </form>
    <p class="small mt">@if (mode==='login') {New here? <a routerLink="/register">Create an account</a> · <a routerLink="/sell">Become a seller</a>} @else {Already registered? <a routerLink="/login">Login</a>}</p>
    @if (mode==='login') {<div class="small muted card" style="background:#faf6ee"><b>Demo logins</b><br>
      Customer: customer&#64;bazaarly.com / Customer&#64;123<br>Seller: seller&#64;bazaarly.com / Seller&#64;123<br>Admin: admin&#64;bazaarly.com / Admin&#64;123</div>}
  </div>`
})
export class AuthComponent implements OnInit {
  mode = 'login'; busy = false; f: any = {};
  constructor(private route: ActivatedRoute, private router: Router, private api: Api, private auth: AuthService, private toast: Toast) {}
  ngOnInit() { this.mode = this.route.snapshot.data['mode'] || 'login'; }
  submit() {
    this.busy = true; const done = () => this.busy = false;
    if (this.mode === 'seller') { this.api.post('/auth/register-seller', this.f).subscribe({ next: r => { done(); this.toast.ok(r.message); this.router.navigate(['/login']); }, error: e => { done(); this.toast.error(e); } }); return; }
    const obs = this.mode === 'login' ? this.auth.login(this.f.email, this.f.password) : this.auth.registerCustomer(this.f);
    obs.subscribe({ next: (r: any) => { done(); const role = r.user.role; const back = this.route.snapshot.queryParamMap.get('returnUrl');
      this.router.navigateByUrl(back || (role === 'ADMIN' ? '/admin' : role === 'SELLER' ? '/seller' : '/')); }, error: e => { done(); this.toast.error(e); } });
  }
}
