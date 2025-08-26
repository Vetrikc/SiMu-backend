import {AuthLayout} from "@/features/auth/ui/Auth-layout.tsx";
import {Link} from "react-router-dom";
import {ROUTES} from "@/shared/model/routes.tsx";
import {RegisterForm} from "@/features/auth/ui/Register-form.tsx";

function RegisterPage() {
  return <AuthLayout
    form={<RegisterForm />}
    title="Sign up"
    description="Sign up for free to access everything from our product"
    footerText={<>Already have an account? <Link to={ROUTES.LOGIN}>Sign in</Link></>}
  />;
}

export const Component = RegisterPage;
