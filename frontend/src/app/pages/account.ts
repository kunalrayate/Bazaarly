import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Toast } from '../core/toast.service';

@Component({
  standalone: true, imports: [FormsModule],
  template: `
  <h2>My account</h2>
  <div class="tabs"><button [class.on]="tab==='p'" (click)="tab='p'">Profile</button>@if (auth.role==='CUSTOMER') {<button [class.on]="tab==='a'" (click)="tab='a'">Addresses</button>}<button [class.on]="tab==='s'" (click)="tab='s'">Password</button></div>
  @if (tab==='p') {<div class="card" style="max-width:560px"><label class="f">Name</label><input [(ngModel)]="u.name"><label class="f">Email</label><input [value]="u.email" disabled><label class="f">Phone</label><input [(ngModel)]="u.phone">
    @if (auth.role==='SELLER') {<label class="f">Store name</label><input [(ngModel)]="u.storeName"><label class="f">Store description</label><textarea [(ngModel)]="u.storeDescription"></textarea><label class="f">Business address</label><input [(ngModel)]="u.businessAddress">}
    <button class="btn btn-primary mt" (click)="saveProfile()">Save changes</button></div>}
  @if (tab==='a') {<div class="grid g2">@for (a of addrs; track a.id) {<div class="card"><b>{{a.fullName}}</b> @if (a.defaultAddress) {<span class="badge">Default</span>}<div class="small muted">{{a.phone}}<br>{{a.line1}}, {{a.line2}}<br>{{a.city}}, {{a.state}} - {{a.pincode}}</div>
      <div class="row mt"><button class="btn btn-ghost btn-sm" (click)="edit(a)">Edit</button>@if (!a.defaultAddress) {<button class="btn btn-ghost btn-sm" (click)="makeDefault(a)">Make default</button>}<button class="btn btn-danger btn-sm" (click)="remove(a)">Delete</button></div></div>}</div>
    <div class="card mt" style="max-width:640px"><h3>{{na.id ? 'Edit address' : 'Add new address'}}</h3><div class="grid g2"><div><label class="f">Full name</label><input [(ngModel)]="na.fullName"></div><div><label class="f">Phone</label><input [(ngModel)]="na.phone"></div>
      <div><label class="f">Line 1</label><input [(ngModel)]="na.line1"></div><div><label class="f">Line 2</label><input [(ngModel)]="na.line2"></div><div><label class="f">City</label><input [(ngModel)]="na.city"></div>
      <div><label class="f">State</label><input [(ngModel)]="na.state"></div><div><label class="f">Pincode</label><input [(ngModel)]="na.pincode"></div></div>
      <label class="f"><input type="checkbox" [(ngModel)]="na.defaultAddress">Set as default</label><button class="btn btn-primary mt" (click)="saveAddr()">Save address</button> @if (na.id) {<button class="btn btn-ghost mt" (click)="na={}">Cancel</button>}</div>}
  @if (tab==='s') {<div class="card" style="max-width:420px"><label class="f">Current password</label><input type="password" [(ngModel)]="pw.currentPassword"><label class="f">New password</label><input type="password" [(ngModel)]="pw.newPassword"><button class="btn btn-primary mt" (click)="changePw()">Update password</button></div>}`
})
export class AccountComponent implements OnInit {
  tab = 'p'; u: any = {}; addrs: any[] = []; na: any = {}; pw: any = {};
  constructor(private api: Api, public auth: AuthService, private toast: Toast) {}
  ngOnInit() { this.u = { ...this.auth.user() }; this.loadAddrs(); }
  loadAddrs() { if (this.auth.role === 'CUSTOMER') this.api.get('/addresses').subscribe(a => this.addrs = a); }
  saveProfile() { this.api.put('/me', this.u).subscribe({ next: (u: any) => { this.auth.updateUser(u); this.toast.ok('Profile updated'); }, error: e => this.toast.error(e) }); }
  saveAddr() { const r = this.na.id ? this.api.put('/addresses/' + this.na.id, this.na) : this.api.post('/addresses', this.na); r.subscribe({ next: () => { this.na = {}; this.loadAddrs(); this.toast.ok('Address saved'); }, error: e => this.toast.error(e) }); }
  edit(a: any) { this.na = { ...a }; }
  makeDefault(a: any) { this.api.put('/addresses/' + a.id, { ...a, defaultAddress: true }).subscribe(() => this.loadAddrs()); }
  remove(a: any) { this.api.del('/addresses/' + a.id).subscribe(() => this.loadAddrs()); }
  changePw() { this.api.put('/me/password', this.pw).subscribe({ next: () => { this.pw = {}; this.toast.ok('Password updated'); }, error: e => this.toast.error(e) }); }
}

@Component({
  standalone: true, imports: [DatePipe],
  template: `<div class="row between"><h2>Notifications</h2><button class="btn btn-ghost btn-sm" (click)="readAll()">Mark all as read</button></div>
    @for (n of items; track n.id) {<div class="card mb" [style.borderLeft]="n.read ? '' : '4px solid var(--saffron)'"><b>{{n.title}}</b><div>{{n.message}}</div><div class="small muted">{{n.createdAt | date:'medium'}}</div></div>}
    @if (!items.length) {<div class="card">You're all caught up.</div>}`
})
export class NotificationsComponent implements OnInit {
  items: any[] = [];
  constructor(private api: Api, private auth: AuthService) {}
  ngOnInit() { this.api.get('/notifications').subscribe(r => this.items = r); }
  readAll() { this.api.post('/notifications/read-all').subscribe(() => { this.items.forEach(n => n.read = true); this.auth.unread.set(0); }); }
}
