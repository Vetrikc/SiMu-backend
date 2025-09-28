import { queryClient } from "@/shared/api/query-client.ts";
import { QueryClientProvider } from "@tanstack/react-query";
import ThemeProvider from "@/app/providers/theme-provider.tsx";

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme">
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </ThemeProvider>
  );
}
