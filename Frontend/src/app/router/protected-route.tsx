import {useSession} from "@/shared/model/session.ts";
import {Navigate, Outlet, redirect} from "react-router-dom";
import {ROUTES} from "@/shared/model/routes.tsx";

export function ProtectedRoute() {
  const {session} = useSession()

  if (!session) {
    return <Navigate to={ROUTES.LOGIN} />
  }

  return <Outlet />
}

export async function protectedLoader() {
  const session = useSession.getState()
  const token = await session.getFreshToken()

  if (!token) {
    return redirect(ROUTES.LOGIN)
  }

  return null
}