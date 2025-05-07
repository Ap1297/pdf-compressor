import { type NextRequest, NextResponse } from "next/server"

const prod = "https://pdf-compressor-bprg.onrender.com/api/video"
const local = "http://localhost:8080/api/video"
const SPRING_BOOT_API = prod

export async function POST(request: NextRequest) {
  try {
    const formData = await request.formData()

    // Forward the request to Spring Boot
    const response = await fetch(`${SPRING_BOOT_API}/compress`, {
      method: "POST",
      body: formData, // Forward the form data as is
    })

    if (!response.ok) {
      const errorData = await response.json()
      return NextResponse.json({ error: errorData.message || "Compression failed" }, { status: response.status })
    }

    const data = await response.json()

    // Transform the response to match our frontend expectations
    return NextResponse.json({
      success: data.success,
      downloadUrl: `/api/video?file=${data.fileName}`, // Updated path
      originalSize: data.originalSize,
      compressedSize: data.compressedSize,
    })
  } catch (error) {
    console.error("Error processing video:", error)
    return NextResponse.json({ error: "Failed to process video" }, { status: 500 })
  }
}

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

    // Get the content type from the response
    const contentType = response.headers.get("content-type") || "application/octet-stream"

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
