kubectl delete namespace monitoring
kubectl apply -f ./k8s/namespaces/monitoring-namespace.yaml
kubectl apply -f ./k8s/monitoring/prometheus/
kubectl apply -f ./k8s/monitoring/loki/
kubectl apply -f ./k8s/monitoring/grafana/
kubectl wait --for=condition=ready pod -l app=loki -n monitoring --timeout=120s

kubectl delete namespace talabat
kubectl apply -f ./k8s/namespaces/namespace.yaml
kubectl apply -f ./k8s/secrets/
kubectl apply -f ./k8s/pvcs/
kubectl apply -f ./k8s/statefulsets/
kubectl wait --for=condition=ready pod -l app=order-postgres -n talabat --timeout=120s
kubectl wait --for=condition=ready pod -l app=checkout-postgres -n talabat --timeout=120s
kubectl wait --for=condition=ready pod -l app=restaurant-postgres -n talabat --timeout=120s
kubectl wait --for=condition=ready pod -l app=user-postgres -n talabat --timeout=120s
# kubectl wait --for=condition=ready pod -l app=delivery-postgres -n talabat --timeout=120s
kubectl wait --for=condition=ready pod -l app=rabbitmq -n talabat --timeout=120s
kubectl wait --for=condition=ready pod -l app=redis -n talabat --timeout=120s
# kubectl wait --for=condition=ready pod -l app=cassandra -n talabat --timeout=120s
kubectl apply -f ./k8s/configmaps/
kubectl apply -f ./k8s/deployments/
kubectl apply -f ./k8s/services/
kubectl apply -f ./k8s/api-gateway/


