import { jwtDecode } from "jwt-decode";
import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { ROLE } from "@/shared/model/user.ts";

type Session = {
  userId: string;
  username: string;
  roles: ROLE[];
  exp: number;
  iat: number;
};

interface AuthStore {
  token: string | null;
  session: Session | null;
  updateToken: (token: string) => void;
  removeToken: () => void;
  getFreshToken: () => Promise<string | null>;
}

export const useSession = create<AuthStore>()(
  persist(
    (set, get) => ({
      token: null,
      session: null,

      updateToken: (token: string) => {
        const session = jwtDecode<Session>(token);
        set({ token, session });
      },

      removeToken: () => {
        set({ token: null, session: null });
      },

      getFreshToken: async () => {
        try {
          const { token } = get();

          if (!token) return null;

          const session = jwtDecode<Session>(token);

          if (session.exp < Date.now() / 1000 + 1) {
            get().removeToken();
            return null;
          }

          return token;
        } catch (error) {
          console.error("Error in getFreshToken:", error);
          get().removeToken();
          return null;
        }
      },
    }),
    {
      name: "auth-storage",
    },
  ),
);
