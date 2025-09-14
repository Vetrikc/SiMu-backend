import {Button} from "@/shared/ui/kit/button";
import {Input} from "@/shared/ui/kit/input.tsx";
import {GalleryVerticalEnd, Search as SearchIcon, X} from "lucide-react";
import {Link, useLocation} from "react-router-dom";
import {ROUTES} from "@/shared/model/routes.tsx";
import {cn} from "@/shared/lib/css.ts";
import {forwardRef, useState} from "react";

type SearchProps = {
  isOpen: boolean
  onClick: () => void,
}

export const Search = forwardRef<HTMLInputElement, SearchProps>(
({ isOpen, onClick }, ref) => {
  const navigator = useLocation()
  const [value, setValue] = useState('')

  const onClear = () => {
    setValue('')
  }

  return (
    <div className="relative group w-xl min-2xl:w-3xl">
      <button
        className="absolute left-3 top-1/2 transform -translate-y-1/2
          max-sm:p-3 max-sm:left-0 max-sm:cursor-pointer"
        onClick={onClick}
      >
        <SearchIcon
          className="text-muted-foreground group-hover:text-accent-foreground
           transition-colors group-focus-within:text-accent-foreground"
        />
      </button>
      <Input
        ref={ref}
        name="search"
        type="search"
        placeholder="What do you want to play?"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        className={cn(
          "w-full pl-12 pr-24 h-14 text-lg rounded-3xl group-hover:bg-accent!",
          "transition-all max-sm:placeholder:text-transparent max-sm:h-12 min-2xl:text-xl",
          !isOpen && "px-0 w-12 bg-secondary! border-none")}
      />
      {value && (
        <Button
          variant="ghost"
          size="icon"
          onClick={onClear}
          className="absolute right-16 top-1/2 transform -translate-y-1/2 cursor-pointer
             size-8 rounded-full bg-transparent text-muted-foreground hover:scale-105
             hover:text-accent-foreground transition-all"
          onMouseDown={(e) => {
            e.preventDefault();
          }}
        >
          <X className="size-6" />
        </Button>
      )}
      {isOpen && (
        <div className="absolute flex items-center right-3 top-1/2 transform -translate-y-1/2">
          <div className="bg-muted-foreground h-6 w-px mr-2"></div>
          <Button
            size="icon"
            variant="ghost"
            asChild
            className=" text-muted-foreground hover:scale-105 hover:text-accent-foreground "
          >
            <Link to={ROUTES.SEARCH}><GalleryVerticalEnd
              className={cn("size-6 ",
                navigator.pathname === ROUTES.SEARCH && "text-accent-foreground fill-accent-foreground")}
            /></Link>
          </Button>
        </div>)}
    </div>
  )
})