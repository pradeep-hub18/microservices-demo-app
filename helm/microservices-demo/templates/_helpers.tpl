{{- define "microservices-demo.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "microservices-demo.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "microservices-demo.namespace" -}}
{{- .Values.namespace.name | default .Release.Namespace -}}
{{- end -}}

{{- define "microservices-demo.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/name: {{ include "microservices-demo.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "microservices-demo.selectorLabels" -}}
app.kubernetes.io/name: {{ include "microservices-demo.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "microservices-demo.authName" -}}
{{- printf "%s-auth" (include "microservices-demo.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "microservices-demo.catalogName" -}}
{{- printf "%s-catalog" (include "microservices-demo.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "microservices-demo.postgresName" -}}
{{- printf "%s-postgresql" (include "microservices-demo.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "microservices-demo.secretName" -}}
{{- printf "%s-secrets" (include "microservices-demo.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "microservices-demo.publicBaseUrl" -}}
{{- trimSuffix "/" .Values.global.publicBaseUrl -}}
{{- end -}}
