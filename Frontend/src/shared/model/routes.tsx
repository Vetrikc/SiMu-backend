import "react-router-dom";

export const ROUTES = {
  HOME: "/",
  LOGIN: "/login",
  REGISTER: "/register",
  SEARCH: "/search",
  LIBRARY: "/library",
  SONGS: "/songs",
  FRIENDS: "/friends",
  FRIEND: "/friend/:friendId",
  PROFILE: "/account/overview",
  ALBUM: "/album/:albumId",
  PLAYLIST: "/playlist/:playlistId",
  ARTISTS: "/artists",
  ARTIST: "/artist/:artistId",
  SETTINGS: "/settings",
} as const;

export type PathParams = {
  [ROUTES.ALBUM]: {
    albumId: string;
  };
  [ROUTES.PLAYLIST]: {
    playlistId: string;
  };
  [ROUTES.FRIEND]: {
    friendId: string;
  };
  [ROUTES.ARTIST]: {
    artistId: string;
  };
};

declare module "react-router-dom" {
  interface Register {
    params: PathParams;
  }
}
