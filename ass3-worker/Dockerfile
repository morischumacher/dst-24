# build: docker build -t dst/ass3-worker .
# run: docker run --rm --network=dst dst/ass3-worker at_vienna
FROM python:3-slim

WORKDIR /app

RUN pip install pika redis

# Copy the worker script into the container
COPY worker.py .

# Command to run the worker
ENTRYPOINT [ "python", "./worker.py" ]