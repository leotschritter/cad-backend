microservices = {
  comment = {
    name         = "comment-service"
    ingress_url  = "https://dev-cl.tripico.fun"
    path_prefix  = "/comment"
    service_name = "comment-service"
    namespace    = "default"
    port         = 8080
  }
  itinerary = {
    name         = "itinerary-service"
    ingress_url  = "https://dev-itinerary.tripico.fun"
    path_prefix  = "/itinerary"
    service_name = "itinerary-service"
    namespace    = "default"
    port         = 8080
  }
  like = {
    name         = "like-service"
    ingress_url  = "https://dev-cl.tripico.fun"
    path_prefix  = "/like"
    service_name = "like-service"
    namespace    = "default"
    port         = 8080
  }
  location = {
    name         = "location-service"
    ingress_url  = "https://dev-itinerary.tripico.fun"
    path_prefix  = "/location"
    service_name = "location-service"
    namespace    = "default"
    port         = 8080
  }
  user = {
    name         = "user-service"
    ingress_url  = "https://dev-itinerary.tripico.fun"
    path_prefix  = "/user"
    service_name = "user-service"
    namespace    = "default"
    port         = 8080
  }
  travel-warnings = {
    name         = "travel-warnings-service"
    ingress_url  = "https://dev-warnings.tripico.fun"
    path_prefix  = "/warnings"
    service_name = "travel-warnings-service"
    namespace    = "default"
    port         = 8080
  }
  weather = {
    name         = "weather-service"
    ingress_url  = "https://dev-weather.tripico.fun"
    path_prefix  = "/api/weather"
    service_name = "weather-service"
    namespace    = "default"
    port         = 8080
  }
  feed = {
    name         = "recommendation-feed-service"
    ingress_url  = "https://dev-recommendation.tripico.fun"
    path_prefix  = "/feed"
    service_name = "recommendation-service"
    namespace    = "default"
    port         = 8080
  }
  graph = {
    name         = "recommendation-graph-service"
    ingress_url  = "https://dev-recommendation.tripico.fun"
    path_prefix  = "/graph"
    service_name = "recommendation-service"
    namespace    = "default"
    port         = 8080
  }
}