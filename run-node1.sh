#!/bin/bash
#docker rm -f node01 && \
docker run \
  --name node01 \
  --network metamorphosis_data_network \
  -e CANONICAL_PORT=2552 \
  -e NATS_CLIENT_ID=test1 \
  -e HTTP_PORT=8082 \
  -e CANONICAL_HOST_NAME=node01 \
  -e SEED_NODES=akka://MetamorphosisSystem@node01:2552,akka://MetamorphosisSystem@node00:2551 \
  -e NATS_URL=nats://metamorphosis_nats_1:4222 \
  -e POSTGRES_URL=jdbc:postgresql://metamorphosis_postgres_1:5432/postgres \
  -p 8082:8082 \
  -p 2552:2552 \
  default/metamorphosis