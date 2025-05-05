import { type NextRequest, NextResponse } from "next/server"

const prod = "https://pdf-compressor-bprg.onrender.com/api/image"
const local = "http://localhost:8080/api/image"
const SPRING_BOOT_API = local

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
      downloadUrl: `/api/image/download?file=${data.fileName}`,
      originalSize: data.originalSize,
      compressedSize: data.compressedSize,
    })
  } catch (error) {
    console.error("Error processing image:", error)
    return NextResponse.json({ error: "Failed to process image" }, { status: 500 })
  }
}
