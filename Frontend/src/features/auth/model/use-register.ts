import {rqClient} from "@/shared/api/instance.ts";
import {ROUTES} from "@/shared/model/routes.tsx";
import {useNavigate} from "react-router-dom";
import type {ApiSchemas} from "@/shared/api/schema";

export function useRegister() {
  const navigate = useNavigate()
  const registerMutation = rqClient.useMutation("post", "/api/auth/signup", {
    onSuccess() {
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