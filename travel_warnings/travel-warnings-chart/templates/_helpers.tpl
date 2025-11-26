{{/*
Expand the name of the chart.
*/}}
{{- define "travel-warnings-chart.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "travel-warnings-chart.fullname" -}}
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
{{- define "travel-warnings-chart.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "travel-warnings-chart.labels" -}}
helm.sh/chart: {{ include "travel-warnings-chart.chart" . }}
{{ include "travel-warnings-chart.selectorLabels" . }}
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
{{- define "travel-warnings-chart.selectorLabels" -}}
app.kubernetes.io/name: {{ include "travel-warnings-chart.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "travel-warnings-chart.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "travel-warnings-chart.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Postgres labels
*/}}
{{- define "travel-warnings-chart.postgres.labels" -}}
{{ include "travel-warnings-chart.labels" . }}
app.kubernetes.io/component: database
{{- end }}

{{/*
API labels
*/}}
{{- define "travel-warnings-chart.api.labels" -}}
{{ include "travel-warnings-chart.labels" . }}
app.kubernetes.io/component: api
{{- end }}

