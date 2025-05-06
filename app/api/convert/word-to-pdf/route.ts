import { type NextRequest, NextResponse } from "next/server"

const prod = "https://pdf-compressor-bprg.onrender.com/api/convert"
const local = "http://localhost:8080/api/convert"
const SPRING_BOOT_API = prod

export async function POST(request: NextRequest) {
  try {
    const formData = await request.formData()

    // Forward the request to Spring Boot
    const response = await fetch(`${SPRING_BOOT_API}/word-to-pdf`, {
      method: "POST",
      body: formData, // Forward the form data as is
    })

    if (!response.ok) {
      const errorData = await response.json()
      return NextResponse.json({ error: errorData.message || "Conversion failed" }, { status: response.status })
    }

    const data = await response.json()

    // Transform the response to match our frontend expectations
    return NextResponse.json({
      success: data.success,
      downloadUrl: `/api/convert/download?file=${data.fileName}`,
      sourceFormat: data.sourceFormat,
      targetFormat: data.targetFormat,
      message: data.message,
    })
  } catch (error) {
    console.error("Error converting Word to PDF:", error)
    return NextResponse.json({ error: "Failed to convert Word to PDF" }, { status: 500 })
  }
}
