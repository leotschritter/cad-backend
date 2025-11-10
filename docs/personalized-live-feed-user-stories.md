# Personalized Live Feed — Feature Overview & User Stories

Purpose

This document describes the "Personalized Live Feed" feature in a non-technical, user-facing way. The feed is a page in the frontend where a traveller discovers suggested itineraries and travel experiences from other travellers that are likely relevant to them (based on shared likes, destinations visited, and other simple social signals).

Value proposition

- Help travellers discover relevant itineraries and experiences faster.
- Surface social proof (what other travellers liked) and inspiration for planning.
- Increase engagement by recommending content that matches the traveller's tastes and past activity.

Primary actor

- Traveller: A signed-in user who plans trips and browses suggestions.

Secondary actors (implicit)

- Other Travellers: produce content (itineraries, likes) that the feed uses to make suggestions.

Scope

- A single page in the frontend: "Live Feed" that shows suggested itineraries and travel stories.
- Interaction primitives: view, like/save, open itinerary details, follow travellers, and report content.
- Personalization uses simple social signals (shared likes, visited locations) — no complex ML detail here.

Epic

- Epic: Personalized Live Feed
  - Summary: As a traveller, I want a single feed page that recommends itineraries and travel stories from other travellers that are relevant to me, so I can discover interesting trips and inspirations.

Feature overview (what the page does)

- Landing: The traveller opens the single, unified "Feed" page and sees a single ranked list of suggested itineraries and travel stories (all signals combined into one feed).
- Relevance signals used (high level):
  - Itineraries that people liked who also liked itineraries I liked.
  - Itineraries of travellers who visited the same locations I visited.
  - Popular or trending itineraries in destinations related to my trips.
- Item presentation: each feed card shows title, short description, origin traveller (name/handle), a few tags (destinations, themes), and key stats (likes, saves).
- Interactions:
  - Open itinerary to see details.
  - Like or save an itinerary.
  - Follow the author to see more from them later.
  - Report or hide the item.
- Navigation: simple controls to refresh and lightly filter or sort the single feed (for example, a toggle to prioritize "Near my next trip" or show "Trending" items), but the page remains one unified feed rather than separate feeds or distinct pages.

User stories (feature-focused, non-technical)

- Story A — See suggestions on Feed page
  - As a traveller, I want to open a Feed page and immediately see suggested itineraries and travel stories that might interest me.
  - Acceptance:
    - The feed page displays a list of suggested itinerary cards tailored to me.
    - Each card shows the itinerary title, short excerpt, origin traveller, primary destination tag, and number of likes.
    - Items are ordered so the most relevant appear first.

- Story B — Explore an itinerary
  - As a traveller, I want to open a suggested itinerary from the feed so I can read details and decide if I want to follow or save it.
  - Acceptance:
    - Tapping or clicking a card opens the itinerary detail page.
    - From the detail view I can save/like the itinerary or go back to the feed.

- Story C — Save or like items I enjoy
  - As a traveller, I want to like or save itineraries from the feed to keep them for later.
  - Acceptance:
    - I can like or save any itinerary from the feed card or the detail view.
    - The UI reflects the change immediately (e.g., like counter increments, save icon toggles).

- Story D — See items liked by travellers similar to me
  - As a traveller, I want the feed to surface itineraries that were liked by people who also liked the same itineraries I liked, so I can discover closely related content.
  - Acceptance:
    - The unified feed prioritizes items that come from this signal (e.g., items are ranked higher when they are liked by users with similar tastes).
    - The UI shows a short reason label when relevant (for example "Liked by travellers who also liked your trips") so I understand why an item was suggested.

- Story E — Discover itineraries from travellers who visited the same places
  - As a traveller, I want to see itineraries from travellers who visited destinations I have been to or plan to visit.
  - Acceptance:
    - The feed surfaces items tagged with destinations that match my visited or planned locations.
    - There is an intuitive label (e.g., "From travellers who visited: [Destination]") when a match is the reason for the suggestion.

Acceptance criteria (overall, non-technical)

- Relevance: The top items on a user's feed should be clearly tied to their past likes or destinations (UI should indicate the reason for the suggestion where possible).
- Discoverability: Users can open suggested itineraries, save/like them, and follow authors without leaving the feed context.
- Control: Users can hide or report items they don't want to see.
- Persistence: Saved items remain accessible in the user's saved list.
- Privacy: The feed respects user privacy rules (only shows public itineraries or content the user has permission to view).

UX notes / Suggested UI elements

- Feed card layout: image (optional), title, short excerpt, origin traveller (avatar + name), destination tags, like/save icons, small reason label (e.g., "Liked by travellers like you") and overflow menu (hide/report).
- Top-of-page controls: search box, quick filters, and a "Refresh" button to manually refresh suggestions.
- Empty state: helpful prompt to create or like itineraries to improve suggestions ("Like itineraries to get better recommendations").

Privacy & content policy (brief)

- Only show content that is public or visible to the requesting user.
- Allow users to opt-out of being used as a signal for personalized suggestions.

Suggested next steps (non-technical)

1. Wireframe the Feed page and feed card.  
2. Finalize the short list of signals to use for suggestions (liked-by-similar, visited-destinations, trending).  
3. Define the UI labels that explain "why" an item is suggested (these make recommendations feel transparent).  
4. Prepare a small user test script: show mockups to 5–8 users and ask whether the suggestions feel relevant and why.


---

Document history

- Created: (auto) Personalized Live Feed feature overview and user stories (non-technical).
