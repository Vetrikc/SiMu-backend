import {useForm} from "react-hook-form";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel, FormMessage
} from "@/shared/ui/kit/form.tsx";
import {Button} from "@/shared/ui/kit/button.tsx";
import {Input} from "@/shared/ui/kit/input.tsx";
import {zodResolver} from "@hookform/resolvers/zod"
import {type LoginFormData, loginSchema} from "@/shared/constants/auth.ts";
import {useLogin} from "src/features/auth/model/use-login.ts";
import {Spinner} from "@/shared/ui/kit/spinner.tsx";


export function LoginForm() {

  const form = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  })

  const {errorMessage, isPending, login} = useLogin()

  const onSubmit = form.handleSubmit(login)

  return (
    <Form {...form}>
      <form
        className="flex flex-col gap-4"
        onSubmit={onSubmit}
      >
        <FormField
          control={form.control}
          name="username"
          render={({field}) => (
            <FormItem>
              <FormLabel>Username</FormLabel>
              <FormControl>
                <Input
                  placeholder="Username"
                  type="text" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="password"
          render={({field}) => (
            <FormItem>
              <FormLabel>Password</FormLabel>
              <FormControl>
                <Input
                  placeholder="**********"
                  type="password" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {errorMessage && (
          <p className="text-destructive text-sm">{errorMessage}</p>
        )}

        <Button
          disabled={isPending}
          type="submit"
        >
          {isPending ? (<Spinner />) : ("Sign in")}
        </Button>
      </form>
    </Form>
  )
}