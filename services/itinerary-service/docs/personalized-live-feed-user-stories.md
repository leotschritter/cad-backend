## Epic - Personalized Live Feed

**Epic:** Personalized Live Feed

**Summary:** As a traveller, I want a single feed page that recommends itineraries and travel stories from other
travellers that are relevant to me, so I can discover interesting trips and inspirations.

---

## User Stories

### Story 1 — See and Explore Suggestions on Feed Page

**As a traveller**, I want to open a Feed page and immediately see suggested itineraries and travel stories that might
interest me, and be able to view their details to learn more.

**Acceptance Criteria:**

1. The feed page displays a list of suggested itinerary tailored to the traveller.
2. Each card shows at least: title, short description, traveller name and number of likes.
3. Items are ordered by relevance based on social signals (likes and shared destinations).
4. Clicking or tapping a card opens the itinerary detail page.
5. From the detail view, the user can return to the feed without losing position or state.

---

### Story 2 — Discover Itineraries from Travellers Who Visited the Same Places

**As a traveller**, I want to see itineraries from travellers who visited destinations I've been to or plan to visit, so
I can find relevant inspiration.

**Acceptance Criteria:**

1. The feed surfaces itineraries tagged with destinations that match my visited or planned locations.
2. When applicable, the card includes a short label explaining the match (e.g., "From travellers who
   visited: [Destination]").
3. These itineraries are prioritized alongside other relevance signals.
4. The feature only uses destinations the user has explicitly marked as visited or planned.

---

### Story 3 — Refine Recommendation Algorithm with Social Signals and Basic Feed

**As a traveller**, I want the feed to prioritize popular itineraries with more likes and automatically fall back to a
basic feed with the most recommended content when I'm new and have no personalization data yet, so I can always
discover high-quality travel inspiration.

**Acceptance Criteria:**

1. The recommendation algorithm prioritizes itineraries with higher like counts as a key relevance signal.
2. Itineraries with more likes appear higher in the personalized feed when other relevance factors are equal.
3. When a new traveller has no personalization data (no visited/planned destinations), the feed automatically shows a
   "basic feed" with the most liked and recommended itineraries across all travellers.
4. The basic feed serves as a fallback for users without sufficient personalization data, not as a separate view.
5. As users add visited/planned destinations, the feed gradually transitions from basic to personalized recommendations.
6. The algorithm balances popularity (likes) with personalization (shared destinations) to avoid showing only viral
   content once personalization data is available.
