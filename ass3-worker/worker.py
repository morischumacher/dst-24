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


class Worker:
    def __init__(self, region):
        self.region = region
        self.connection = pika.BlockingConnection(pika.ConnectionParameters(
            'rabbit',
            5672,
            '/',
            pika.PlainCredentials('dst', 'dst')))
        self.channel = self.connection.channel()
        self.queue = "dst." + region
        self.channel.queue_declare(self.queue, durable=True)
        self.channel.basic_consume(queue=self.queue,
                                   on_message_callback=self.callback,
                                   auto_ack=True)

    def fetch_drivers(self):
        r = redis.Redis(host="redis")
        all = r.hgetall("drivers:" + self.region)
        drivers = []
        for k, v in all.items():
            k, v = k.decode(), v.decode()
            lon, lat = v.split(" ")
            drivers.append(Driver(k, Location(float(lon), float(lat))))

        return drivers

    def distance(self, start, end):
        lat1 = radians(start.latitude)
        lon1 = radians(start.longitude)
        lat2 = radians(end.latitude)
        lon2 = radians(start.longitude)

        dlon = lon2 - lon1
        dlat = lat2 - lat1

        a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
        c = 2 * atan2(sqrt(a), sqrt(1 - a))

        R = 6373.0
        return R * c

    def match(self, request, drivers):
        return min(drivers, key=lambda d: self.distance(d.location, request.pickup))

    def simulate_work(self):
        random_sleeps = {
            "at_linz": (1, 2),
            "at_vienna": (3, 5),
            "de_berlin": (8, 11),
        }

        lower, upper = random_sleeps[self.region]
        time.sleep(random.randint(lower, upper))

    def remove_driver(self, driver):
        r = redis.Redis(host="redis")
        return r.hdel("drivers:" + self.region, str(driver.id))

    def publish(self, req, matched_driver, duration_ms):
        message = {
            "requestId": req.id,
            "driverId": matched_driver.id,
            "processingTime": duration_ms,
        }
        self.channel.basic_publish("dst.workers", "requests." + self.region, json.dumps(message).encode())

    def callback(self, ch, method, properties, body):
        print("callback")
        while True:
            start_ms = time.time_ns() / 1000
            message = json.loads(body)
            req = MatchRequest(message['id'], Location(message['pickup']['longitude'], message['pickup']['longitude']))
            print(f"Start matching for {req.id}")
            drivers = self.fetch_drivers()
            if drivers == []:
                print("No driver available")
                self.publish(req, Driver("", None), 0)
                break

            print(f"Got {len(drivers)} available drivers")
            matched_driver = self.match(req, drivers)
            self.simulate_work()
            print(f"Matched driver {matched_driver.id}")
            removed = self.remove_driver(matched_driver)
            if removed == 0:
                print("Recalculate a match, driver no longer available")
                continue

            duration_ms = (time.time_ns() / 1000) - start_ms
            self.publish(req, matched_driver, int(duration_ms))
            print(f"Published result")
            break

    def start(self):
        def term_handler(_signo, _stack_frame):
            print("Terminiated with SIGTERM")
            sys.exit(0)

        signal.signal(signal.SIGTERM, term_handler)

        print("Waiting for messages")
        self.channel.start_consuming()


def main():
    if len(sys.argv) != 2:
        print("Usage: worker.py REGION")
        exit(1)

    region = sys.argv[1]
    worker = Worker(region)
    worker.start()


if __name__ == "__main__":
    main()