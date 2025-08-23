# LwM2M Client for 1NCE Bootstrap (NoSec) + Signal K (skeleton)

This client:
- Uses LwM2M Bootstrap (NoSec) at `coap://lwm2m.os.1nce.com:5683`
- Relies on the Bootstrap server to provision the operational LwM2M Server (no local Server config)
- Connects to a local Signal K WebSocket (mapping to LwM2M sensors will be added later)

## Prerequisites
- Java 17+ (e.g., Temurin/OpenJDK)
- Network allows outbound UDP/5683
- Signal K running locally (default: `ws://localhost:3000/signalk/v1/stream`)

## Configure (Environment Variables)
- LWM2M_ENDPOINT_NAME=rpi-client
- LWM2M_BOOTSTRAP_URI=coap://lwm2m.os.1nce.com:5683
- SIGNALK_WS_URL=ws://localhost:3000/signalk/v1/stream
- SIGNALK_TOKEN=optional

Note: Do not set any direct server URI; 1NCE Bootstrap provides all operational data.

## Build
```bash
./gradlew shadowJar
```
Fat jar output: `build/libs/lwm2m-signalk-all.jar`

## Run (manual)
```bash
export LWM2M_ENDPOINT_NAME=rpi-client
export LWM2M_BOOTSTRAP_URI=coap://lwm2m.os.1nce.com:5683
export SIGNALK_WS_URL=ws://localhost:3000/signalk/v1/stream
java -jar build/libs/lwm2m-signalk-all.jar
```

## Install as systemd service (Pi)
1) Copy the unit file from `packaging/systemd/lwm2m-signalk.service` to `/etc/systemd/system/`
2) Adjust paths if needed, then:
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now lwm2m-signalk.service
sudo systemctl status lwm2m-signalk.service
```

## Next steps
- We’ll add IPSO objects and map chosen Signal K paths (e.g., Location 6, Temp 3303, Humidity 3304, Generic Sensor 3300).
- With Bootstrap, lifetime/binding/observe policy can be controlled on your 1NCE side.