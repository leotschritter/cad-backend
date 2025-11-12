## Epic  Personalized Live Feed

**Epic:** Personalized Live Feed  
**Summary:** As a traveller, I want a single feed page that recommends itineraries and travel stories from other travellers that are relevant to me, so I can discover interesting trips and inspirations.

---

## User Stories

### Story 1 — See and Explore Suggestions on Feed Page

**As a traveller**, I want to open a Feed page and immediately see suggested itineraries and travel stories that might interest me, and be able to view their details to learn more.

**Acceptance Criteria:**
1. The feed page displays a ranked list of suggested itinerary cards tailored to the traveller.
2. Each card shows at least: title, short excerpt, origin traveller, primary destination tag, and number of likes.
3. Items are ordered by relevance based on social signals (likes and shared destinations).
4. Clicking or tapping a card opens the itinerary detail page.
5. From the detail view, the user can return to the feed without losing position or state.
6. Only public itineraries are displayed.

---

### Story 2 — Like Itineraries from the Feed

**As a traveller**, I want to like itineraries from the feed so I can express interest and improve future recommendations.

**Acceptance Criteria:**
1. The user can like or unlike any itinerary directly from the feed card or the detail view.
2. The UI reflects the change immediately (like counter updates, icon toggles).
3. Liked itineraries are used as a relevance signal for future recommendations.
4. Feedback on likes is persisted across sessions.

---

### Story 3 — Discover Itineraries from Travellers Who Visited the Same Places

**As a traveller**, I want to see itineraries from travellers who visited destinations I’ve been to or plan to visit, so I can find relevant inspiration.

**Acceptance Criteria:**
1. The feed surfaces itineraries tagged with destinations that match my visited or planned locations.
2. When applicable, the card includes a short label explaining the match (e.g., “From travellers who visited: [Destination]”).
3. These itineraries are prioritized alongside other relevance signals.
4. The feature only uses destinations the user has explicitly marked as visited or planned.

---

## Acceptance Criteria (Overall, Non-Technical)

- **Relevance:** The top items on a user’s feed should be clearly tied to their past likes or destinations.
- **Discoverability:** Users can easily explore itineraries by clicking cards.
- **Persistence:** Likes are saved and influence future recommendations.

---
