import { ROUTES } from "@/shared/model/routes.tsx";
import { createBrowserRouter } from "react-router-dom";
import { App } from "../app.tsx";
import { Providers } from "@/app/providers";
import {
  protectedLoader,
  ProtectedRoute,
} from "@/app/router/protected-route.tsx";
import { MainLayout } from "@/app/layout.tsx";

export const router = createBrowserRouter([
  {
    element: (
      <Providers>
        <App />
      </Providers>
    ),
    children: [
      {
        loader: protectedLoader,
        element: (
          <MainLayout>
            <ProtectedRoute />
          </MainLayout>
        ),
        children: [
          {
            path: ROUTES.HOME,
            lazy: () => import("@/features/home/home.page.tsx"),
          },
          {
            path: ROUTES.LIBRARY,
            lazy: () => import("@/features/library/library.page.tsx"),
          },
          {
            path: ROUTES.SEARCH,
            lazy: () => import("@/features/search/search.page.tsx"),
          },
          {
            path: ROUTES.PROFILE,
            lazy: () => import("@/features/profile/profile.page.tsx"),
          },
          {
            path: ROUTES.ALBUM,
            lazy: () => import("@/features/album/album.page.tsx"),
          },
          {
            path: ROUTES.PLAYLIST,
            lazy: () => import("@/features/playlist/playlist.page.tsx"),
          },
          {
            path: ROUTES.ARTISTS,
            lazy: () => import("@/features/artists/artists.page.tsx"),
          },
          {
            path: ROUTES.SONGS,
            lazy: () => import("@/features/songs/songs.page.tsx"),
          },
          {
            path: ROUTES.FRIENDS,
            lazy: () => import("@/features/friends/friends.page.tsx"),
          },
          {
            path: ROUTES.SETTINGS,
            lazy: () => import("@/features/settings/settings.page.tsx"),
          },
        ],
      },
      {
        path: ROUTES.LOGIN,
        lazy: () => import("@/features/auth/Login.page.tsx"),
      },
      {
        path: ROUTES.REGISTER,
        lazy: () => import("@/features/auth/Register.page.tsx"),
      },
    ],
  },
]);
