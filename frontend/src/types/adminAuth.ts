export type AdminRole = "SUPER_ADMIN" | "EDITOR";

export type AdminLoginRequest = {
  email: string;
  password: string;
};

export type AdminLoginResponse = {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  email: string;
  role: AdminRole;
};
