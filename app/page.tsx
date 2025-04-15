"use client"

import type React from "react"

import { useState } from "react"
import { Upload, FileUp, FileDown, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Slider } from "@/components/ui/slider"
import { toast } from "@/components/ui/use-toast"
import { Toaster } from "@/components/ui/toaster"
import { ThemeToggleWithLabel } from "@/components/theme-toggle-with-label"

export default function PDFCompressor() {
  const [file, setFile] = useState<File | null>(null)
  const [compressedFile, setCompressedFile] = useState<string | null>(null)
  const [compressedFileName, setCompressedFileName] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [compressionLevel, setCompressionLevel] = useState([50])
  const [originalSize, setOriginalSize] = useState<number | null>(null)
  const [compressedSize, setCompressedSize] = useState<number | null>(null)

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0]
      if (selectedFile.type !== "application/pdf") {
        toast({
          title: "Invalid file type",
          description: "Please select a PDF file",
          variant: "destructive",
        })
        return
      }
      if (selectedFile.size > 1024 * 1024 * 1024) { // 1GB
        toast({
          title: "File too large",
          description: "Please select a PDF smaller than 1GB",
          variant: "destructive",
        })
        return
      }
      setFile(selectedFile)
      setOriginalSize(selectedFile.size)
      setCompressedFile(null)
      setCompressedFileName(null)
      setCompressedSize(null)
    }
  }

  const handleCompress = async () => {
    if (!file) return

    setLoading(true)
    setProgress(0)

    const formData = new FormData()
    formData.append("file", file)
    formData.append("compressionLevel", compressionLevel[0].toString())

    try {
      // Simulate progress
      const progressInterval = setInterval(() => {
        setProgress((prev) => {
          if (prev >= 95) {
            clearInterval(progressInterval)
            return 95
          }
          return prev + 5
        })
      }, 300)

      const response = await fetch("/api/compress", {
        method: "POST",
        body: formData,
      })

      clearInterval(progressInterval)
      setProgress(100)

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || "Compression failed")
      }

      const data = await response.json()
      setCompressedFile(data.downloadUrl)
      // Extract the filename from the download URL
      const fileName = data.downloadUrl.split("=")[1]
      setCompressedFileName(fileName)
      // Use the original size from the API response instead of the local file size
      setOriginalSize(data.originalSize)
      setCompressedSize(data.compressedSize)

      toast({
        title: "Compression complete",
        description: `Reduced from ${formatFileSize(data.originalSize)} to ${formatFileSize(data.compressedSize)}`,
      })
    } catch (error) {
      toast({
        title: "Compression failed",
        description: error instanceof Error ? error.message : "An error occurred while compressing the PDF",
        variant: "destructive",
      })
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  const handleDownloadAndDelete = async () => {
    if (!compressedFile || !compressedFileName) return

    // Start the download
    window.location.href = compressedFile

    // Wait a moment to ensure the download has started
    setTimeout(async () => {
      try {
        // Delete the file
        const response = await fetch(`/api/delete?file=${compressedFileName}`, {
          method: "DELETE",
        })

        if (!response.ok) {
          console.error("Failed to delete file:", await response.json())
        } else {
          console.log("File deleted successfully")
        }
      } catch (error) {
        console.error("Error deleting file:", error)
      }
    }, 2000) // Wait 2 seconds to ensure download has started
  }

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return "0 Bytes"
    const k = 1024
    const sizes = ["Bytes", "KB", "MB", "GB"]
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Number.parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i]
  }

  const calculateReduction = () => {
    if (!originalSize || !compressedSize) return 0
    return Math.round(((originalSize - compressedSize) / originalSize) * 100)
  }

  const handleClear = () => {
    // If we have a compressed file, delete it before clearing the state
    if (compressedFileName) {
      fetch(`/api/delete?file=${compressedFileName}`, {
        method: "DELETE",
      }).catch((error) => {
        console.error("Error deleting file during clear:", error)
      })
    }

    setFile(null)
    setCompressedFile(null)
    setCompressedFileName(null)
    setOriginalSize(null)
    setCompressedSize(null)
  }

  return (
    <div className="min-h-screen bg-background transition-colors duration-300">
      <div className="container mx-auto py-10">
        <div className="flex justify-end mb-4">
          <ThemeToggleWithLabel />
        </div>
        <Card className="max-w-2xl mx-auto">
          <CardHeader>
            <CardTitle className="text-2xl flex items-center gap-2">
              <FileDown className="h-6 w-6" />
              PDF Compressor
            </CardTitle>
            <CardDescription>
              Upload a PDF file and compress it to reduce file size while maintaining quality
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="border-2 border-dashed border-muted rounded-lg p-6 text-center">
              <input
                type="file"
                id="pdf-upload"
                className="hidden"
                onChange={handleFileChange}
                accept="application/pdf"
              />
              <label htmlFor="pdf-upload" className="flex flex-col items-center justify-center cursor-pointer">
                <Upload className="h-10 w-10 text-muted-foreground mb-2" />
                <span className="text-sm font-medium">{file ? file.name : "Click to upload or drag and drop"}</span>
                <span className="text-xs text-muted-foreground mt-1">PDF (max. 1GB)</span>
              </label>
            </div>

            {file && (
              <>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm font-medium">Compression Level</span>
                    <span className="text-sm text-muted-foreground">{compressionLevel[0]}%</span>
                  </div>
                  <Slider
                    value={compressionLevel}
                    onValueChange={setCompressionLevel}
                    min={10}
                    max={90}
                    step={10}
                    disabled={loading}
                  />
                  <div className="flex justify-between text-xs text-muted-foreground">
                    <span>Higher Quality</span>
                    <span>Smaller Size</span>
                  </div>
                </div>

                {loading && (
                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span className="text-sm font-medium">Compressing...</span>
                      <span className="text-sm text-muted-foreground">{progress}%</span>
                    </div>
                    <Progress value={progress} className="h-2" />
                  </div>
                )}

                {compressedFile && (
                  <div className="bg-muted/30 p-4 rounded-lg">
                    <div className="flex justify-between mb-2">
                      <span className="text-sm font-medium">Original size:</span>
                      <span className="text-sm">{formatFileSize(originalSize!)}</span>
                    </div>
                    <div className="flex justify-between mb-2">
                      <span className="text-sm font-medium">Compressed size:</span>
                      <span className="text-sm">{formatFileSize(compressedSize!)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-sm font-medium">Reduction:</span>
                      <span
                        className={`text-sm ${calculateReduction() > 0 ? "text-green-600 dark:text-green-400" : "text-red-600 dark:text-red-400"}`}
                      >
                        {calculateReduction() > 0 ? `${calculateReduction()}%` : "No reduction"}
                      </span>
                    </div>
                  </div>
                )}
              </>
            )}
          </CardContent>
          <CardFooter className="flex justify-between">
            <Button variant="outline" onClick={handleClear} disabled={!file || loading}>
              Clear
            </Button>
            {compressedFile ? (
              <Button onClick={handleDownloadAndDelete}>
                <FileDown className="mr-2 h-4 w-4" />
                Download
              </Button>
            ) : (
              <Button onClick={handleCompress} disabled={!file || loading}>
                {loading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Compressing...
                  </>
                ) : (
                  <>
                    <FileUp className="mr-2 h-4 w-4" />
                    Compress PDF
                  </>
                )}
              </Button>
            )}
          </CardFooter>
          <CardHeader>
          <CardDescription>
            Made by Ankit M. Panchal
          </CardDescription>
        </CardHeader>
        </Card>
        <Toaster />
      </div>
    </div>
  )
}
