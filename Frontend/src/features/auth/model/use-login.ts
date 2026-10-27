import {publicRqClient} from "@/shared/api/instance.ts";
import {ROUTES} from "@/shared/model/routes.tsx";
import {useNavigate} from "react-router-dom";
import type {ApiSchemas} from "@/shared/api/schema";
import {useSession} from "@/shared/model/session.ts";

export function useLogin() {
  const navigate = useNavigate()

  const createSession = useSession(state => state.updateToken)

  const loginMutation = publicRqClient.useMutation("post", "/api/auth/signin", {
    onSuccess(data) {
      createSession(data.token)
      navigate(ROUTES.HOME)
    }
  })

  const login = (data: ApiSchemas["SigninRequest"]) => {
    loginMutation.mutate({body: data})
  }
  const errorMessage = loginMutation.isError
    ? loginMutation.error.message
    : undefined

  return {
    login,
    isPending: loginMutation.isPending,
    errorMessage
  }
}