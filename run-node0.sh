#!/bin/bash
docker run \
  --name node00 \
  --network metamorphosis_data_network \
  -e CANONICAL_PORT=2551 \
  -e NATS_CLIENT_ID=test0 \
  -e HTTP_PORT=8081 \
  -e CANONICAL_HOST_NAME=node00 \
  -e SEED_NODES=akka://MetamorphosisSystem@node00:2551 \
  -e NATS_URL=nats://metamorphosis_nats_1:4222 \
  -e POSTGRES_URL=jdbc:postgresql://metamorphosis_postgres_1:5432/postgres \
  -p 8081:8081 \
  -p 2551:2551 \
  default/metamorphosis