import createFetchClient from "openapi-fetch";
import createClient from "openapi-react-query";
import { CONFIG } from "@/shared/model/config";
import { useSession } from "@/shared/model/session.ts";
import type { ApiPaths, ApiSchemas } from "@/shared/api/schema";

export const fetchClient = createFetchClient<ApiPaths>({
  baseUrl: CONFIG.API_BASE_URL,
});

export const rqClient = createClient(fetchClient);

export const publicFetchClient = createFetchClient<ApiPaths>({
  baseUrl: CONFIG.API_BASE_URL,
});

export const publicRqClient = createClient(publicFetchClient);

fetchClient.use({
  async onRequest({ request }) {
    const session = useSession.getState();
    const token = await session.getFreshToken();
    if (token) {
      request.headers.set("Authorization", `Bearer ${token}`);
    } else {
      return new Response(
        JSON.stringify({
          code: "NOT_AUTHORIZED",
          message: "You are not authorized to access this resource",
        } as ApiSchemas["ErrorResponse"]),
        {
          status: 401,
          headers: {
            "Content-Type": "application/json",
          },
        },
      );
    }
  },
});
