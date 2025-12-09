import json
import random
import signal
import sys
import time
from dataclasses import dataclass
from math import radians, sin, cos, atan2, sqrt

import pika
import redis
from pika.adapters.blocking_connection import BlockingChannel

# Dataclasses for the request, location, and driver
class Location:
    def __init__(self, longitude, latitude):
        self.longitude = longitude
        self.latitude = latitude

class MatchRequest:
    def __init__(self, id, pickup):
        self.id = id
        self.pickup = pickup

class Driver:
    def __init__(self, id, location):
        self.id = id
        self.location = location

# Class for the worker
class Worker:
    def __init__(self, region):
        self.region = region
        # Pika Python client to connection to the RabbitMQ server
        self.connection = pika.BlockingConnection(pika.ConnectionParameters(
            'rabbit',
            5672,
            '/',
            pika.PlainCredentials('dst', 'dst')))

        # Create a new channel for the worker
        self.channel = self.connection.channel()

        # Declare the queue for the worker (like in Constants.WORK_QUEUES)
        self.queue = "dst." + region

        # Declare the queue for the worker, it will survive a broker restart (durable=True)
        self.channel.queue_declare(self.queue, durable=True)

        # Channel subscribes (consumes) from self.queue, uses self.callback and auto-acknowledges the messages
        self.channel.basic_consume(queue=self.queue,
                                   on_message_callback=self.callback,
                                   auto_ack=True)


    # Fetch all drivers from the Redis database
    def fetch_drivers(self):

        # Connect to the Redis database with the hostname "redis"
        r = redis.Redis(host="redis")

        # Get all drivers from the Redis database with the key "drivers:region"
        all = r.hgetall("drivers:" + self.region)
        drivers = []

        # Iterate over all drivers and create a new Driver object with the driver ID and location
        for k, v in all.items():
            # Decode the keys and values hashes  to strings
            k, v = k.decode(), v.decode()
            # Since the location is stored as a string "longitude latitude"
            lon, lat = v.split(" ")
            # Append the new driver to the drivers (String --> float)
            drivers.append(Driver(k, Location(float(lon), float(lat))))

        return drivers

    # Calculate the distance between two locations using the Haversine formula
    def distance(self, start, end):
        lat1 = radians(start.latitude)
        lon1 = radians(start.longitude)
        lat2 = radians(end.latitude)
        lon2 = radians(start.longitude)

        dlon = lon2 - lon1
        dlat = lat2 - lat1

        a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
        c = 2 * atan2(sqrt(a), sqrt(1 - a))

        # Earth radius in km
        R = 6373.0
        return R * c

    def match(self, request, drivers):
        # Return min of drivers based on the distance between the driver location and the request pickup location
        # (Min of all lambda functions with one argument d(=distance) to all drivers)
        return min(drivers, key=lambda d: self.distance(d.location, request.pickup))

    # Simulate work for the worker depending on the region
    def simulate_work(self):
        random_sleeps = {
            "at_linz": (1, 2),
            "at_vienna": (3, 5),
            "de_berlin": (8, 11),
        }

        lower, upper = random_sleeps[self.region]
        time.sleep(random.randint(lower, upper))

    # Remove the driver from the Redis database
    def remove_driver(self, driver):
        # Connect to the Redis database with the hostname "redis"
        r = redis.Redis(host="redis")
        # Remove the driver with the key "drivers:region" and the driver ID, return 1 if the driver was removed
        return r.hdel("drivers:" + self.region, str(driver.id))

    # Publish the result of the matching to the RabbitMQ exchange "dst.workers" with the routing key "requests.REGION"
    def publish(self, req, matched_driver, duration_ms):
        message = {
            "requestId": req.id,
            "driverId": matched_driver.id,
            "processingTime": duration_ms,
        }
        self.channel.basic_publish("dst.workers", "requests." + self.region, json.dumps(message).encode())

    # Callback function for the worker queue, triggered when a message is consumed from the RabbitMQ queue that the
    # worker is subscribed to
    def callback(self, ch, method, properties, body):
        print("callback")
        while True:
            # Start the timer (in milliseconds)
            start_ms = time.time_ns() / 1000

            # Parse JSON message into a Python dictionary
            message = json.loads(body)

            # Create a new MatchRequest object with the message ID and the pickup location
            req = MatchRequest(message['id'], Location(message['pickup']['longitude'], message['pickup']['longitude']))
            print(f"Start matching for {req.id}")

            # Fetch all drivers from the Redis database
            drivers = self.fetch_drivers()

            # If there are no drivers available, print "No driver available" and publish the result with an empty driver
            if drivers == []:
                print("No driver available")
                # Publish with MatchedRequest id, an empty driver and a processingTime of 0
                self.publish(req, Driver("", None), 0)
                break

            print(f"Got {len(drivers)} available drivers")

            # Match the request with the closest driver
            matched_driver = self.match(req, drivers)

            # Simulate work for the worker
            self.simulate_work()
            print(f"Matched driver {matched_driver.id}")

            # Remove the matched driver from the Redis database
            removed = self.remove_driver(matched_driver)

            # If the driver was not removed, redo match procedure
            if removed == 0:
                print("Recalculate a match, driver no longer available")
                continue

            # Calculate the duration of the matching (in milliseconds)
            duration_ms = (time.time_ns() / 1000) - start_ms

            # Publish the result of the matching and break the loop
            self.publish(req, matched_driver, int(duration_ms))
            print(f"Published result")
            break

    def start(self):
        def term_handler(_signo, _stack_frame):
            print("Terminiated with SIGTERM")
            sys.exit(0)

        # Registering the term_handler function as the handler for the SIGTERM signal
        signal.signal(signal.SIGTERM, term_handler)

        print("Waiting for messages")
        # Start consumption of the channel
        self.channel.start_consuming()


# Entry point of the worker
def main():
    # sys.argv[0] is the script itself, sys.argv[1] is the first argument (region), if not like this error code 1
    if len(sys.argv) != 2:
        print("Usage: worker.py REGION")
        exit(1)

    # Create a new worker with the region from the first argument
    region = sys.argv[1]
    worker = Worker(region)
    worker.start()


if __name__ == "__main__":
    main()