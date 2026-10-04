import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class Toast {
  items = signal<{ id: number; msg: string; type: string }[]>([]);
  private n = 0;
  show(msg: string, type = 'ok') { const id = ++this.n; this.items.update(l => [...l, { id, msg, type }]); setTimeout(() => this.items.update(l => l.filter(t => t.id !== id)), 3500); }
  ok(msg: string) { this.show(msg, 'ok'); }
  error(e: any) { this.show(typeof e === 'string' ? e : e?.error?.message || (e?.status === 0 ? 'Cannot reach the server. Is the backend running on port 8080?' : 'Something went wrong'), 'err'); }
}
