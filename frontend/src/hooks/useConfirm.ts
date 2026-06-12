"use client"

import { useState, useCallback } from "react"

interface ConfirmOptions {
  title: string
  description: string
  confirmText?: string
  cancelText?: string
  variant?: "default" | "destructive"
  onConfirm: () => void | Promise<void>
}

export function useConfirm() {
  const [isOpen, setIsOpen] = useState(false)
  const [options, setOptions] = useState<ConfirmOptions | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const confirm = useCallback((newOptions: ConfirmOptions) => {
    setOptions(newOptions)
    setIsOpen(true)
  }, [])

  const close = useCallback(() => {
    setIsOpen(false)
    // Wait for animation to finish before clearing options
    setTimeout(() => {
      setOptions(null)
      setIsLoading(false)
    }, 200)
  }, [])

  const execute = useCallback(async () => {
    if (!options) return
    try {
      setIsLoading(true)
      await options.onConfirm()
      close()
    } catch (error) {
      console.error("Confirm action failed:", error)
      setIsLoading(false)
    }
  }, [options, close])

  return {
    isOpen,
    isLoading,
    options,
    confirm,
    close,
    execute
  }
}
