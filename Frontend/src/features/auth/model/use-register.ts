import {publicRqClient} from "@/shared/api/instance.ts";
import {ROUTES} from "@/shared/model/routes.tsx";
import {useNavigate} from "react-router-dom";
import type {ApiSchemas} from "@/shared/api/schema";
import {useSession} from "@/shared/model/session.ts";

export function useRegister() {
  const navigate = useNavigate()

  const createSession = useSession(state => state.updateToken)

  const registerMutation = publicRqClient.useMutation("post", "/api/auth/signup", {
    onSuccess(data) {
      createSession(data.token)
      navigate(ROUTES.HOME)
    }
  })

  const register = (data: ApiSchemas["SignupRequest"]) => {
    registerMutation.mutate({body: data})
  }
  const errorMessage = registerMutation.isError
    ? registerMutation.error.message
    : undefined

  return {
    register,
    isPending: registerMutation.isPending,
    errorMessage
  }
}