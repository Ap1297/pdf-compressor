import { type NextRequest, NextResponse } from "next/server"

// Spring Boot API URL
const SPRING_BOOT_API = "http://localhost:8080/api/watermark"

export async function POST(request: NextRequest) {
  try {
    const formData = await request.formData()
    const fileType = formData.get("fileType") as string

    if (!fileType || (fileType !== "image" && fileType !== "pdf")) {
      return NextResponse.json({ error: "Invalid file type" }, { status: 400 })
    }

    // Forward the request to Spring Boot
    const response = await fetch(`${SPRING_BOOT_API}/remove/${fileType}`, {
      method: "POST",
      body: formData, // Forward the form data as is
    })

    if (!response.ok) {
      const errorData = await response.json()
      return NextResponse.json({ error: errorData.message || "Watermark removal failed" }, { status: response.status })
    }

    const data = await response.json()

    // Transform the response to match our frontend expectations
    return NextResponse.json({
      success: data.success,
      downloadUrl: `/api/watermark/download?file=${data.fileName}`,
      fileType: data.fileType,
      message: data.message,
    })
  } catch (error) {
    console.error("Error removing watermark:", error)
    return NextResponse.json({ error: "Failed to remove watermark" }, { status: 500 })
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
