import { FileUp } from "lucide-react"

interface LogoProps {
  size?: "sm" | "md" | "lg"
  showText?: boolean
}

export function Logo({ size = "md", showText = true }: LogoProps) {
  const sizeClasses = {
    sm: "h-6 w-6",
    md: "h-8 w-8",
    lg: "h-10 w-10",
  }

  const textSizeClasses = {
    sm: "text-lg",
    md: "text-xl",
    lg: "text-2xl",
  }

  return (
    <div className="flex items-center gap-2">
      <div className="bg-primary text-primary-foreground p-2 rounded-lg">
        <FileUp className={sizeClasses[size]} />
      </div>
      {showText && (
        <div className="font-bold tracking-tight leading-none">
          <span className={`${textSizeClasses[size]} text-primary`}>File</span>
          <span className={`${textSizeClasses[size]}`}>Compressor</span>
        </div>
      )}
    </div>
  )
}
