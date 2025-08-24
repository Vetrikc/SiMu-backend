import {ROUTES} from "../shared/model/routes";
import {createBrowserRouter} from "react-router-dom";
import {App} from "./app";
import {Providers} from "@/app/providers.tsx";

export const router = createBrowserRouter([
  {
    element: <Providers><App /></Providers>,
    children: [
      {
        path: ROUTES.HOME,
        lazy: () => import("@/features/home/home.page"),
      },
      {
        path: ROUTES.LIBRARY,
        lazy: () => import("@/features/library/library.page"),
      },
      {
        path: ROUTES.SEARCH,
        lazy: () => import("@/features/search/search.page"),
      },
      {
        path: ROUTES.LOGIN,
        lazy: () => import("@/features/auth/Login.page.tsx"),
      },
      {
        path: ROUTES.REGISTER,
        lazy: () => import("@/features/auth/Register.page.tsx"),
      },
      {
        path: ROUTES.ACCOUNT,
        lazy: () => import("@/features/account/account.page")
      }, {
        path: ROUTES.ALBUM,
        lazy: () => import("@/features/album/album.page")
      }, {
        path: ROUTES.PLAYLIST,
        lazy: () => import("@/features/playlist/playlist.page")
      },
    ],
  },
]);
