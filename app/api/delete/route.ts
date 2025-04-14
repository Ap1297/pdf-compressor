import { type NextRequest, NextResponse } from "next/server"

const prod = "https://pdf-compressor-production.up.railway.app/api"
const local = "http://localhost:8080/api"
const SPRING_BOOT_API = prod

export async function DELETE(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    const fileName = searchParams.get("file")

    if (!fileName) {
      return NextResponse.json({ error: "No file specified" }, { status: 400 })
    }

    // Forward the request to Spring Boot
    const response = await fetch(`${SPRING_BOOT_API}/delete/${fileName}`, {
      method: "DELETE",
    })

    if (!response.ok) {
      const errorData = await response.json()
      return NextResponse.json({ error: errorData.message || "Deletion failed" }, { status: response.status })
    }

    const data = await response.json()
    return NextResponse.json(data)
  } catch (error) {
    console.error("Error deleting file:", error)
    return NextResponse.json({ error: "Failed to delete file" }, { status: 500 })
  }
}
