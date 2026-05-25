export type UserProfile = {
  userId: number
  email: string
  firstName: string
  lastName: string
  roleName: string
  avatarUrl?: string | null
  phoneNumber: string
  userRank: string
  userPoint: number
  accountStatus: string
}

export type UpdateProfilePayload = {
  firstName: string
  lastName: string
  phoneNumber: string
  avatarUrl?: string
}
