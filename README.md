# Acme Factory

Fun and full of gusto :)

## Running with SBT

* compile:`sbt compile`
* test: `sbt test`
* run: `sbt run`

## Running with Docker
* build image: `sbt docker:publishLocal`
* start container: `docker-compose acme`

## Requirement Notes
* create Producer
  - creates main units, mops, brooms
  - one component every second (completely pseudo-random)
* create Consumer (3 seconds to create) (only 2 workers)
  - Type 1: Dry-2000 (prereqs: 1 main unit, 2 brooms)
  - Type 2: Wet-2000 (prereqs: 1 main unit, 2 mops)
* create Factory (queue)
  - 1 producer
  - 2 consumers
  - 10 items Max limit (wait for room)
  - after 10 seconds of inactivity, dispose of front in queue

## Design Notes
* **Design Decision**: Use basic cats Dequeue und monolithic instead of splitting into microservices (Time constraints)
* **Design Decision**: Build a custom Dequeue to be able to peek at front of pipeline
* **Design Decision**: Use cats effect IO to control sequencing and order of execution of side effects
* **Design Decision**: Use a Semaphore to control functional access to critical sections (e.g. accessing/modifying queue)
* **Design Decision**: Use logback as efficient and easy formatted (info or debug) logging
* **Bottleneck**: queue could potentially contain many unneeded components and workers have to wait until supplier removes them 1 by 1 every 10 seconds.
* **Optimization Idea 1**: set inactivityTimeout = sum(robot assemblyTime) + 1 s
* **Optimization Idea 2**: development of an algorithm that efficiently measures the wait time based upon components in queue and weight supplier creation thereon
* **Optimization Idea 3**: product-specific or component-specific pipelines/queues/topics with multiple suppliers
* **Optimization Idea 4**: peek in worker inventory and remove all from conveyor belt until at least 1 needed component
* **Optimization Idea 5**: instead of disposing components, add to bounded storage queues with auto-scaling of assemblyRobots depending on amounts
* **Improvement**: Need to improve test coverage and add Integration tests (Time constraints)
