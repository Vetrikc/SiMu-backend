import "react-router-dom";

export const ROUTES = {
  HOME: "/",
  LOGIN: "/login",
  REGISTER: "/register",
  SEARCH: "/search",
  LIBRARY: "/library",
  ACCOUNT: "/account/overview",
  ALBUM: "/album/:albumId",
  PLAYLIST: "/playlist/:playlistId"
} as const;

export type PathParams = {
  [ROUTES.ALBUM]: {
    albumId: string;
  };
  [ROUTES.PLAYLIST]: {
    playlistId: string;
  }
};

declare module "react-router-dom" {
  interface Register {
    params: PathParams;
  }
}
