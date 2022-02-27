export class AuthData {
  username: string = '';
  token: string = '';
  refreshToken: string = '';
  expiryDate: number = -1;

  isRefreshTokenExpired(): boolean {
    if (this.expiryDate < Date.now()) return true;
    else return false;
  }
}
