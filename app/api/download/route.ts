import { type NextRequest, NextResponse } from "next/server"

const prod = "https://pdf-compressor-production.up.railway.app/api"
const local = "http://localhost:8080/api"
const SPRING_BOOT_API = prod

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    const fileName = searchParams.get("file")

    if (!fileName) {
      return NextResponse.json({ error: "No file specified" }, { status: 400 })
    }
console.log(SPRING_BOOT_API);
    // Forward the request to Spring Boot
    const response = await fetch(`${SPRING_BOOT_API}/download/${fileName}`)

    if (!response.ok) {
      return NextResponse.json({ error: "File not found" }, { status: response.status })
    }

    // Get the file content
    const fileBuffer = await response.arrayBuffer()

    // Set appropriate headers for file download
    const headers = new Headers()
    headers.set("Content-Disposition", `attachment; filename=${fileName}`)
    headers.set("Content-Type", "application/pdf")

    return new NextResponse(fileBuffer, {
      status: 200,
      headers,
    })
  } catch (error) {
    console.error("Error downloading file:", error)
    return NextResponse.json({ error: "Failed to download file" }, { status: 500 })
  }
}
