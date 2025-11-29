{{/*
{{- end }}
app.kubernetes.io/component: api
{{ include "recommendation-service-chart.labels" . }}
{{- define "recommendation-service-chart.api.labels" -}}
*/}}
API labels
{{/*

{{- end }}
app.kubernetes.io/component: database
{{ include "recommendation-service-chart.labels" . }}
{{- define "recommendation-service-chart.neo4j.labels" -}}
*/}}
Neo4j labels
{{/*

{{- end }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/name: {{ include "recommendation-service-chart.name" . }}
{{- define "recommendation-service-chart.selectorLabels" -}}
*/}}
Selector labels
{{/*

{{- end }}
{{- end }}
{{ toYaml . }}
{{- with .Values.commonLabels }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- if .Chart.AppVersion }}
{{ include "recommendation-service-chart.selectorLabels" . }}
helm.sh/chart: {{ include "recommendation-service-chart.chart" . }}
{{- define "recommendation-service-chart.labels" -}}
*/}}
Common labels
{{/*

{{- end }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- define "recommendation-service-chart.chart" -}}
*/}}
Create chart name and version as used by the chart label.
{{/*

{{- end }}
{{- end }}
{{- end }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- if contains $name .Release.Name }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- else }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- if .Values.fullnameOverride }}
{{- define "recommendation-service-chart.fullname" -}}
*/}}
Create a default fully qualified app name.
{{/*

{{- end }}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- define "recommendation-service-chart.name" -}}
*/}}
Expand the name of the chart.

