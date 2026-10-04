import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

const PLACEHOLDER = 'data:image/svg+xml;utf8,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400"><rect width="100%" height="100%" fill="#efe9dc"/><text x="50%" y="50%" fill="#a39a85" font-family="sans-serif" font-size="22" text-anchor="middle">No image</text></svg>');

@Injectable({ providedIn: 'root' })
export class Api {
  base = environment.apiUrl + '/api';
  constructor(private http: HttpClient) {}

  private clean(p?: any) { const o: any = {}; Object.keys(p || {}).forEach(k => { const v = p[k]; if (v !== null && v !== undefined && v !== '' && !(Array.isArray(v) && !v.length)) o[k] = v; }); return o; }
  get<T = any>(path: string, params?: any) { return this.http.get<T>(this.base + path, { params: this.clean(params) }); }
  post<T = any>(path: string, body: any = {}) { return this.http.post<T>(this.base + path, body); }
  put<T = any>(path: string, body: any = {}) { return this.http.put<T>(this.base + path, body); }
  del<T = any>(path: string) { return this.http.delete<T>(this.base + path); }
  upload(file: File) { const f = new FormData(); f.append('file', file); return this.http.post<{ url: string }>(this.base + '/upload', f); }
  img(u?: string) { return !u ? PLACEHOLDER : u.startsWith('/') ? environment.apiUrl + u : u; }
}
