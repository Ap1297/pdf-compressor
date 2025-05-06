"use client"

import type React from "react"

import { useState, useEffect } from "react"
import { Upload, FileUp, FileDown, Loader2, ImageIcon, FileText, FileType, ArrowRight } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Slider } from "@/components/ui/slider"
import { toast } from "@/components/ui/use-toast"
import { Toaster } from "@/components/ui/toaster"
import { ThemeToggleWithLabel } from "@/components/theme-toggle-with-label"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Label } from "@/components/ui/label"

// Supported file types
const SUPPORTED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/tiff"]
const SUPPORTED_PDF_TYPES = ["application/pdf"]
const SUPPORTED_WORD_TYPES = [
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/msword",
]

// Maximum file size in bytes (1GB)
const MAX_FILE_SIZE = 1024 * 1024 * 1024

export default function FileProcessor() {
  const [activeTab, setActiveTab] = useState("pdf")
  const [file, setFile] = useState<File | null>(null)
  const [processedFile, setProcessedFile] = useState<string | null>(null)
  const [processedFileName, setProcessedFileName] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [compressionLevel, setCompressionLevel] = useState([50])
  const [originalSize, setOriginalSize] = useState<number | null>(null)
  const [compressedSize, setCompressedSize] = useState<number | null>(null)
  const [filePreview, setFilePreview] = useState<string | null>(null)
  const [conversionType, setConversionType] = useState<string>("compress")
  const [conversionDirection, setConversionDirection] = useState<string>("pdf-to-word")

  // Reset state when changing tabs
  useEffect(() => {
    handleClear()

    // Set default conversion type based on active tab
    if (activeTab === "convert") {
      setConversionType("convert")
      setConversionDirection("pdf-to-word")
    } else {
      setConversionType("compress")
    }
  }, [activeTab])

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0]

      // Check file type based on active tab and conversion type
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
      } else if (activeTab === "convert") {
        if (conversionDirection === "pdf-to-word" && !SUPPORTED_PDF_TYPES.includes(selectedFile.type)) {
          toast({
            title: "Invalid file type",
            description: "Please select a PDF file for PDF to Word conversion",
            variant: "destructive",
          })
          return
        } else if (conversionDirection === "word-to-pdf" && !SUPPORTED_WORD_TYPES.includes(selectedFile.type)) {
          toast({
            title: "Invalid file type",
            description: "Please select a Word document for Word to PDF conversion",
            variant: "destructive",
          })
          return
        }
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
      setProcessedFile(null)
      setProcessedFileName(null)
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

  const handleProcess = async () => {
    if (!file) return

    setLoading(true)
    setProgress(0)

    const formData = new FormData()
    formData.append("file", file)

    // Add compression level for compression operations
    if (activeTab !== "convert") {
      formData.append("compressionLevel", compressionLevel[0].toString())
    }

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

      // Determine the endpoint based on the active tab and operation
      let endpoint = ""
      if (activeTab === "pdf") {
        endpoint = "/api/compress"
      } else if (activeTab === "image") {
        endpoint = "/api/image/compress"
      } else if (activeTab === "convert") {
        endpoint = `/api/convert/${conversionDirection}`
      }

      const response = await fetch(endpoint, {
        method: "POST",
        body: formData,
      })

      clearInterval(progressInterval)
      setProgress(100)

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || "Operation failed")
      }

      const data = await response.json()
      setProcessedFile(data.downloadUrl)

      // Extract the filename from the download URL
      const fileName = data.downloadUrl.split("=")[1]
      setProcessedFileName(fileName)

      // For compression operations, set size information
      if (activeTab !== "convert") {
        setOriginalSize(data.originalSize)
        setCompressedSize(data.compressedSize)

        toast({
          title: "Compression complete",
          description: `Reduced from ${formatFileSize(data.originalSize)} to ${formatFileSize(data.compressedSize)}`,
        })
      } else {
        // For conversion operations
        toast({
          title: "Conversion complete",
          description: `Successfully converted from ${data.sourceFormat} to ${data.targetFormat}`,
        })
      }
    } catch (error) {
      toast({
        title: activeTab === "convert" ? "Conversion failed" : "Compression failed",
        description: error instanceof Error ? error.message : "An error occurred",
        variant: "destructive",
      })
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  const handleDownloadAndDelete = async () => {
    if (!processedFile || !processedFileName) return

    // Start the download
    window.location.href = processedFile

    // Wait a moment to ensure the download has started
    setTimeout(async () => {
      try {
        // Use different endpoints based on file type
        let endpoint = ""
        if (activeTab === "pdf") {
          endpoint = "/api/delete"
        } else if (activeTab === "image") {
          endpoint = "/api/image/delete"
        } else if (activeTab === "convert") {
          endpoint = "/api/convert/delete"
        }

        // Delete the file
        const response = await fetch(`${endpoint}?file=${processedFileName}`, {
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
    // If we have a processed file, delete it before clearing the state
    if (processedFileName) {
      let endpoint = ""
      if (activeTab === "pdf") {
        endpoint = "/api/delete"
      } else if (activeTab === "image") {
        endpoint = "/api/image/delete"
      } else if (activeTab === "convert") {
        endpoint = "/api/convert/delete"
      }

      fetch(`${endpoint}?file=${processedFileName}`, {
        method: "DELETE",
      }).catch((error) => {
        console.error("Error deleting file during clear:", error)
      })
    }

    setFile(null)
    setProcessedFile(null)
    setProcessedFileName(null)
    setOriginalSize(null)
    setCompressedSize(null)
    setFilePreview(null)
  }

  const getButtonText = () => {
    if (loading) {
      return (
        <>
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          {activeTab === "convert" ? "Converting..." : "Compressing..."}
        </>
      )
    }

    if (activeTab === "convert") {
      return (
        <>
          <FileType className="mr-2 h-4 w-4" />
          Convert {conversionDirection === "pdf-to-word" ? "PDF to Word" : "Word to PDF"}
        </>
      )
    }

    return (
      <>
        <FileUp className="mr-2 h-4 w-4" />
        Compress {activeTab === "pdf" ? "PDF" : "Image"}
      </>
    )
  }

  const getAcceptedFileTypes = () => {
    if (activeTab === "pdf") {
      return ".pdf"
    } else if (activeTab === "image") {
      return ".jpg,.jpeg,.png,.gif,.bmp,.webp,.tiff,.tif"
    } else if (activeTab === "convert") {
      return conversionDirection === "pdf-to-word" ? ".pdf" : ".docx,.doc"
    }
    return ""
  }

  const getFileTypeDescription = () => {
    if (activeTab === "pdf") {
      return "PDF (max. 1GB)"
    } else if (activeTab === "image") {
      return "JPEG, PNG, GIF, BMP, WebP, TIFF (max. 1GB)"
    } else if (activeTab === "convert") {
      return conversionDirection === "pdf-to-word" ? "PDF (max. 1GB)" : "DOCX, DOC (max. 1GB)"
    }
    return ""
  }

  return (
    <div className="min-h-screen bg-background transition-colors duration-300">
      <div className="container mx-auto px-4 py-6 md:py-10">
        <div className="flex justify-end mb-4">
          <ThemeToggleWithLabel />
        </div>

        <Tabs defaultValue="pdf" value={activeTab} onValueChange={setActiveTab} className="max-w-2xl mx-auto">
          <TabsList className="w-full mb-4 flex flex-wrap overflow-x-auto">
            <TabsTrigger
              value="pdf"
              className="flex-1 min-w-[33%] flex items-center justify-center gap-1 text-xs sm:text-sm py-2"
            >
              <FileText className="h-3 w-3 sm:h-4 sm:w-4" />
              <span className="hidden xs:inline sm:inline">PDF Compression</span>
              <span className="inline xs:hidden sm:hidden">PDF</span>
            </TabsTrigger>
            <TabsTrigger
              value="image"
              className="flex-1 min-w-[33%] flex items-center justify-center gap-1 text-xs sm:text-sm py-2"
            >
              <ImageIcon className="h-3 w-3 sm:h-4 sm:w-4" />
              <span className="hidden xs:inline sm:inline">Image Compression</span>
              <span className="inline xs:hidden sm:hidden">Image</span>
            </TabsTrigger>
            <TabsTrigger
              value="convert"
              className="flex-1 min-w-[33%] flex items-center justify-center gap-1 text-xs sm:text-sm py-2"
            >
              <FileType className="h-3 w-3 sm:h-4 sm:w-4" />
              <span className="hidden xs:inline sm:inline">Document Conversion</span>
              <span className="inline xs:hidden sm:hidden">Convert</span>
            </TabsTrigger>
          </TabsList>

          <Card>
            <CardHeader className="p-4 sm:p-6">
              <CardTitle className="text-xl sm:text-2xl flex items-center gap-2">
                {activeTab === "pdf" ? (
                  <>
                    <FileText className="h-5 w-5 sm:h-6 sm:w-6" />
                    PDF Compressor
                  </>
                ) : activeTab === "image" ? (
                  <>
                    <ImageIcon className="h-5 w-5 sm:h-6 sm:w-6" />
                    Image Compressor
                  </>
                ) : (
                  <>
                    <FileType className="h-5 w-5 sm:h-6 sm:w-6" />
                    Document Converter
                  </>
                )}
              </CardTitle>
              <CardDescription className="text-xs sm:text-sm">
                {activeTab === "pdf"
                  ? "Upload a PDF file and compress it to reduce file size while maintaining quality"
                  : activeTab === "image"
                    ? "Upload an image and compress it to reduce file size while maintaining quality"
                    : "Convert between PDF and Word document formats"}
              </CardDescription>
            </CardHeader>
            <CardContent className="p-4 sm:p-6 space-y-4 sm:space-y-6">
              {activeTab === "convert" && (
                <div className="space-y-3 sm:space-y-4">
                  <div className="text-sm font-medium">Conversion Type</div>
                  <RadioGroup
                    value={conversionDirection}
                    onValueChange={setConversionDirection}
                    className="flex flex-col space-y-2"
                  >
                    <div className="flex items-center space-x-2">
                      <RadioGroupItem value="pdf-to-word" id="pdf-to-word" />
                      <Label htmlFor="pdf-to-word" className="flex items-center gap-1 sm:gap-2 text-sm cursor-pointer">
                        <FileText className="h-4 w-4" />
                        <ArrowRight className="h-3 w-3" />
                        <FileType className="h-4 w-4" />
                        <span>PDF to Word</span>
                      </Label>
                    </div>
                    <div className="flex items-center space-x-2">
                      <RadioGroupItem value="word-to-pdf" id="word-to-pdf" />
                      <Label htmlFor="word-to-pdf" className="flex items-center gap-1 sm:gap-2 text-sm cursor-pointer">
                        <FileType className="h-4 w-4" />
                        <ArrowRight className="h-3 w-3" />
                        <FileText className="h-4 w-4" />
                        <span>Word to PDF</span>
                      </Label>
                    </div>
                  </RadioGroup>
                </div>
              )}

              <div className="border-2 border-dashed border-muted rounded-lg p-4 sm:p-6 text-center">
                <input
                  type="file"
                  id="file-upload"
                  className="hidden"
                  onChange={handleFileChange}
                  accept={getAcceptedFileTypes()}
                />
                <label htmlFor="file-upload" className="flex flex-col items-center justify-center cursor-pointer">
                  <Upload className="h-8 w-8 sm:h-10 sm:w-10 text-muted-foreground mb-2" />
                  <span className="text-sm font-medium break-words max-w-full px-2">
                    {file ? file.name : "Click to upload or drag and drop"}
                  </span>
                  <span className="text-xs text-muted-foreground mt-1">{getFileTypeDescription()}</span>
                </label>
              </div>

              {filePreview && (
                <div className="flex justify-center">
                  <div className="relative w-full max-w-xs h-36 sm:h-48 border rounded-md overflow-hidden">
                    <img
                      src={filePreview || "/placeholder.svg"}
                      alt="Preview"
                      className="w-full h-full object-contain"
                    />
                  </div>
                </div>
              )}

              {file && activeTab !== "convert" && (
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
              )}

              {loading && (
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm font-medium">
                      {activeTab === "convert" ? "Converting..." : "Compressing..."}
                    </span>
                    <span className="text-sm text-muted-foreground">{progress}%</span>
                  </div>
                  <Progress value={progress} className="h-2" />
                </div>
              )}

              {processedFile && activeTab !== "convert" && (
                <div className="bg-muted/30 p-3 sm:p-4 rounded-lg">
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

              {processedFile && activeTab === "convert" && (
                <div className="bg-muted/30 p-3 sm:p-4 rounded-lg">
                  <div className="flex justify-between">
                    <span className="text-sm font-medium">Conversion:</span>
                    <span className="text-sm">
                      {conversionDirection === "pdf-to-word" ? "PDF → Word" : "Word → PDF"}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm font-medium">Status:</span>
                    <span className="text-sm text-green-600 dark:text-green-400">Ready for download</span>
                  </div>
                </div>
              )}
            </CardContent>
            <CardFooter className="p-4 sm:p-6 flex flex-col sm:flex-row gap-2 sm:gap-4">
              <Button
                variant="outline"
                onClick={handleClear}
                disabled={!file || loading}
                className="w-full sm:w-auto order-2 sm:order-1"
              >
                Clear
              </Button>
              <div className="hidden sm:block sm:grow"></div>
              {processedFile ? (
                <Button onClick={handleDownloadAndDelete} className="w-full sm:w-auto order-1 sm:order-2">
                  <FileDown className="mr-2 h-4 w-4" />
                  Download
                </Button>
              ) : (
                <Button
                  onClick={handleProcess}
                  disabled={!file || loading}
                  className="w-full sm:w-auto order-1 sm:order-2"
                >
                  {getButtonText()}
                </Button>
              )}
            </CardFooter>
            <CardHeader className="p-4 sm:p-6 pt-0 sm:pt-0 border-t">
              <CardDescription className="text-center text-xs sm:text-sm">Made By Ankit Panchal</CardDescription>
            </CardHeader>
          </Card>
        </Tabs>
        <Toaster />
      </div>
    </div>
  )
}
