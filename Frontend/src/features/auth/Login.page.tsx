import {AuthLayout} from "@/features/auth/ui/Auth-layout.tsx";
import {Link} from "react-router-dom";
import {ROUTES} from "@/shared/model/routes.tsx";
import {LoginForm} from "@/features/auth/ui/Login-form.tsx";

function LoginPage() {
  return <AuthLayout
    form={<LoginForm/>}
    title="Sign in"
    description="Enter your username and password to enter the site"
    footerText={<>Don't have an account? <Link to={ROUTES.REGISTER}>Sign up</Link></>}
  />
}

export const Component = LoginPage;
