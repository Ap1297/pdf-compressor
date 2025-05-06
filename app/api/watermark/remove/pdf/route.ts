import { type NextRequest, NextResponse } from "next/server"

const prod = "https://pdf-compressor-bprg.onrender.com/api/watermark"
const local = "http://localhost:8080/api/watermark"
const SPRING_BOOT_API = local

export async function POST(request: NextRequest) {
  try {
    const formData = await request.formData()

    // Forward the request to Spring Boot
    const response = await fetch(`${SPRING_BOOT_API}/remove/pdf`, {
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
    console.error("Error removing watermark from PDF:", error)
    return NextResponse.json({ error: "Failed to remove watermark from PDF" }, { status: 500 })
  }
}
