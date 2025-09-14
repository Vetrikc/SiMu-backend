const ROLES = {
  USER: "USER",
  ADMIN: "ADMIN",
} as const;

export type ROLE = typeof ROLES[keyof typeof ROLES];

export type User = {
  userId: string;
  username: string;
  email: string;
  password: string;
  roles: ROLE[];
}