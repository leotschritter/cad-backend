{{/*
Expand the name of the chart.
*/}}
{{- define "comments-likes-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "comments-likes-service.fullname" -}}
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
{{- define "comments-likes-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "comments-likes-service.labels" -}}
helm.sh/chart: {{ include "comments-likes-service.chart" . }}
{{ include "comments-likes-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "comments-likes-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "comments-likes-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app: comments-likes-service
service: itinerary
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "comments-likes-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "comments-likes-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

