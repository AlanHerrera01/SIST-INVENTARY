import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const alertVariants = cva("relative w-full rounded-lg border p-4 text-sm [&>svg]:absolute [&>svg]:left-4 [&>svg]:top-4 [&>svg+div]:pl-7", {
  variants: {
    variant: {
      default: "bg-card text-foreground",
      destructive: "border-red-200 bg-red-50 text-red-800 [&>svg]:text-red-600",
      warning: "border-amber-200 bg-amber-50 text-amber-800 [&>svg]:text-amber-600",
      success: "border-emerald-200 bg-emerald-50 text-emerald-800 [&>svg]:text-emerald-600",
    },
  },
  defaultVariants: { variant: "default" },
})

function Alert({ className, variant, ...props }: React.ComponentProps<"div"> & VariantProps<typeof alertVariants>) {
  return <div data-slot="alert" role="alert" className={cn(alertVariants({ variant }), className)} {...props} />
}
function AlertTitle({ className, ...props }: React.ComponentProps<"div">) {
  return <div data-slot="alert-title" className={cn("mb-1 font-medium", className)} {...props} />
}
function AlertDescription({ className, ...props }: React.ComponentProps<"div">) {
  return <div data-slot="alert-description" className={cn("text-sm opacity-90", className)} {...props} />
}

export { Alert, AlertTitle, AlertDescription }
