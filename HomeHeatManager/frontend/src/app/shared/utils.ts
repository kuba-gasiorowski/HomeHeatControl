import {Time} from "../models/api/heat-status";
import {AuthData} from "../auth/auth-data";

export class Utils {

  public static convert(body: any): any {
    if (body == null || body == undefined) {
      return body;
    }
    if (typeof body === 'object') {
      for (const key of Object.keys(body)) {
        const value = body[key];
        if (this.isTimestampType(key)) {
          body[key] = new Date(parseInt(value, 10));
        } else if (this.isTimeType(key)) {
          body[key] = new Time(value);
        } else if (typeof value === 'object') {
          this.convert(value);
        }
      }
    }
    return body;
  }

  static isTimestampType(key: string): boolean {
    switch (key) {
      case 'lastStatusChangeTime':
      case 'lastMessageTime':
        return true;
      default:
        return false;
    }
  }

  static isTimeType(key: string): boolean {
    switch (key) {
      case 'nightStartTime':
      case 'nightEndTime':
      case 'dayStartTime':
      case 'dayEndTime':
        return true;
      default:
        return false;
    }
  }

  static storeAuthData(authData: AuthData): void {
    window.sessionStorage.setItem('access_token', authData.token);
    window.sessionStorage.setItem('refresh_token', authData.refreshToken);
    window.sessionStorage.setItem(
      'refresh_token_expiry',
      authData.expiryDate.toString(10)
    );
  }

  static removeAuthData(): void {
    window.sessionStorage.removeItem('access_token');
    window.sessionStorage.removeItem('refresh_token');
    window.sessionStorage.removeItem('refresh_token_expiry');
  }

  static getRefreshToken(): string | null {
    return window.sessionStorage.getItem('refresh_token');
  }

  static getAuthHeader() {
    const token = window.sessionStorage.getItem('access_token');
    if (token != null) {
      return {
        'Authorization': 'Bearer ' + token,
      };
    }
    return null;
  }
}
