# Git Workflow - View Venue Detail Feature

## Branch Creation
```bash
git checkout develop
git pull origin develop
git checkout -b feature/venue/detail-page-enhancements
```

## Files to Commit

### New Files
```bash
git add frontend/src/components/venues/ImageGallery.tsx
git add frontend/src/components/venues/VenueMap.tsx
git add IMPLEMENTATION_VENUE_DETAIL.md
```

### Modified Files
```bash
git add frontend/src/app/venues/[id]/page.tsx
git add frontend/src/app/search/components/StadiumCard.tsx
```

## Commit Messages

```bash
# Commit 1: Add venue components
git commit -m "feat(venue): add ImageGallery and VenueMap components

- Add ImageGallery with carousel and lightbox
- Add VenueMap with Leaflet integration
- Lazy load map for performance"

# Commit 2: Update venue detail page
git commit -m "feat(venue): enhance detail page with gallery and map

- Replace static image with ImageGallery component
- Integrate Leaflet map with Suspense
- Improve layout and user experience"

# Commit 3: Connect search to detail
git commit -m "feat(venue): connect search cards to detail page

- Add Link wrapper to StadiumCard
- Update button text to 'Xem Chi Tiết'
- Enable full card click navigation"
```

## Or Single Commit
```bash
git add -A
git commit -m "feat(venue): complete venue detail page implementation

- Add ImageGallery component with carousel and lightbox
- Add VenueMap component with Leaflet integration
- Connect StadiumCard to venue detail page
- Enhance detail page layout and UX
- Add lazy loading for map performance"
```

## Push and Create PR
```bash
git push origin feature/venue/detail-page-enhancements
```

Then create Pull Request on GitHub with description:

```markdown
## Feature: View Venue Detail

### Changes
- ✅ Added ImageGallery component (carousel + lightbox)
- ✅ Added VenueMap component (Leaflet integration)
- ✅ Enhanced venue detail page with new components
- ✅ Connected search cards to detail page via Link

### Backend
No backend changes needed - endpoints already exist.

### Testing
- [x] Image carousel works (arrows, dots, lightbox)
- [x] Map displays correct location
- [x] Navigation from search works
- [x] "Đặt sân ngay" links to booking
- [x] Responsive design verified

### Screenshots
[Add screenshots of venue detail page]

### Related
- Implements requirement: View Venue Detail (public access)
- Related to search functionality
- Connects to booking flow
```

## Merge
After approval:
```bash
git checkout develop
git pull origin develop
git merge feature/venue/detail-page-enhancements
git push origin develop
```
