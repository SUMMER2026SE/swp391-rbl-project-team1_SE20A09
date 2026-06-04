$ErrorActionPreference = "Stop"

function Commit-File {
    param([string]$File, [string]$Message)
    if (Test-Path $File) {
        git add $File
        git commit -m $Message
    }
}

Commit-File "backend/src/main/java/com/sportvenue/repository/BookingRepository.java" "feat(booking): add custom queries to booking repository"
Commit-File "backend/src/main/java/com/sportvenue/repository/ComplaintRepository.java" "feat(complaint): add methods to complaint repository"
Commit-File "backend/src/main/java/com/sportvenue/repository/ReviewRepository.java" "feat(review): add custom queries to review repository"

Commit-File "backend/src/main/java/com/sportvenue/entity/converter/BookingStatusConverter.java" "fix(db): implement JPA enum converter for booking status"

git add backend/src/main/java/com/sportvenue/dto/request/
git add backend/src/main/java/com/sportvenue/dto/response/
git commit -m "feat(dto): implement request and response payloads for owner API"

git add backend/src/main/java/com/sportvenue/service/BookingService.java backend/src/main/java/com/sportvenue/service/impl/BookingServiceImpl.java
git commit -m "feat(booking): implement owner booking management service"

git add backend/src/main/java/com/sportvenue/service/ComplaintService.java backend/src/main/java/com/sportvenue/service/impl/ComplaintServiceImpl.java
git commit -m "feat(complaint): implement owner complaint resolution service"

git add backend/src/main/java/com/sportvenue/service/ReviewService.java backend/src/main/java/com/sportvenue/service/impl/ReviewServiceImpl.java
git commit -m "feat(review): implement owner review response service"

Commit-File "backend/src/main/java/com/sportvenue/controller/BookingController.java" "feat(api): expose owner booking REST endpoints"
Commit-File "backend/src/main/java/com/sportvenue/controller/ComplaintController.java" "feat(api): expose owner complaint REST endpoints"
Commit-File "backend/src/main/java/com/sportvenue/controller/ReviewController.java" "feat(api): expose owner review REST endpoints"

Commit-File "backend/src/main/java/com/sportvenue/exception/GlobalExceptionHandler.java" "chore(error): update global exception handler for verbose logging"
Commit-File "backend/src/main/java/com/sportvenue/SportVenueApplication.java" "chore(config): adjust main application annotations"
Commit-File "backend/src/main/java/com/sportvenue/config/DataSeeder.java" "chore(db): update data seeder for test accounts"
Commit-File "backend/src/main/resources/application.yml" "chore(config): update backend port and upload properties"

Commit-File "backend/src/main/resources/db/migration/V12__seed_complaints.sql" "chore(db): add V12 seed data for complaints"
Commit-File "backend/src/main/resources/db/migration/V13__seed_pickleball_data.sql" "chore(db): add V13 seed data for pickleball"
Commit-File "backend/src/main/resources/db/migration/V14__seed_pickleball_for_all_customers.sql" "chore(db): add V14 diverse pickleball bookings"
Commit-File "backend/src/main/resources/db/migration/V15__seed_diverse_bookings_and_cleanup.sql" "chore(db): add V15 cleanup duplicate bookings"
Commit-File "backend/src/main/resources/db/migration/V16__fix_duplicate_stadium.sql" "chore(db): add V16 fix duplicate stadium records"
Commit-File "backend/src/main/resources/db/migration/V17__reinsert_good_stadium_and_delete_bad.sql" "chore(db): add V17 reinsert valid stadium"
Commit-File "backend/src/main/resources/db/migration/V18__delete_my_dinh_stadium.sql" "chore(db): add V18 remove legacy stadium"
Commit-File "backend/src/main/resources/db/migration/V19__seed_new_booking_k34.sql" "chore(db): add V19 seed booking for K34"
Commit-File "backend/src/main/resources/db/migration/V20__seed_reviews_for_thanh_cong.sql" "chore(db): add V20 seed mock review for Thanh Cong"
Commit-File "backend/src/main/resources/db/migration/V21__fix_duplicate_reviews.sql" "chore(db): add V21 remove duplicate mock reviews"

Commit-File "frontend/src/app/owner/bookings/page.tsx" "feat(ui): create owner bookings dashboard"
git add frontend/src/app/owner/complaints/
git commit -m "feat(ui): create owner complaints dashboard"
git add frontend/src/app/owner/reviews/
git commit -m "feat(ui): create owner reviews dashboard"

Commit-File "frontend/src/app/profile/page.tsx" "feat(ui): integrate customer reviews in profile page"
Commit-File "frontend/src/app/booking/[id]/page.tsx" "feat(ui): implement review submission in booking detail"
Commit-File "frontend/src/app/booking/new/page.tsx" "feat(ui): update new booking page UI"
Commit-File "frontend/src/app/booking/payment/page.tsx" "feat(ui): update booking payment page UI"
Commit-File "frontend/src/app/bookings/page.tsx" "feat(ui): update customer bookings history list"
Commit-File "frontend/src/app/chat/page.tsx" "feat(ui): improve real-time chat interface"
Commit-File "frontend/src/app/complaints/page.tsx" "feat(ui): improve customer complaint submission"
Commit-File "frontend/src/app/owner/dashboard/page.tsx" "feat(ui): adjust owner dashboard metrics"
Commit-File "frontend/src/app/owner/page.tsx" "feat(ui): adjust owner root layout"

git add -A
git commit -m "chore(ui): minor UI fixes and component updates"

git push --force -u origin feature/HuyHoang1233/booking-review-notification
