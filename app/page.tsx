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

export default function PDFCompressor() {
  const [file, setFile] = useState<File | null>(null)
  const [compressedFile, setCompressedFile] = useState<string | null>(null)
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
      setFile(selectedFile)
      setOriginalSize(selectedFile.size)
      setCompressedFile(null)
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
        throw new Error("Compression failed")
      }

      const data = await response.json()
      setCompressedFile(data.downloadUrl)
      setCompressedSize(data.compressedSize)

      toast({
        title: "Compression complete",
        description: `Reduced from ${formatFileSize(originalSize!)} to ${formatFileSize(data.compressedSize)}`,
      })
    } catch (error) {
      toast({
        title: "Compression failed",
        description: "An error occurred while compressing the PDF",
        variant: "destructive",
      })
      console.error(error)
    } finally {
      setLoading(false)
    }
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

  return (
    <div className="container mx-auto py-10">
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
          <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center">
            <input
              type="file"
              id="pdf-upload"
              className="hidden"
              onChange={handleFileChange}
              accept="application/pdf"
            />
            <label htmlFor="pdf-upload" className="flex flex-col items-center justify-center cursor-pointer">
              <Upload className="h-10 w-10 text-gray-400 mb-2" />
              <span className="text-sm font-medium">{file ? file.name : "Click to upload or drag and drop"}</span>
              <span className="text-xs text-gray-500 mt-1">PDF (max. 10MB)</span>
            </label>
          </div>

          {file && (
            <>
              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-sm font-medium">Compression Level</span>
                  <span className="text-sm text-gray-500">{compressionLevel[0]}%</span>
                </div>
                <Slider
                  value={compressionLevel}
                  onValueChange={setCompressionLevel}
                  min={10}
                  max={90}
                  step={10}
                  disabled={loading}
                />
                <div className="flex justify-between text-xs text-gray-500">
                  <span>Higher Quality</span>
                  <span>Smaller Size</span>
                </div>
              </div>

              {loading && (
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm font-medium">Compressing...</span>
                    <span className="text-sm text-gray-500">{progress}%</span>
                  </div>
                  <Progress value={progress} className="h-2" />
                </div>
              )}

              {compressedFile && (
                <div className="bg-gray-50 p-4 rounded-lg">
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
                    <span className="text-sm text-green-600">{calculateReduction()}%</span>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
        <CardFooter className="flex justify-between">
          <Button
            variant="outline"
            onClick={() => {
              setFile(null)
              setCompressedFile(null)
              setOriginalSize(null)
              setCompressedSize(null)
            }}
            disabled={!file || loading}
          >
            Clear
          </Button>
          {compressedFile ? (
            <Button asChild>
              <a href={compressedFile} download>
                <FileDown className="mr-2 h-4 w-4" />
                Download
              </a>
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
      </Card>
      <Toaster />
    </div>
  )
}
