import type React from "react"
import { Github, Linkedin, Twitter } from "lucide-react"
import Link from "next/link"

interface SocialLink {
  icon: React.ReactNode
  href: string
  label: string
}

export function CreatorBadge() {
  const socialLinks: SocialLink[] = [
    {
      icon: <Github className="h-4 w-4" />,
      href: "https://github.com/ap1297",
      label: "GitHub",
    },
    {
      icon: <Linkedin className="h-4 w-4" />,
      href: "https://www.linkedin.com/in/ankit-panchal-developer/",
      label: "LinkedIn",
    },
  ]

  return (
    <div className="flex flex-col items-center md:items-start gap-2">
      <div className="inline-flex items-center px-3 py-1 rounded-full bg-primary/10 border border-primary/20 shadow-sm transition-all duration-300 hover:bg-primary/20 hover:shadow-md">
        <span className="text-xs font-medium mr-1">👨‍💻</span>
        <span className="text-sm font-medium bg-gradient-to-r from-primary to-purple-600 bg-clip-text text-transparent">
          Crafted by Ankit Panchal
        </span>
      </div>

      <div className="flex gap-2">
        {socialLinks.map((link) => (
          <Link
            key={link.label}
            href={link.href}
            target="_blank"
            rel="noopener noreferrer"
            className="p-1.5 rounded-full bg-muted/50 hover:bg-muted transition-colors duration-200 text-muted-foreground hover:text-foreground"
            aria-label={link.label}
          >
            {link.icon}
            <span className="sr-only">{link.label}</span>
          </Link>
        ))}
      </div>
    </div>
  )
}
