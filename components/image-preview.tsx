"use client"

import { useState } from "react"
import { X } from "lucide-react"
import { Button } from "@/components/ui/button"

interface ImagePreviewProps {
  src: string
  alt?: string
  onClose?: () => void
}

export function ImagePreview({ src, alt = "Preview", onClose }: ImagePreviewProps) {
  const [isLoading, setIsLoading] = useState(true)

  return (
    <div className="relative w-full max-w-md mx-auto border rounded-md overflow-hidden bg-muted/20">
      {onClose && (
        <Button
          variant="ghost"
          size="icon"
          className="absolute top-2 right-2 z-10 bg-background/80 hover:bg-background"
          onClick={onClose}
        >
          <X className="h-4 w-4" />
          <span className="sr-only">Close preview</span>
        </Button>
      )}

      {isLoading && (
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
        </div>
      )}

      <img
        src={src || "/placeholder.svg"}
        alt={alt}
        className="w-full h-auto object-contain"
        style={{ maxHeight: "300px" }}
        onLoad={() => setIsLoading(false)}
      />
    </div>
  )
}
