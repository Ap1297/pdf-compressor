import { type NextRequest, NextResponse } from "next/server"
import { writeFile, mkdir } from "fs/promises"
import { join } from "path"
import { exec } from "child_process"
import { promisify } from "util"
import { randomUUID } from "crypto"

const execPromise = promisify(exec)

// Ensure upload and output directories exist
async function ensureDirectories() {
  try {
    await mkdir(join(process.cwd(), "uploads"), { recursive: true })
    await mkdir(join(process.cwd(), "outputs"), { recursive: true })
  } catch (error) {
    console.error("Error creating directories:", error)
  }
}

export async function POST(request: NextRequest) {
  try {
    await ensureDirectories()

    const formData = await request.formData()
    const file = formData.get("file") as File
    const compressionLevel = formData.get("compressionLevel") as string

    if (!file) {
      return NextResponse.json({ error: "No file provided" }, { status: 400 })
    }

    // Generate unique filenames
    const fileId = randomUUID()
    const inputPath = join(process.cwd(), "uploads", `${fileId}.pdf`)
    const outputPath = join(process.cwd(), "outputs", `${fileId}_compressed.pdf`)

    // Convert File to Buffer and save to disk
    const buffer = Buffer.from(await file.arrayBuffer())
    await writeFile(inputPath, buffer)

    // Determine compression settings based on compressionLevel
    const level = Number.parseInt(compressionLevel) || 50
    const quality = Math.max(0.1, 1 - level / 100)

    // Execute Java PDF compression (simulated here - would call actual Java process)
    // In a real implementation, you would call your Java application here
    await simulateJavaCompression(inputPath, outputPath, quality)

    // Get file stats for the compressed file
    const fs = require("fs")
    const compressedStats = fs.statSync(outputPath)
    const compressedSize = compressedStats.size

    // In a real application, you would store files in a proper storage service
    // For this example, we'll just return a simulated download URL
    const downloadUrl = `/api/download?file=${fileId}_compressed.pdf`

    return NextResponse.json({
      success: true,
      downloadUrl,
      compressedSize,
    })
  } catch (error) {
    console.error("Error processing PDF:", error)
    return NextResponse.json({ error: "Failed to process PDF" }, { status: 500 })
  }
}

// This function simulates the Java PDF compression process
// In a real application, you would call your Java application here
async function simulateJavaCompression(inputPath: string, outputPath: string, quality: number) {
  // Simulate compression by copying the file (in a real app, you'd call Java here)
  const jarPath = join(process.cwd(), "java", "target", "pdf-compressor-1.0-SNAPSHOT-jar-with-dependencies.jar")
  
  // Execute the Java application with the provided parameters
  const command = `java -jar "${jarPath}" "${inputPath}" "${outputPath}" ${quality}`
  
  console.log(`Executing: ${command}`)
  
  try {
    const { stdout, stderr } = await execPromise(command)
    
    if (stderr) {
      console.error(`Java process stderr: ${stderr}`)
    }
    
    console.log(`Java process stdout: ${stdout}`)
    return outputPath
  } catch (error) {
    console.error("Error executing Java process:", error)
    throw error
  }
}
