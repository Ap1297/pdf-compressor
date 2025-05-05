"use client"

import type React from "react"

import { useState, useEffect } from "react"
import { Upload, FileUp, FileDown, Loader2, ImageIcon, FileText } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Slider } from "@/components/ui/slider"
import { toast } from "@/components/ui/use-toast"
import { Toaster } from "@/components/ui/toaster"
import { ThemeToggleWithLabel } from "@/components/theme-toggle-with-label"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"

// Supported file types
const SUPPORTED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/tiff"]

const SUPPORTED_PDF_TYPES = ["application/pdf"]

// Maximum file size in bytes (1GB)
const MAX_FILE_SIZE = 1024 * 1024 * 1024

export default function FileCompressor() {
  const [activeTab, setActiveTab] = useState("pdf")
  const [file, setFile] = useState<File | null>(null)
  const [compressedFile, setCompressedFile] = useState<string | null>(null)
  const [compressedFileName, setCompressedFileName] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [compressionLevel, setCompressionLevel] = useState([50])
  const [originalSize, setOriginalSize] = useState<number | null>(null)
  const [compressedSize, setCompressedSize] = useState<number | null>(null)
  const [filePreview, setFilePreview] = useState<string | null>(null)

  // Reset state when changing tabs
  useEffect(() => {
    handleClear()
  }, [activeTab])

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0]

      // Check file type based on active tab
      if (activeTab === "pdf" && !SUPPORTED_PDF_TYPES.includes(selectedFile.type)) {
        toast({
          title: "Invalid file type",
          description: "Please select a PDF file",
          variant: "destructive",
        })
        return
      } else if (activeTab === "image" && !SUPPORTED_IMAGE_TYPES.includes(selectedFile.type)) {
        toast({
          title: "Invalid file type",
          description: "Please select an image file (JPEG, PNG, GIF, BMP, WebP, TIFF)",
          variant: "destructive",
        })
        return
      }

      // Check file size
      if (selectedFile.size > MAX_FILE_SIZE) {
        toast({
          title: "File too large",
          description: `The maximum file size is ${formatFileSize(MAX_FILE_SIZE)}`,
          variant: "destructive",
        })
        return
      }

      setFile(selectedFile)
      setOriginalSize(selectedFile.size)
      setCompressedFile(null)
      setCompressedFileName(null)
      setCompressedSize(null)

      // Create preview for images
      if (activeTab === "image" && selectedFile.type.startsWith("image/")) {
        const reader = new FileReader()
        reader.onload = (e) => {
          setFilePreview(e.target?.result as string)
        }
        reader.readAsDataURL(selectedFile)
      } else {
        setFilePreview(null)
      }
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

      // Use different endpoints based on file type
      const endpoint = activeTab === "pdf" ? "/api/compress" : "/api/image/compress"

      const response = await fetch(endpoint, {
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
        description: error instanceof Error ? error.message : "An error occurred while compressing the file",
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
        // Use different endpoints based on file type
        const endpoint = activeTab === "pdf" ? "/api/delete" : "/api/image/delete"

        // Delete the file
        const response = await fetch(`${endpoint}?file=${compressedFileName}`, {
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
      const endpoint = activeTab === "pdf" ? "/api/delete" : "/api/image/delete"
      fetch(`${endpoint}?file=${compressedFileName}`, {
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
    setFilePreview(null)
  }

  return (
    <div className="min-h-screen bg-background transition-colors duration-300">
      <div className="container mx-auto py-10">
        <div className="flex justify-end mb-4">
          <ThemeToggleWithLabel />
        </div>

        <Tabs defaultValue="pdf" value={activeTab} onValueChange={setActiveTab} className="max-w-2xl mx-auto">
          <TabsList className="grid w-full grid-cols-2 mb-4">
            <TabsTrigger value="pdf" className="flex items-center gap-2">
              <FileText className="h-4 w-4" />
              PDF Compression
            </TabsTrigger>
            <TabsTrigger value="image" className="flex items-center gap-2">
              <ImageIcon className="h-4 w-4" />
              Image Compression
            </TabsTrigger>
          </TabsList>

          <Card>
            <CardHeader>
              <CardTitle className="text-2xl flex items-center gap-2">
                {activeTab === "pdf" ? (
                  <>
                    <FileText className="h-6 w-6" />
                    PDF Compressor
                  </>
                ) : (
                  <>
                    <ImageIcon className="h-6 w-6" />
                    Image Compressor
                  </>
                )}
              </CardTitle>
              <CardDescription>
                {activeTab === "pdf"
                  ? "Upload a PDF file and compress it to reduce file size while maintaining quality"
                  : "Upload an image and compress it to reduce file size while maintaining quality"}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="border-2 border-dashed border-muted rounded-lg p-6 text-center">
                <input
                  type="file"
                  id="file-upload"
                  className="hidden"
                  onChange={handleFileChange}
                  accept={activeTab === "pdf" ? ".pdf" : ".jpg,.jpeg,.png,.gif,.bmp,.webp,.tiff,.tif"}
                />
                <label htmlFor="file-upload" className="flex flex-col items-center justify-center cursor-pointer">
                  <Upload className="h-10 w-10 text-muted-foreground mb-2" />
                  <span className="text-sm font-medium">{file ? file.name : "Click to upload or drag and drop"}</span>
                  <span className="text-xs text-muted-foreground mt-1">
                    {activeTab === "pdf" ? "PDF (max. 1GB)" : "JPEG, PNG, GIF, BMP, WebP, TIFF (max. 1GB)"}
                  </span>
                </label>
              </div>

              {filePreview && (
                <div className="flex justify-center">
                  <div className="relative w-full max-w-xs h-48 border rounded-md overflow-hidden">
                    <img
                      src={filePreview || "/placeholder.svg"}
                      alt="Preview"
                      className="w-full h-full object-contain"
                    />
                  </div>
                </div>
              )}

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
                      Compress {activeTab === "pdf" ? "PDF" : "Image"}
                    </>
                  )}
                </Button>
              )}
            </CardFooter>
          </Card>
        </Tabs>
        <Toaster />
      </div>
    </div>
  )
}
