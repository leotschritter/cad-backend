package de.htwg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.htwg.dto.deserializer.CoordinateDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeteoSourceResponse {

    @JsonDeserialize(using = CoordinateDeserializer.class)
    private Double lat;

    @JsonDeserialize(using = CoordinateDeserializer.class)
    private Double lon;

    private Integer elevation;
    private String timezone;
    private String units;

    @JsonProperty("daily")
    private DailyData daily;

    @JsonProperty("hourly")
    private HourlyData hourly;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyData {
        private List<DailyForecastData> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyData {
        private List<HourlyForecastData> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyForecastData {
        private String day;
        private String weather;
        private String icon;
        private String summary;

        @JsonProperty("all_day")
        private AllDayData allDay;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AllDayData {
            private Double temperature;

            @JsonProperty("temperature_min")
            private Double temperatureMin;

            @JsonProperty("temperature_max")
            private Double temperatureMax;

            private Wind wind;
            private Precipitation precipitation;

            @JsonProperty("cloud_cover")
            private CloudCover cloudCover;

            private Integer humidity;

            @JsonProperty("uv_index")
            private Integer uvIndex;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Wind {
                private Double speed;
                private String dir;
                private Integer angle;
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Precipitation {
                private Double total;
                private String type;
                private Integer probability;
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class CloudCover {
                private Integer total;
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyForecastData {
        private String date;
        private String weather;
        private String icon;
        private String summary;
        private Double temperature;
        private Wind wind;
        private Precipitation precipitation;

        @JsonProperty("cloud_cover")
        private CloudCover cloudCover;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Wind {
            private Double speed;
            private String dir;
            private Integer angle;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Precipitation {
            private Double total;
            private String type;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CloudCover {
            private Integer total;
        }
    }
}
