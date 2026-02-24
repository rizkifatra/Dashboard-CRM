export interface User {
  email: string;
  name: string;
  givenName?: string;
  surname?: string;
}

export interface AuthResponse {
  email: string;
  name: string;
  givenName?: string;
  surname?: string;
  token: string;
}
