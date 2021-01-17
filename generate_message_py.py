import asyncio
import sys
from nats.aio.client import Client as NATS
from stan.aio.client import Client as STAN

async def run(loop, qtdMessages, topic):

    nc = NATS()
    await nc.connect(io_loop=loop)

    sc = STAN()
    await sc.connect("test-cluster", "client-123", nats=nc)

    await sc.publish(topic, b'{"nome": "Auto de teste"}')

    await sc.close()
    await nc.close()

if __name__ == '__main__':
    print("Total messages "+ sys.argv[1])
    print("Topic "+ sys.argv[2])
    loop = asyncio.get_event_loop()
    loop.run_until_complete(run(loop, sys.argv[1], sys.argv[2]))
    loop.close()