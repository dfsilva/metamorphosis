# pip install asyncio-nats-streaming
# Call using python3 generate_messages.py 1 topic1 '{"nome":"Auto de teste"}'

import asyncio
import sys
from nats.aio.client import Client as NATS
from stan.aio.client import Client as STAN

async def run(loop, qtd, topic, message):

    nc = NATS()
    await nc.connect(io_loop=loop)

    sc = STAN()
    await sc.connect("test-cluster", "message-producer", nats=nc)

    for x in range(qtd):
        await sc.publish(topic, message.encode())

    await sc.close()
    await nc.close()

if __name__ == '__main__':
    print("Qtd of messages "+ sys.argv[1])
    print("Topic "+ sys.argv[2])
    print("Message "+ sys.argv[3])

    loop = asyncio.get_event_loop()
    loop.run_until_complete(run(loop, int(sys.argv[1]), sys.argv[2], sys.argv[3]))
    loop.close()