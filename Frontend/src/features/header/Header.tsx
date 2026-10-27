import { Link } from "react-router-dom";
import { ROUTES } from "@/shared/model/routes.tsx";
import { Button } from "@/shared/ui/kit/button.tsx";
import { Search } from "@/features/search";
import { useEffect, useRef, useState } from "react";
import { useMediaQuery } from "@/shared/model/hooks/useMediaQuery.ts";
import { UserMenu } from "@/features/header/UserMenu.tsx";

export function AppHeader() {
  const isMobile = useMediaQuery("(max-width: 639px)");
  const [isSearchOpen, setIsSearchOpen] = useState<boolean>(true);
  const searchRef = useRef<HTMLInputElement | null>(null);

  const handleSearchClick = () => {
    if (!isMobile) {
      searchRef.current?.focus();
      return;
    }
    setIsSearchOpen((prev) => !prev);
    if (!isSearchOpen) {
      searchRef.current?.focus();
    }
  };

  useEffect(() => {
    setIsSearchOpen(!isMobile);
  }, [isMobile]);

  return (
    <header className="z-50 flex items-center justify-between gap-5 p-2">
      <Button
        size="lg"
        variant="outline"
        asChild
        className="hover:bg-accent max-sm:p-4"
      >
        <Link to={ROUTES.HOME}>
          <h1 className="text-center text-2xl font-bold tracking-tight max-sm:text-xl">
            SiMu
          </h1>
        </Link>
      </Button>

      <Search
        isOpen={isSearchOpen}
        onClick={handleSearchClick}
        ref={searchRef}
      />

      {!(isMobile && isSearchOpen) && (
        <div className="flex">
          <UserMenu />
        </div>
      )}
    </header>
  );
}
