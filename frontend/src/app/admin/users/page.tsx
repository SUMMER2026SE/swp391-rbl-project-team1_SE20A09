"use client";

import { useEffect, useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import AdminCustomersPage from "../customers/page";
import AdminOwnersPage from "../owners/page";
import OwnerApprovalPage from "../owner-approvals/page";

type MainTab = "customers" | "owners";
type OwnerTab = "accounts" | "approvals";

function normalizeMainTab(value: string | null): MainTab {
  return value === "owners" ? "owners" : "customers";
}

function normalizeOwnerTab(value: string | null): OwnerTab {
  return value === "approvals" ? "approvals" : "accounts";
}

export default function AdminUsersPage() {
  const [mainTab, setMainTab] = useState<MainTab>("customers");
  const [ownerTab, setOwnerTab] = useState<OwnerTab>("accounts");

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    setMainTab(normalizeMainTab(searchParams.get("tab")));
    setOwnerTab(normalizeOwnerTab(searchParams.get("ownerTab")));
  }, []);

  return (
    <div className="space-y-6">
      <Tabs value={mainTab} onValueChange={(value) => setMainTab(value as MainTab)}>
        <TabsList className="mb-4">
          <TabsTrigger value="customers">Khách hàng</TabsTrigger>
          <TabsTrigger value="owners">Chủ sân</TabsTrigger>
        </TabsList>

        <TabsContent value="customers" className="mt-0">
          <AdminCustomersPage />
        </TabsContent>

        <TabsContent value="owners" className="mt-0 space-y-4">
          <Tabs value={ownerTab} onValueChange={(value) => setOwnerTab(value as OwnerTab)}>
            <TabsList>
              <TabsTrigger value="accounts">Quản lý tài khoản</TabsTrigger>
              <TabsTrigger value="approvals">Duyệt hồ sơ</TabsTrigger>
            </TabsList>

            <TabsContent value="accounts" className="mt-0">
              <AdminOwnersPage />
            </TabsContent>

            <TabsContent value="approvals" className="mt-0">
              <OwnerApprovalPage />
            </TabsContent>
          </Tabs>
        </TabsContent>
      </Tabs>
    </div>
  );
}
