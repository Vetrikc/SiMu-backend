import { useState } from "react";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
} from "@/shared/ui/kit/dropdown-menu";
import { Button } from "@/shared/ui/kit/button";
import { Avatar, AvatarImage, AvatarFallback } from "@/shared/ui/kit/avatar";
import { Link } from "react-router-dom";
import { ROUTES } from "@/shared/model/routes";
import { useSession } from "@/shared/model/session.ts";

export function UserMenu() {
  const [open, setOpen] = useState(false);
  const logout = useSession((state) => state.removeToken);

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button
          className="rounded-full p-6 hover:scale-105 cursor-pointer"
          variant="secondary"
          size="icon"
        >
          <Avatar>
            <AvatarImage src="https://github.com/shadcn.png" />
            <AvatarFallback />
          </Avatar>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="mr-2 mt-2">
        <DropdownMenuItem asChild onClick={() => setOpen(false)}>
          <Link to={ROUTES.PROFILE}>Profile</Link>
        </DropdownMenuItem>
        <DropdownMenuItem asChild onClick={() => setOpen(false)}>
          <Link to={ROUTES.SETTINGS}>Settings</Link>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          onClick={() => {
            logout();
            setOpen(false);
          }}
        >
          Log out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
