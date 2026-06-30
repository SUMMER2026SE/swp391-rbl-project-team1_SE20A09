import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Label } from "@/components/ui/label"
import { useState, useEffect } from "react"
import type { ChatMessageDto } from "@/lib/chat-api"

interface RecallModalProps {
  message: ChatMessageDto | null
  isOpen: boolean
  isMe: boolean
  onClose: () => void
  onRecall: (messageId: number, type: "everyone" | "me") => void
}

export function RecallModal({ message, isOpen, isMe, onClose, onRecall }: RecallModalProps) {
  const [recallType, setRecallType] = useState<"everyone" | "me">("everyone")

  useEffect(() => {
    if (isOpen) {
      setRecallType(isMe ? "everyone" : "me")
    }
  }, [isOpen, isMe])

  if (!message) return null

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-lg bg-background text-foreground border-border shadow-2xl">
        <DialogHeader className="border-b border-border pb-4">
          <DialogTitle className="text-center text-xl font-bold">
            Bạn muốn thu hồi tin nhắn này ở phía ai?
          </DialogTitle>
          <DialogDescription className="hidden">
            Lựa chọn phương thức thu hồi tin nhắn
          </DialogDescription>
        </DialogHeader>
        
        <div className="py-4">
          <RadioGroup 
            value={recallType} 
            onValueChange={(val) => setRecallType(val as "everyone" | "me")}
            className="space-y-4"
          >
            {isMe && (
              <div className="flex items-start space-x-3 hover:bg-muted p-2 rounded-lg transition-colors cursor-pointer" onClick={() => setRecallType("everyone")}>
                <RadioGroupItem value="everyone" id="everyone" className="mt-1 text-primary" />
                <div className="grid gap-1">
                  <Label htmlFor="everyone" className="font-semibold text-base cursor-pointer">
                    Thu hồi với mọi người
                  </Label>
                  <p className="text-sm text-muted-foreground">
                    Tin nhắn này sẽ bị thu hồi với mọi người trong đoạn chat. Những người khác có thể đã xem hoặc chuyển tiếp tin nhắn đó. Tin nhắn đã thu hồi vẫn có thể bị báo cáo.
                  </p>
                </div>
              </div>
            )}
            
            <div className="flex items-start space-x-3 hover:bg-muted p-2 rounded-lg transition-colors cursor-pointer" onClick={() => setRecallType("me")}>
              <RadioGroupItem value="me" id="me" className="mt-1 text-primary" />
              <div className="grid gap-1">
                <Label htmlFor="me" className="font-semibold text-base cursor-pointer">
                  Thu hồi với bạn
                </Label>
                <p className="text-sm text-muted-foreground">
                  Tin nhắn này sẽ bị gỡ khỏi thiết bị của bạn, nhưng vẫn hiển thị với các thành viên khác trong đoạn chat.
                </p>
              </div>
            </div>
          </RadioGroup>
        </div>

        <DialogFooter className="flex gap-2 sm:justify-end border-t border-border pt-4">
          <Button 
            variant="ghost" 
            onClick={onClose}
            className="font-semibold"
          >
            Hủy
          </Button>
          <Button 
            onClick={() => onRecall(message.messageId, recallType)}
            className="bg-primary hover:bg-primary/90 text-primary-foreground font-semibold px-6"
          >
            Gỡ
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
