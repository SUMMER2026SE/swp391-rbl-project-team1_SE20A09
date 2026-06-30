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

interface BlockConfirmationModalProps {
  isOpen: boolean
  onClose: () => void
  onConfirm: () => void
}

export function BlockConfirmationModal({ isOpen, onClose, onConfirm }: BlockConfirmationModalProps) {
  return (
    <AlertDialog open={isOpen} onOpenChange={onClose}>
      <AlertDialogContent className="max-w-md bg-card text-foreground">
        <AlertDialogHeader>
          <AlertDialogTitle>Bạn chắc chắn muốn chặn người này?</AlertDialogTitle>
          <AlertDialogDescription>
            Họ sẽ không nhắn tin được cho bạn nữa.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter className="mt-4">
          <AlertDialogCancel onClick={onClose} className="bg-transparent text-gray-500 hover:text-gray-700">Hủy</AlertDialogCancel>
          <AlertDialogAction onClick={onConfirm} className="bg-red-600 hover:bg-red-700 text-white">
            Chặn
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
