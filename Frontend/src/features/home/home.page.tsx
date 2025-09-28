import { rqClient } from "@/shared/api/instance";
import { href, Link } from "react-router-dom";
import { ROUTES } from "@/shared/model/routes.tsx";
import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "@/shared/ui/kit/carousel";

type Playlist = {
  id: string;
  name: string;
  coverImg: string;
  tracks: unknown[];
};

const mockData: Array<Playlist> = [
  {
    id: "1",
    name: "playlist1",
    coverImg:
      "https://i.pinimg.com/736x/f3/99/d0/f399d058d1786f534b447503b1b76f54.jpg",
    tracks: [null, null, null],
  },
  {
    id: "2",
    name: "playlist2",
    coverImg:
      "https://i.pinimg.com/736x/c3/06/ee/c306ee7ee3e1ddf93b99104c00ab8dc0.jpg",
    tracks: [null, null, null],
  },
  {
    id: "3",
    name: "playlist3",
    coverImg:
      "https://i.pinimg.com/736x/3c/82/c8/3c82c83d956a69eea6f869be76abfe53.jpg",
    tracks: [null, null, null],
  },
  {
    id: "4",
    name: "playlist4",
    coverImg:
      "https://i.pinimg.com/736x/39/af/72/39af725002f52e383dbe0ac20b498454.jpg",
    tracks: [null, null, null],
  },
  {
    id: "5",
    name: "playlist5",
    coverImg:
      "https://i.pinimg.com/736x/10/64/26/106426e6118b0c14c00830524cf3e8dd.jpg",
    tracks: [null, null, null],
  },
  {
    id: "6",
    name: "playlist6",
    coverImg:
      "https://i.pinimg.com/1200x/0e/04/1e/0e041e5509f0cee7a6f7dfdbfd066ebb.jpg",
    tracks: [null, null, null],
  },
  {
    id: "7",
    name: "playlist7",
    coverImg:
      "https://i.pinimg.com/736x/3b/a2/54/3ba254909204e5dd33563f9b3e4ec12c.jpg",
    tracks: [null, null, null],
  },
  {
    id: "8",
    name: "playlist8",
    coverImg:
      "https://i.pinimg.com/736x/57/21/6e/57216e599f2fd297400bb80ad05c12a0.jpg",
    tracks: [null, null, null],
  },
  {
    id: "9",
    name: "playlist9",
    coverImg:
      "https://i.pinimg.com/1200x/9a/14/c8/9a14c85a999cc226cfcba14123f4e2c2.jpg",
    tracks: [null, null, null],
  },
  {
    id: "10",
    name: "playlist10",
    coverImg:
      "https://i.pinimg.com/1200x/b9/6e/50/b96e50e5952532084543f953f772c44a.jpg",
    tracks: [null, null, null],
  },
];

function HomePage() {
  // const { data, isLoading } = rqClient.useQuery("get", "/api/v1/music");

  // if (isLoading) {
  //   return <div>loading...</div>;
  // }

  return (
    <section className="grow bg-card px-10 py-8 rounded-lg">
      <h1 className="sr-only">Music for every moment.</h1>
      <div>
        <Carousel
          opts={{
            slidesToScroll: 3,
            align: "start",
          }}
          className="flex gap-8 group"
        >
          <CarouselContent>
            {mockData.map((playlist) => (
              <CarouselItem key={playlist.id} className="basis-0.5/5">
                <Link
                  to={href(ROUTES.PLAYLIST, { playlistId: playlist.id })}
                  className="flex flex-col items-center justify-center gap-2 p-3
                rounded-lg hover:bg-accent"
                >
                  <img
                    src={playlist.coverImg}
                    alt={`${playlist.name} image`}
                    className="rounded-md w-38 h-38 object-cover "
                  />
                  <p>{playlist.name}</p>
                </Link>
              </CarouselItem>
            ))}
          </CarouselContent>
          <CarouselPrevious
            variant="secondary"
            className="left-0 transition-all transform -translate-x-1/2 cursor-pointer
            opacity-0 group-hover:opacity-100 group-hover:translate-x-0 disabled:hidden"
          >
            previous
          </CarouselPrevious>
          <CarouselNext
            variant="secondary"
            className="right-0 transition-all transform translate-x-1/2 cursor-pointer
            opacity-0 group-hover:opacity-100 group-hover:translate-x-0 disabled:hidden"
          >
            next
          </CarouselNext>
        </Carousel>
      </div>
    </section>
  );
}

export const Component = HomePage;
