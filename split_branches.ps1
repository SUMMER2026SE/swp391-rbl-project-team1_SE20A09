$ErrorActionPreference = "Stop"

function Commit-File {
    param([string]$File, [string]$Message)
    if (Test-Path $File) {
        git add $File
        git commit -m $Message
    }
}

# Ensure we are starting from origin/main without any tracked changes
git checkout origin/main

# 1. Booking Branch
git checkout -B feature/HuyHoang1233/owner-booking-management origin/main
Commit-File "backend/src/main/java/com/sportvenue/repository/BookingRepository.java" "feat(booking): implement BookingRepository queries"
Commit-File "backend/src/main/java/com/sportvenue/entity/converter/BookingStatusConverter.java" "fix(db): add BookingStatus converter"
Commit-File "backend/src/main/java/com/sportvenue/dto/response/CustomerBookingResponse.java" "feat(dto): add CustomerBookingResponse DTO"
git add backend/src/main/java/com/sportvenue/service/BookingService.java backend/src/main/java/com/sportvenue/service/impl/BookingServiceImpl.java
git commit -m "feat(booking): implement BookingService for owner"
Commit-File "backend/src/main/java/com/sportvenue/controller/BookingController.java" "feat(api): expose BookingController endpoints"
Commit-File "frontend/src/app/owner/bookings/page.tsx" "feat(ui): implement owner bookings page"
Commit-File "frontend/src/app/owner/dashboard/page.tsx" "feat(ui): update owner dashboard stats"
Commit-File "frontend/src/app/owner/page.tsx" "feat(ui): add owner root layout"
git push --force -u origin feature/HuyHoang1233/owner-booking-management

# 2. Review Branch
git checkout -B feature/HuyHoang1233/owner-review origin/main
Commit-File "backend/src/main/java/com/sportvenue/repository/ReviewRepository.java" "feat(review): implement ReviewRepository queries"
git add backend/src/main/java/com/sportvenue/dto/request/CreateReviewRequest.java backend/src/main/java/com/sportvenue/dto/request/ReplyReviewRequest.java backend/src/main/java/com/sportvenue/dto/response/ReviewResponse.java
git commit -m "feat(dto): add Review related DTOs"
git add backend/src/main/java/com/sportvenue/service/ReviewService.java backend/src/main/java/com/sportvenue/service/impl/ReviewServiceImpl.java
git commit -m "feat(review): implement ReviewService for owner"
Commit-File "backend/src/main/java/com/sportvenue/controller/ReviewController.java" "feat(api): expose ReviewController endpoints"
git add frontend/src/app/owner/reviews/
git commit -m "feat(ui): implement owner reviews page"
Commit-File "frontend/src/app/profile/page.tsx" "feat(ui): display customer reviews on profile"
Commit-File "frontend/src/app/booking/[id]/page.tsx" "feat(ui): allow customer to post review"
git push --force -u origin feature/HuyHoang1233/owner-review

# 3. Complaint Branch
git checkout -B feature/HuyHoang1233/owner-complaint origin/main
Commit-File "backend/src/main/java/com/sportvenue/repository/ComplaintRepository.java" "feat(complaint): implement ComplaintRepository queries"
git add backend/src/main/java/com/sportvenue/dto/request/CreateComplaintRequest.java backend/src/main/java/com/sportvenue/dto/request/ReplyComplaintRequest.java backend/src/main/java/com/sportvenue/dto/request/ResolveComplaintRequest.java backend/src/main/java/com/sportvenue/dto/response/ComplaintResponse.java
git commit -m "feat(dto): add Complaint related DTOs"
git add backend/src/main/java/com/sportvenue/service/ComplaintService.java backend/src/main/java/com/sportvenue/service/impl/ComplaintServiceImpl.java
git commit -m "feat(complaint): implement ComplaintService for owner"
Commit-File "backend/src/main/java/com/sportvenue/controller/ComplaintController.java" "feat(api): expose ComplaintController endpoints"
git add frontend/src/app/owner/complaints/
git commit -m "feat(ui): implement owner complaints page"
Commit-File "frontend/src/app/complaints/page.tsx" "feat(ui): allow customer to submit complaint"
git push --force -u origin feature/HuyHoang1233/owner-complaint

# 4. Seed Data & UI Fixes Branch (to contain all the rest)
git checkout -B feature/HuyHoang1233/seed-data-ui-fixes origin/main
Commit-File "backend/src/main/java/com/sportvenue/exception/GlobalExceptionHandler.java" "fix(error): update GlobalExceptionHandler"
Commit-File "backend/src/main/java/com/sportvenue/SportVenueApplication.java" "chore(config): update main application class"
Commit-File "backend/src/main/java/com/sportvenue/config/DataSeeder.java" "chore(db): update DataSeeder with more test accounts"
Commit-File "backend/src/main/resources/application.yml" "chore(config): adjust port and properties"

# Seed data SQL files
git add backend/src/main/resources/db/migration/
git commit -m "chore(db): add migrations V12 to V21 for seed data"

# Frontend remaining files
git add frontend/src/app/booking/new/page.tsx frontend/src/app/booking/payment/page.tsx frontend/src/app/bookings/page.tsx frontend/src/app/chat/page.tsx frontend/src/components/landing/VenueCard.tsx frontend/src/components/layout/Header.tsx frontend/src/lib/api.ts
git commit -m "feat(ui): improve UI for booking process and chat"

git add -A
git commit -m "chore(misc): add remaining files"
git push --force -u origin feature/HuyHoang1233/seed-data-ui-fixes

# Go back to booking branch for safety
git checkout feature/HuyHoang1233/owner-booking-management
