import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { jwtInterceptor } from './jwt.interceptor';
import { TokenStorage } from './token.storage';

describe('jwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let tokens: TokenStorage;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TokenStorage,
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    tokens = TestBed.inject(TokenStorage);
  });

  afterEach(() => {
    httpMock.verify();
    tokens.clear();
  });

  it('attaches Authorization header when token is present', () => {
    tokens.set('my-jwt-token');
    http.get('/api/products').subscribe();
    const req = httpMock.expectOne('/api/products');
    expect(req.request.headers.get('Authorization')).toBe('Bearer my-jwt-token');
    req.flush([]);
  });

  it('does not attach Authorization header when no token', () => {
    http.get('/api/products').subscribe();
    const req = httpMock.expectOne('/api/products');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush([]);
  });
});
