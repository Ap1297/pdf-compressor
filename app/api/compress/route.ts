import { type NextRequest, NextResponse } from "next/server"

// Spring Boot API URL
const prod = "http://3.142.245.96:8081/api"
const local = "http://localhost:8081/api"
const SPRING_BOOT_API = prod

export async function POST(request: NextRequest) {
  try {
    const formData = await request.formData()
    console.log(SPRING_BOOT_API);
    console.log(formData);
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
      downloadUrl: `/api/download?file=${data.fileName}`,
      originalSize: data.originalSize,
      compressedSize: data.compressedSize,
    })
  } catch (error) {
    console.error("Error processing PDF:", error)
    return NextResponse.json({ error: "Failed to process PDF" }, { status: 500 })
  }
}