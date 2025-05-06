import { type NextRequest, NextResponse } from "next/server"

const prod = "https://pdf-compressor-bprg.onrender.com/api/convert"
const local = "http://localhost:8080/api/convert"
const SPRING_BOOT_API = prod

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    const fileName = searchParams.get("file")

    if (!fileName) {
      return NextResponse.json({ error: "No file specified" }, { status: 400 })
    }

    // Forward the request to Spring Boot
    const response = await fetch(`${SPRING_BOOT_API}/download/${fileName}`)

    if (!response.ok) {
      return NextResponse.json({ error: "File not found" }, { status: response.status })
    }

    // Get the file content
    const fileBuffer = await response.arrayBuffer()

    // Determine content type based on file extension
    let contentType = "application/octet-stream"
    if (fileName.endsWith(".pdf")) {
      contentType = "application/pdf"
    } else if (fileName.endsWith(".docx")) {
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }

    // Set appropriate headers for file download
    const headers = new Headers()
    headers.set("Content-Disposition", `attachment; filename=${fileName}`)
    headers.set("Content-Type", contentType)

    return new NextResponse(fileBuffer, {
      status: 200,
      headers,
    })
  } catch (error) {
    console.error("Error downloading file:", error)
    return NextResponse.json({ error: "Failed to download file" }, { status: 500 })
  }
}

