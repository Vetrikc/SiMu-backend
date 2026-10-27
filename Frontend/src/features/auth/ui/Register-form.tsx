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
import {
  type RegisterFormData, registerSchema
} from "@/shared/constants/auth.ts";
import {Spinner} from "@/shared/ui/kit/spinner.tsx";
import {useLogin} from "src/features/auth/model/use-login.ts";
import {useRegister} from "src/features/auth/model/use-register.ts";


export function RegisterForm() {

  const form = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      username: "",
      email: "",
      password: "",
      confirmPassword: "",
    },
  })

  const {errorMessage, isPending, register} = useRegister()


  const onSubmit = form.handleSubmit(register)

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
          name="email"
          render={({field}) => (
            <FormItem>
              <FormLabel>Email</FormLabel>
              <FormControl>
                <Input
                  placeholder="example@gmail.com"
                  type="email" {...field} />
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

        <FormField
          control={form.control}
          name="confirmPassword"
          render={({field}) => (
            <FormItem>
              <FormLabel>Confirm password</FormLabel>
              <FormControl>
                <Input type="password" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {errorMessage && (
          <p className="text-destructive text-sm">{errorMessage}</p>
        )}


        <Button disabled={isPending} type="submit">
          {isPending ? (<Spinner/>) : ("Sign up")}
        </Button>
      </form>
    </Form>
  )
}