#!/usr/bin/env bash

kubectl apply -f smtp-secret.yaml
kubectl apply -f app-config.yaml
kubectl apply -f postgres-pvc.yaml
kubectl apply -f postgres.yaml
kubectl apply -f travel-warnings-api.yaml
kubectl apply -f ingress.yaml