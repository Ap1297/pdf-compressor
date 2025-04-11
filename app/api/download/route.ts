import { type NextRequest, NextResponse } from "next/server"
import { readFile } from "fs/promises"
import { join } from "path"
import { stat } from "fs/promises"

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    const fileName = searchParams.get("file")

    if (!fileName) {
      return NextResponse.json({ error: "No file specified" }, { status: 400 })
    }

    const filePath = join(process.cwd(), "outputs", fileName)

    try {
      await stat(filePath)
    } catch (error) {
      return NextResponse.json({ error: "File not found" }, { status: 404 })
    }

    const fileBuffer = await readFile(filePath)

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
