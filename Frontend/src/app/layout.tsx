import { SidebarProvider } from "@/shared/ui/kit/sidebar";
import { type ReactNode } from "react";
import { AppSidebar } from "@/features/sidebar";

export function MainLayout({ children }: { children: ReactNode }) {
  return (
    <SidebarProvider className="grow min-h-full">
      <div className="flex grow gap-2 mx-2">
        <aside className="h-full">
          <AppSidebar />
        </aside>
        <main className="grow">{children}</main>
      </div>
    </SidebarProvider>
  );
}
