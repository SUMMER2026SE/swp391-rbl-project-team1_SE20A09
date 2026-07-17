"use client";

import { useEffect, useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import AdminCustomersPage from "../customers/page";
import AdminOwnersPage from "../owners/page";
import AdminOwnerApprovalsClient from "../owner-approvals/components/AdminOwnerApprovalsClient";

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
    const syncFromUrl = () => {
      const searchParams = new URLSearchParams(window.location.search);
      setMainTab(normalizeMainTab(searchParams.get("tab")));
      setOwnerTab(normalizeOwnerTab(searchParams.get("ownerTab")));
    };

    syncFromUrl();
    window.addEventListener("popstate", syncFromUrl);
    return () => window.removeEventListener("popstate", syncFromUrl);
  }, []);

  const updateUrl = (nextMainTab: MainTab, nextOwnerTab: OwnerTab) => {
    const searchParams = new URLSearchParams(window.location.search);

    if (nextMainTab === "owners") {
      searchParams.set("tab", "owners");
      if (nextOwnerTab === "approvals") {
        searchParams.set("ownerTab", "approvals");
      } else {
        searchParams.delete("ownerTab");
      }
    } else {
      searchParams.delete("tab");
      searchParams.delete("ownerTab");
    }

    const query = searchParams.toString();
    const nextUrl = `${window.location.pathname}${query ? `?${query}` : ""}`;
    window.history.replaceState(null, "", nextUrl);
  };

  const handleMainTabChange = (value: string) => {
    const nextMainTab = value as MainTab;
    setMainTab(nextMainTab);
    updateUrl(nextMainTab, ownerTab);
  };

  const handleOwnerTabChange = (value: string) => {
    const nextOwnerTab = value as OwnerTab;
    setOwnerTab(nextOwnerTab);
    updateUrl("owners", nextOwnerTab);
  };

  return (
    <div className="space-y-6">
      <Tabs value={mainTab} onValueChange={handleMainTabChange}>
        <TabsList className="mb-4">
          <TabsTrigger value="customers">Khách hàng</TabsTrigger>
          <TabsTrigger value="owners">Chủ sân</TabsTrigger>
        </TabsList>

        <TabsContent value="customers" className="mt-0">
          <AdminCustomersPage />
        </TabsContent>

        <TabsContent value="owners" className="mt-0 space-y-4">
          <Tabs value={ownerTab} onValueChange={handleOwnerTabChange}>
            <TabsList>
              <TabsTrigger value="accounts">Quản lý tài khoản</TabsTrigger>
              <TabsTrigger value="approvals">Duyệt hồ sơ</TabsTrigger>
            </TabsList>

            <TabsContent value="accounts" className="mt-0">
              <AdminOwnersPage />
            </TabsContent>

            <TabsContent value="approvals" className="mt-0">
              <AdminOwnerApprovalsClient initialApplications={[]} initialTotalPages={0} />
            </TabsContent>
          </Tabs>
        </TabsContent>
      </Tabs>
    </div>
  );
}
