{{/*
Expand the name of the chart.
*/}}
{{- define "recommendation-service-chart.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "recommendation-service-chart.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "recommendation-service-chart.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "recommendation-service-chart.labels" -}}
helm.sh/chart: {{ include "recommendation-service-chart.chart" . }}
{{ include "recommendation-service-chart.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.commonLabels }}
{{ toYaml . }}
{{- end }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "recommendation-service-chart.selectorLabels" -}}
app.kubernetes.io/name: {{ include "recommendation-service-chart.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Neo4j labels
*/}}
{{- define "recommendation-service-chart.neo4j.labels" -}}
{{ include "recommendation-service-chart.labels" . }}
app.kubernetes.io/component: database
{{- end }}

{{/*
API labels
*/}}
{{- define "recommendation-service-chart.api.labels" -}}
{{ include "recommendation-service-chart.labels" . }}
app.kubernetes.io/component: api
{{- end }}
