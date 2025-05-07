"use client"

import type React from "react"

import { useState, useEffect } from "react"
import {
  Upload,
  FileUp,
  FileDown,
  Loader2,
  ImageIcon,
  FileText,
  FileType,
  ArrowRight,
  Eraser,
  Video,
  CheckCircle,
  Clock,
  Zap,
  Lock,
} from "lucide-react"
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
import { Logo } from "@/components/logo"

// Supported file types
const SUPPORTED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/tiff"]
const SUPPORTED_PDF_TYPES = ["application/pdf"]
const SUPPORTED_WORD_TYPES = [
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/msword",
]
const SUPPORTED_VIDEO_TYPES = [
  "video/mp4",
  "video/x-msvideo",
  "video/quicktime",
  "video/x-ms-wmv",
  "video/x-flv",
  "video/x-matroska",
  "video/webm",
]

// Combined media types
const SUPPORTED_MEDIA_TYPES = [...SUPPORTED_IMAGE_TYPES, ...SUPPORTED_VIDEO_TYPES]

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
  const [watermarkThreshold, setWatermarkThreshold] = useState([200])
  const [watermarkTolerance, setWatermarkTolerance] = useState([30])
  const [originalSize, setOriginalSize] = useState<number | null>(null)
  const [compressedSize, setCompressedSize] = useState<number | null>(null)
  const [filePreview, setFilePreview] = useState<string | null>(null)
  const [conversionType, setConversionType] = useState<string>("compress")
  const [conversionDirection, setConversionDirection] = useState<string>("pdf-to-word")
  const [watermarkFileType, setWatermarkFileType] = useState<string>("image")
  const [mediaType, setMediaType] = useState<string>("image")

  // Reset state when changing tabs
  useEffect(() => {
    handleClear()

    // Set default values based on active tab
    if (activeTab === "convert") {
      setConversionType("convert")
      setConversionDirection("pdf-to-word")
    } else if (activeTab === "watermark") {
      setWatermarkFileType("image")
    } else if (activeTab === "media") {
      setMediaType("image")
    } else {
      setConversionType("compress")
    }
  }, [activeTab])

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0]

      // Check file type based on active tab and operation type
      if (activeTab === "pdf" && !SUPPORTED_PDF_TYPES.includes(selectedFile.type)) {
        toast({
          title: "Invalid file type",
          description: "Please select a PDF file",
          variant: "destructive",
        })
        return
      } else if (activeTab === "media") {
        if (!SUPPORTED_MEDIA_TYPES.includes(selectedFile.type)) {
          toast({
            title: "Invalid file type",
            description: "Please select an image or video file",
            variant: "destructive",
          })
          return
        }
        // Set the media type based on the file type
        if (SUPPORTED_IMAGE_TYPES.includes(selectedFile.type)) {
          setMediaType("image")
        } else if (SUPPORTED_VIDEO_TYPES.includes(selectedFile.type)) {
          setMediaType("video")
        }
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
      } else if (activeTab === "watermark") {
        if (watermarkFileType === "image" && !SUPPORTED_IMAGE_TYPES.includes(selectedFile.type)) {
          toast({
            title: "Invalid file type",
            description: "Please select an image file for watermark removal",
            variant: "destructive",
          })
          return
        } else if (watermarkFileType === "pdf" && !SUPPORTED_PDF_TYPES.includes(selectedFile.type)) {
          toast({
            title: "Invalid file type",
            description: "Please select a PDF file for watermark removal",
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

      // Clean up previous preview if it exists
      if (filePreview) {
        URL.revokeObjectURL(filePreview)
      }

      // Create preview based on file type
      if (
        (activeTab === "media" && selectedFile.type.startsWith("image/")) ||
        (activeTab === "watermark" && watermarkFileType === "image" && selectedFile.type.startsWith("image/"))
      ) {
        const reader = new FileReader()
        reader.onload = (e) => {
          setFilePreview(e.target?.result as string)
        }
        reader.readAsDataURL(selectedFile)
      } else if (activeTab === "media" && selectedFile.type.startsWith("video/")) {
        // Create a video preview URL
        setFilePreview(URL.createObjectURL(selectedFile))
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

    // Add parameters based on operation type
    if (activeTab === "pdf" || activeTab === "media") {
      formData.append("compressionLevel", compressionLevel[0].toString())
    } else if (activeTab === "watermark") {
      formData.append("threshold", watermarkThreshold[0].toString())
      formData.append("tolerance", watermarkTolerance[0].toString())
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
      } else if (activeTab === "media") {
        // Choose the appropriate endpoint based on the media type
        if (mediaType === "image") {
          endpoint = "/api/image/compress"
        } else if (mediaType === "video") {
          endpoint = "/api/video" // Updated endpoint
        }
      } else if (activeTab === "convert") {
        endpoint = `/api/convert/${conversionDirection}`
      } else if (activeTab === "watermark") {
        endpoint = "/api/watermark"
        // Add fileType to formData for watermark removal
        formData.append("fileType", watermarkFileType)
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
      if (activeTab === "pdf" || activeTab === "media") {
        setOriginalSize(data.originalSize)
        setCompressedSize(data.compressedSize)

        toast({
          title: "Compression complete",
          description: `Reduced from ${formatFileSize(data.originalSize)} to ${formatFileSize(data.compressedSize)}`,
        })
      } else if (activeTab === "convert") {
        // For conversion operations
        toast({
          title: "Conversion complete",
          description: `Successfully converted from ${data.sourceFormat} to ${data.targetFormat}`,
        })
      } else if (activeTab === "watermark") {
        // For watermark removal operations
        toast({
          title: "Watermark removal complete",
          description: `Successfully removed watermark from ${watermarkFileType === "image" ? "image" : "PDF"}`,
        })
      }
    } catch (error) {
      let errorTitle = "Operation failed"
      if (activeTab === "pdf" || activeTab === "media") {
        errorTitle = "Compression failed"
      } else if (activeTab === "convert") {
        errorTitle = "Conversion failed"
      } else if (activeTab === "watermark") {
        errorTitle = "Watermark removal failed"
      }

      toast({
        title: errorTitle,
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
        } else if (activeTab === "media") {
          if (mediaType === "image") {
            endpoint = "/api/image/delete"
          } else if (mediaType === "video") {
            endpoint = "/api/video" // Updated endpoint
          }
        } else if (activeTab === "convert") {
          endpoint = "/api/convert/delete"
        } else if (activeTab === "watermark") {
          endpoint = "/api/watermark/delete"
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
      } else if (activeTab === "media") {
        if (mediaType === "image") {
          endpoint = "/api/image/delete"
        } else if (mediaType === "video") {
          endpoint = "/api/video"
        }
      } else if (activeTab === "convert") {
        endpoint = "/api/convert/delete"
      } else if (activeTab === "watermark") {
        endpoint = "/api/watermark/delete"
      }

      fetch(`${endpoint}?file=${processedFileName}`, {
        method: "DELETE",
      }).catch((error) => {
        console.error("Error deleting file during clear:", error)
      })
    }

    // Clean up any object URLs
    if (filePreview && activeTab === "media" && mediaType === "video") {
      URL.revokeObjectURL(filePreview)
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
          {activeTab === "convert"
            ? "Converting..."
            : activeTab === "watermark"
              ? "Removing watermark..."
              : "Compressing..."}
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
    } else if (activeTab === "watermark") {
      return (
        <>
          <Eraser className="mr-2 h-4 w-4" />
          Remove Watermark
        </>
      )
    }

    return (
      <>
        <FileUp className="mr-2 h-4 w-4" />
        Compress {activeTab === "pdf" ? "PDF" : mediaType === "image" ? "Image" : "Video"}
      </>
    )
  }

  const getAcceptedFileTypes = () => {
    if (activeTab === "pdf") {
      return ".pdf"
    } else if (activeTab === "media") {
      return ".jpg,.jpeg,.png,.gif,.bmp,.webp,.tiff,.tif,.mp4,.avi,.mov,.wmv,.flv,.mkv,.webm"
    } else if (activeTab === "convert") {
      return conversionDirection === "pdf-to-word" ? ".pdf" : ".docx,.doc"
    } else if (activeTab === "watermark") {
      return watermarkFileType === "image" ? ".jpg,.jpeg,.png,.gif,.bmp,.webp,.tiff,.tif" : ".pdf"
    }
    return ""
  }

  const getFileTypeDescription = () => {
    if (activeTab === "pdf") {
      return "PDF (max. 1GB)"
    } else if (activeTab === "media") {
      return "Images (JPEG, PNG, GIF, etc.) or Videos (MP4, AVI, etc.) (max. 1GB)"
    } else if (activeTab === "convert") {
      return conversionDirection === "pdf-to-word" ? "PDF (max. 1GB)" : "DOCX, DOC (max. 1GB)"
    } else if (activeTab === "watermark") {
      return watermarkFileType === "image" ? "JPEG, PNG, GIF, BMP, WebP, TIFF (max. 1GB)" : "PDF (max. 1GB)"
    }
    return ""
  }

  const getMediaIcon = () => {
    if (!file) return <Upload className="h-8 w-8 sm:h-10 sm:w-10 text-muted-foreground mb-2" />

    if (mediaType === "image") {
      return <ImageIcon className="h-8 w-8 sm:h-10 sm:w-10 text-muted-foreground mb-2" />
    } else if (mediaType === "video") {
      return <Video className="h-8 w-8 sm:h-10 sm:w-10 text-muted-foreground mb-2" />
    }

    return <Upload className="h-8 w-8 sm:h-10 sm:w-10 text-muted-foreground mb-2" />
  }

  return (
    <div className="min-h-screen bg-background transition-colors duration-300">
      <div className="container mx-auto px-4 py-6 md:py-10">
        {/* Header with Logo and Description */}
        <header className="mb-8">
          <div className="flex flex-col md:flex-row justify-between items-center gap-4 mb-6">
            <Logo size="lg" />
            <ThemeToggleWithLabel />
          </div>

          <div className="bg-muted/40 rounded-lg p-4 md:p-6 mb-8">
            <h1 className="text-2xl md:text-3xl font-bold mb-3">All-in-One File Processing Tool</h1>
            <p className="text-muted-foreground mb-4">
              Compress, convert, and optimize your files with our powerful suite of tools. Reduce file sizes while
              maintaining quality, convert between formats, and remove watermarks - all in one place.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 mt-4">
              <div className="flex items-start gap-2">
                <CheckCircle className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                <div>
                  <h3 className="font-medium">High Quality</h3>
                  <p className="text-sm text-muted-foreground">Maintain quality while reducing file size</p>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Lock className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                <div>
                  <h3 className="font-medium">Secure Processing</h3>
                  <p className="text-sm text-muted-foreground">
                    Files are processed locally and deleted after download
                  </p>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Zap className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                <div>
                  <h3 className="font-medium">Fast & Efficient</h3>
                  <p className="text-sm text-muted-foreground">Optimized algorithms for quick processing</p>
                </div>
              </div>
              <div className="flex items-start gap-2">
                <Clock className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                <div>
                  <h3 className="font-medium">Time-Saving</h3>
                  <p className="text-sm text-muted-foreground">Process multiple file types in one application</p>
                </div>
              </div>
            </div>
          </div>
        </header>

        <Tabs defaultValue="pdf" value={activeTab} onValueChange={setActiveTab} className="max-w-2xl mx-auto">
          <TabsList className="w-full mb-4 flex flex-wrap overflow-x-auto">
            <TabsTrigger
              value="pdf"
              className="flex-1 min-w-[25%] flex items-center justify-center gap-1 text-xs sm:text-sm py-2"
            >
              <FileText className="h-3 w-3 sm:h-4 sm:w-4" />
              <span className="hidden xs:inline sm:inline">PDF</span>
              <span className="inline xs:hidden sm:hidden">PDF</span>
            </TabsTrigger>
            <TabsTrigger
              value="media"
              className="flex-1 min-w-[25%] flex items-center justify-center gap-1 text-xs sm:text-sm py-2"
            >
              <ImageIcon className="h-3 w-3 sm:h-4 sm:w-4" />
              <span className="hidden xs:inline sm:inline">Media</span>
              <span className="inline xs:hidden sm:hidden">MED</span>
            </TabsTrigger>
            <TabsTrigger
              value="convert"
              className="flex-1 min-w-[25%] flex items-center justify-center gap-1 text-xs sm:text-sm py-2"
            >
              <FileType className="h-3 w-3 sm:h-4 sm:w-4" />
              <span className="hidden xs:inline sm:inline">Convert</span>
              <span className="inline xs:hidden sm:hidden">CNV</span>
            </TabsTrigger>
            <TabsTrigger
              value="watermark"
              className="flex-1 min-w-[25%] flex items-center justify-center gap-1 text-xs sm:text-sm py-2"
            >
              <Eraser className="h-3 w-3 sm:h-4 sm:w-4" />
              <span className="hidden xs:inline sm:inline">Watermark</span>
              <span className="inline xs:hidden sm:hidden">WM</span>
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
                ) : activeTab === "media" ? (
                  <>
                    {mediaType === "image" ? (
                      <ImageIcon className="h-5 w-5 sm:h-6 sm:w-6" />
                    ) : (
                      <Video className="h-5 w-5 sm:h-6 sm:w-6" />
                    )}
                    Media Compressor
                  </>
                ) : activeTab === "convert" ? (
                  <>
                    <FileType className="h-5 w-5 sm:h-6 sm:w-6" />
                    Document Converter
                  </>
                ) : (
                  <>
                    <Eraser className="h-5 w-5 sm:h-6 sm:w-6" />
                    Watermark Remover
                  </>
                )}
              </CardTitle>
              <CardDescription className="text-xs sm:text-sm">
                {activeTab === "pdf"
                  ? "Upload a PDF file and compress it to reduce file size while maintaining quality"
                  : activeTab === "media"
                    ? "Upload an image or video and compress it to reduce file size while maintaining quality"
                    : activeTab === "convert"
                      ? "Convert between PDF and Word document formats"
                      : "Remove watermarks from images and PDF documents"}
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

              {activeTab === "watermark" && (
                <div className="space-y-3 sm:space-y-4">
                  <div className="text-sm font-medium">File Type</div>
                  <RadioGroup
                    value={watermarkFileType}
                    onValueChange={setWatermarkFileType}
                    className="flex flex-col space-y-2"
                  >
                    <div className="flex items-center space-x-2">
                      <RadioGroupItem value="image" id="watermark-image" />
                      <Label
                        htmlFor="watermark-image"
                        className="flex items-center gap-1 sm:gap-2 text-sm cursor-pointer"
                      >
                        <ImageIcon className="h-4 w-4" />
                        <span>Image</span>
                      </Label>
                    </div>
                    <div className="flex items-center space-x-2">
                      <RadioGroupItem value="pdf" id="watermark-pdf" />
                      <Label
                        htmlFor="watermark-pdf"
                        className="flex items-center gap-1 sm:gap-2 text-sm cursor-pointer"
                      >
                        <FileText className="h-4 w-4" />
                        <span>PDF Document</span>
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
                  {activeTab === "media" ? (
                    getMediaIcon()
                  ) : (
                    <Upload className="h-8 w-8 sm:h-10 sm:w-10 text-muted-foreground mb-2" />
                  )}
                  <span className="text-sm font-medium break-words max-w-full px-2">
                    {file ? file.name : "Click to upload or drag and drop"}
                  </span>
                  <span className="text-xs text-muted-foreground mt-1">{getFileTypeDescription()}</span>
                </label>
              </div>

              {filePreview && activeTab === "media" && mediaType === "image" && (
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

              {filePreview && activeTab === "media" && mediaType === "video" && (
                <div className="flex justify-center">
                  <div className="relative w-full max-w-xs h-36 sm:h-48 border rounded-md overflow-hidden">
                    <video src={filePreview} controls className="w-full h-full object-contain" />
                  </div>
                </div>
              )}

              {file && (activeTab === "pdf" || activeTab === "media") && (
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

              {file && activeTab === "watermark" && (
                <div className="space-y-4">
                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span className="text-sm font-medium">Watermark Threshold</span>
                      <span className="text-sm text-muted-foreground">{watermarkThreshold[0]}</span>
                    </div>
                    <Slider
                      value={watermarkThreshold}
                      onValueChange={setWatermarkThreshold}
                      min={100}
                      max={250}
                      step={10}
                      disabled={loading}
                    />
                    <div className="flex justify-between text-xs text-muted-foreground">
                      <span>Darker Watermarks</span>
                      <span>Lighter Watermarks</span>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span className="text-sm font-medium">Watermark Tolerance</span>
                      <span className="text-sm text-muted-foreground">{watermarkTolerance[0]}</span>
                    </div>
                    <Slider
                      value={watermarkTolerance}
                      onValueChange={setWatermarkTolerance}
                      min={10}
                      max={50}
                      step={5}
                      disabled={loading}
                    />
                    <div className="flex justify-between text-xs text-muted-foreground">
                      <span>Less Aggressive</span>
                      <span>More Aggressive</span>
                    </div>
                  </div>
                </div>
              )}

              {loading && (
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm font-medium">
                      {activeTab === "convert"
                        ? "Converting..."
                        : activeTab === "watermark"
                          ? "Removing watermark..."
                          : "Compressing..."}
                    </span>
                    <span className="text-sm text-muted-foreground">{progress}%</span>
                  </div>
                  <Progress value={progress} className="h-2" />
                </div>
              )}

              {processedFile && (activeTab === "pdf" || activeTab === "media") && (
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

              {processedFile && activeTab === "watermark" && (
                <div className="bg-muted/30 p-3 sm:p-4 rounded-lg">
                  <div className="flex justify-between">
                    <span className="text-sm font-medium">Process:</span>
                    <span className="text-sm">
                      Watermark Removal ({watermarkFileType === "image" ? "Image" : "PDF"})
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

        {/* Footer with additional information */}
        <footer className="mt-12 text-center text-sm text-muted-foreground">
          <p className="mb-2">FileCompressor - Your all-in-one solution for file optimization</p>
          <p>© 2025 FileCompressor. All rights reserved.</p>
        </footer>

        <Toaster />
      </div>
    </div>
  )
}
