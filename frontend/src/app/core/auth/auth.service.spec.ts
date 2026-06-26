import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { TokenStorage } from './token.storage';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  let tokens: TokenStorage;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthService, TokenStorage, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    tokens = TestBed.inject(TokenStorage);
  });

  afterEach(() => {
    http.verify();
    tokens.clear();
  });

  it('login stores token and returns response', () => {
    const mockResp = { token: 'abc123', type: 'Bearer', email: 'u@test.com', roles: ['BUYER'] };
    service.login({ email: 'u@test.com', password: 'pass' }).subscribe((res) => {
      expect(res.token).toBe('abc123');
      expect(tokens.get()).toBe('abc123');
    });
    http.expectOne('http://localhost:8080/api/auth/login').flush(mockResp);
  });

  it('logout clears token and user signal', () => {
    tokens.set('existing-token');
    service.logout();
    expect(tokens.get()).toBeNull();
    expect(service.user()).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
  });

  it('isLoggedIn returns true when token is present', () => {
    tokens.set('some-token');
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('me() sets user signal', () => {
    const mockMe = { id: 1, email: 'u@test.com', name: 'User', roles: ['BUYER'] };
    service.me().subscribe((me) => {
      expect(service.user()?.email).toBe('u@test.com');
    });
    http.expectOne('http://localhost:8080/api/me').flush(mockMe);
  });
});
