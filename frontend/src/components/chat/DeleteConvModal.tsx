import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"

interface DeleteConvModalProps {
  isOpen: boolean
  onClose: () => void
  onConfirm: () => void
}

export function DeleteConvModal({ isOpen, onClose, onConfirm }: DeleteConvModalProps) {
  return (
    <AlertDialog open={isOpen} onOpenChange={onClose}>
      <AlertDialogContent className="max-w-md bg-card text-foreground">
        <AlertDialogHeader>
          <AlertDialogTitle>Xóa đoạn chat</AlertDialogTitle>
          <AlertDialogDescription>
            Bạn không thể hoàn tác sau khi xóa bản sao của cuộc trò chuyện này.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter className="mt-4">
          <AlertDialogCancel onClick={onClose}>Hủy</AlertDialogCancel>
          <AlertDialogAction onClick={onConfirm} className="bg-blue-600 hover:bg-blue-700 text-white">
            Xóa đoạn chat
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
