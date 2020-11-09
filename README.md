# Earthquake API - Distributed tracing demo using Istio and ktor-header-forwarding

Demo application consisting of two Ktor microservices instrumented with [ktor-header-forwarding](https://github.com/fstien/ktor-header-forwarding), illustrating the use of distributed tracing with [Istio](https://istio.io/) in a local [Kubernetes cluster](https://www.docker.com/products/docker-desktop).

* EarthquakeAdaptor retrieves data about earthquakes that happened today using an [API from the U.S. Geological Survey](https://earthquake.usgs.gov/fdsnws/event/1/).
* EarthquakeStats calls EarthquakeStats and exposes statistics about today's earthquakes such as `GET /earthquakes/latest` or `GET /earthquakes/biggest`. 


## Requirements
This demo assumes that you have the following installed on your local machine. 
- [Docker Desktop](https://www.docker.com/products/docker-desktop) (or some other local Kubernetes installation)
- [Istioctl](https://istio.io/latest/docs/ops/diagnostic-tools/istioctl/)

## Running
1. Build docker images for both services. This can take some time if you do not have the base images cached. 
        
        docker build -f docker/Dockerfile.EarthquakeStats -t earthquake-stats .
        docker build -f docker/Dockerfile.EarthquakeAdaptor -t earthquake-adaptor .

2. Configure the default namespace to have istio sidecar proxy injection enabled. 
        
        kubectl apply -f k8s/namespaces.yaml

3. Install Istio onto the cluter with the demo config (which includes a Jaeger instance).
        
        istioctl manifest apply --set profile=demo

4. Release the two docker containers to the cluster.
        
        kubectl apply -f k8s/earthquake-adaptor.yaml
        kubectl apply -f k8s/earthquake-stats.yaml

5. Check that both pods are ready. 
        
        kubectl get po

6. Send some traffic to the `earthquake-stats` pod. 
        
        curl localhost:30101/earthquake/latest

7. Open the Jaeger UI to view traces.
        
        istioctl dashboard jaeger



## Steps 

1. Import [ktor-header-forwarding](https://github.com/fstien/ktor-header-forwarding) in the `build.gradle` of the `earthquake-stats` application.

        implementation "com.github.fstien:ktor-header-forwarding:0.1.0"

2. Install the `HeaderForwardingServer` Ktor feature onto the application call pipeline, configured to forward requests as described in the [Istio distributed tracing documentation](https://istio.io/latest/docs/tasks/observability/distributed-tracing/overview/). 

        install(HeaderForwardingServer) {
           header("x-request-id")
           header("x-ot-span-context")
           filter { header -> header.startsWith("x-b3-") }
        }

3. Install the `HeaderForwardingClient` Ktor feature onto the http client. 

        install(HeaderForwardingClient)

   We do not need to instrument `earthquake-adaptor` as it does not make any requests within the cluster. 
