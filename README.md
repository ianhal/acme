# Acme Factory

Fun and full of gusto :)

## How to compile

`sbt compile`

## How to test

`sbt test`

## How to run

`sbt run`

## Needed Optimization/Improvement Notes
* sds
* 
## Development Notes
* create Producer
  - creates main units, mops, brooms
  - one component every second (completely random)
* create Consumer (3 seconds to create) (only 2 workers)
  - Type 1: Dry-2000 (prereqs: main unit, 2 brooms)
  - Type 2: Wet-2000 (prereqs: main unit, 2 mops)
* create Factory (queue)
  - 1 producer
  - 2 consumers
  - 10 items Max limit (wait for room)
  - if 10 items and after 10 seconds no movement, delete last in queue
