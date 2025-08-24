import {z} from 'zod';

const USERNAME = {
  MIN: 2,
  MAX: 20,
  PATTERN: /^[a-zA-Z0-9_-]+$/, // только буквы, цифры, дефисы и подчеркивания
} as const;

const EMAIL = {
  MAX: 254
} as const

const PASSWORD = {
  MIN: 6,
  MAX: 100,
  PATTERN: /(?=.*[A-Z])(?=.*\d)[A-Za-z\d]/, // минимум 1 цифра, 1 заглавная
} as const;

export const loginSchema = z.object({
  username: z.string()
    .min(USERNAME.MIN, {
      message: `Username must contain at least ${USERNAME.MIN} characters`
    })
    .max(USERNAME.MAX, {
      message: `Username must not exceed ${USERNAME.MAX} characters`
    })
    .regex(USERNAME.PATTERN, {
      message: 'Username can only contain letters (A-Z, a-z), numbers, hyphens and underscores'
    })
    .trim(),

  password: z.string()
    .min(PASSWORD.MIN, {
      message: `Password must contain at least ${PASSWORD.MIN} characters`
    })
    .max(PASSWORD.MAX, {
      message: `Password must not exceed ${PASSWORD.MAX} characters`
    })
    .regex(PASSWORD.PATTERN, {
      message: 'Password must contain at least one uppercase letter and one digit'
    }),
});


export const registerSchema = z.object({
  username: z.string()
    .min(USERNAME.MIN, {
      message: `Username must contain at least ${USERNAME.MIN} characters`
    })
    .max(USERNAME.MAX, {
      message: `Username must not exceed ${USERNAME.MAX} characters`
    })
    .regex(USERNAME.PATTERN, {
      message: 'Username can only contain letters, numbers, hyphens and underscores'
    })
    .trim(),

  email: z.string({
    message: "Email is required"
  }).email({
    message: "Please enter a valid email address"
  })
    .max(EMAIL.MAX, {message: "Email cannot exceed 254 characters"})
    .trim(),

  password: z.string()
    .min(PASSWORD.MIN, {
      message: `Password must contain at least ${PASSWORD.MIN} characters`
    })
    .max(PASSWORD.MAX, {
      message: `Password must not exceed ${PASSWORD.MAX} characters`
    })
    .regex(PASSWORD.PATTERN, {
      message: 'Password must contain at least one uppercase letter and one digit'
    }),

  confirmPassword: z.string().optional(),
})
  .refine((data) => data.password === data.confirmPassword, {
    path: ["confirmPassword"],
    message: "Passwords are not equal"
  })


export type LoginFormData = z.infer<typeof loginSchema>;
export type RegisterFormData = z.infer<typeof registerSchema>;
