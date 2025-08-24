import {rqClient} from "@/shared/api/instance.ts";
import {ROUTES} from "@/shared/model/routes.tsx";
import {useNavigate} from "react-router-dom";
import type {ApiSchemas} from "@/shared/api/schema";

export function useLogin() {
  const navigate = useNavigate()
  const loginMutation = rqClient.useMutation("post", "/api/auth/signin", {
    onSuccess() {
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