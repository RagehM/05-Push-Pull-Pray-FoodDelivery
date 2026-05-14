# checkout-service k8s manifests — S5-READ-DB

These manifests stand up an **isolated** Postgres for the checkout-service
(`talabatdb-checkout`). Apply order:

```sh
kubectl apply -f k8s/namespaces/namespace.yaml
kubectl apply -f k8s/checkout-service/checkout-postgres-secret.yaml
kubectl apply -f k8s/checkout-service/checkout-postgres-service.yaml
kubectl apply -f k8s/checkout-service/checkout-postgres-statefulset.yaml
# checkout-postgres-pvc.yaml is mirrored from the volumeClaimTemplate above;
# only apply it directly when bootstrapping a cluster without auto-provisioned PVCs.
```

The checkout-service deployment should set
`SPRING_DATASOURCE_URL=jdbc:postgresql://checkout-postgres:5432/talabatdb-checkout`
and pull credentials from the `checkout-postgres-secret`.
