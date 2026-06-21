import { z } from "zod";

export const registerSchema = z
  .object({
    fullName: z.string().min(2, "Họ và tên phải có ít nhất 2 ký tự"),
    email: z.string().email("Email không hợp lệ"),
    phone: z
      .string()
      .regex(/^(0[3|5|7|8|9])+([0-9]{8})$/, "Số điện thoại không hợp lệ"),
    password: z
      .string()
      .min(8, "Mật khẩu phải có ít nhất 8 ký tự")
      .regex(/[A-Z]/, "Mật khẩu phải chứa ít nhất 1 chữ hoa")
      .regex(/[0-9]/, "Mật khẩu phải chứa ít nhất 1 số")
      .regex(/[^A-Za-z0-9]/, "Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt"),
    confirmPassword: z.string(),
    terms: z.boolean().refine((val) => val === true, {
      message: "Vui lòng đồng ý với điều khoản sử dụng",
    }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Mật khẩu xác nhận không khớp",
    path: ["confirmPassword"],
  });

export type RegisterFormValues = z.infer<typeof registerSchema>;

export const registerOwnerSchema = z
  .object({
    fullName: z.string().min(2, "Họ và tên phải có ít nhất 2 ký tự"),
    email: z.string().email("Email không hợp lệ"),
    phone: z
      .string()
      .regex(/^(0[3|5|7|8|9])+([0-9]{8})$/, "Số điện thoại không hợp lệ"),
    password: z
      .string()
      .min(8, "Mật khẩu phải có ít nhất 8 ký tự")
      .regex(/[A-Z]/, "Mật khẩu phải chứa ít nhất 1 chữ hoa")
      .regex(/[0-9]/, "Mật khẩu phải chứa ít nhất 1 số")
      .regex(/[^A-Za-z0-9]/, "Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt"),
    confirmPassword: z.string(),
    businessName: z.string().min(2, "Tên doanh nghiệp phải có ít nhất 2 ký tự"),
    taxCode: z
      .string()
      .regex(/^(?:\d{10}|\d{13}|\d{10}-\d{3})$/, "Mã số thuế phải gồm 10 hoặc 13 chữ số (ví dụ: 0312456789 hoặc 0312456789-001)"),
    businessAddress: z.string().min(5, "Địa chỉ kinh doanh phải có ít nhất 5 ký tự"),
    businessLicenseUrl: z.string().min(1, "Vui lòng tải lên ảnh Giấy phép kinh doanh"),
    identityCardUrl: z.string().min(1, "Vui lòng tải lên ảnh CCCD/CMND"),
    terms: z.boolean().refine((val) => val === true, {
      message: "Vui lòng đồng ý với điều khoản sử dụng",
    }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Mật khẩu xác nhận không khớp",
    path: ["confirmPassword"],
  });

export type RegisterOwnerFormValues = z.infer<typeof registerOwnerSchema>;

export const upgradeToOwnerSchema = z.object({
  businessName: z.string().min(2, "Tên doanh nghiệp phải có ít nhất 2 ký tự"),
  taxCode: z
    .string()
    .regex(/^(?:\d{10}|\d{13}|\d{10}-\d{3})$/, "Mã số thuế phải gồm 10 hoặc 13 chữ số (ví dụ: 0312456789 hoặc 0312456789-001)"),
  businessAddress: z.string().min(5, "Địa chỉ kinh doanh phải có ít nhất 5 ký tự"),
  businessLicenseUrl: z.string().min(1, "Vui lòng tải lên ảnh Giấy phép kinh doanh"),
  identityCardUrl: z.string().min(1, "Vui lòng tải lên ảnh CCCD/CMND"),
});

export type UpgradeToOwnerFormValues = z.infer<typeof upgradeToOwnerSchema>;

