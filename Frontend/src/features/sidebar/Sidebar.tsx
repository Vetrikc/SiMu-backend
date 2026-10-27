import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarTrigger,
  useSidebar,
} from "@/shared/ui/kit/sidebar.tsx";
import {
  CirclePlus,
  Handshake,
  HeartPlus,
  Home,
  LibraryBig,
  UserStar,
} from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { ROUTES } from "@/shared/model/routes.tsx";
import { Button } from "@/shared/ui/kit/button.tsx";
import { cn } from "@/shared/lib/css.ts";
import { useIsMobile } from "@/shared/lib/react/use-mobile.ts";

const LINKS = [
  {
    title: "Home",
    url: ROUTES.HOME,
    icon: Home,
  },
  {
    title: "Your Library",
    url: ROUTES.LIBRARY,
    icon: LibraryBig,
  },
  {
    title: "Liked Songs",
    url: ROUTES.SONGS,
    icon: HeartPlus,
  },
  {
    title: "Artists",
    url: ROUTES.ARTISTS,
    icon: UserStar,
  },
  {
    title: "Friends",
    url: ROUTES.FRIENDS,
    icon: Handshake,
  },
];

const BUTTONS = [
  {
    title: "Create Playlist",
    icon: CirclePlus,
  },
];

export function AppSidebar() {
  const navigator = useLocation();
  const isMobile = useIsMobile();
  const { setOpenMobile, state } = useSidebar();

  return (
    <>
      {isMobile && <SidebarTrigger size="lg" />}
      <Sidebar
        collapsible="icon"
        variant="floating"
        className="group relative h-full p-0"
      >
        <SidebarHeader>
          <SidebarTrigger className="opacity-0 transform -translate-x-1/3 transition-all group-hover:opacity-100 group-hover:translate-x-0" />
        </SidebarHeader>
        <SidebarContent className="text-muted-foreground">
          <SidebarGroup>
            <SidebarGroupContent>
              <SidebarMenu>
                {LINKS.map((link) => (
                  <SidebarMenuItem key={link.title}>
                    {/*do is active*/}
                    <SidebarMenuButton
                      asChild
                      size="lg"
                      onClick={() => setOpenMobile(false)}
                      isActive={
                        navigator.pathname === link.url && state === "expanded"
                      }
                      className={cn(
                        navigator.pathname === link.url &&
                          "text-accent-foreground",
                        state === "expanded"
                          ? "hover:bg-accent/50"
                          : "hover:bg-transparent",
                      )}
                    >
                      <Link to={link.url}>
                        <link.icon className="size-9!" />
                        <span>{link.title}</span>
                      </Link>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
          <SidebarGroup>
            <SidebarGroupContent>
              <SidebarMenu>
                {BUTTONS.map((button) => (
                  <SidebarMenuItem key={button.title}>
                    <SidebarMenuButton
                      asChild
                      size="lg"
                      className={cn(
                        "p-2!",
                        state === "collapsed" && "hover:bg-transparent!",
                      )}
                      onClick={() => setOpenMobile(false)}
                    >
                      <Button
                        variant="ghost"
                        className="justify-start cursor-pointer"
                      >
                        <button.icon className="size-9!" />
                        <span>{button.title}</span>
                      </Button>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        </SidebarContent>
      </Sidebar>
    </>
  );
}
