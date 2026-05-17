kubectl delete namespace talabat
kubectl apply -f ./k8s/namespaces/namespace.yaml
kubectl apply -f ./k8s/secrets/
kubectl apply -f ./k8s/pvcs/
kubectl apply -f ./k8s/statefulsets/
kubectl wait --for=condition=ready pod -l app=order-postgres -n talabat --timeout=120s
kubectl apply -f ./k8s/configmaps/
kubectl apply -f ./k8s/deployments/
kubectl apply -f ./k8s/services/
kubectl apply -f ./k8s/api-gateway/


