package io.mukulele.lwm2msk;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;

public class App {

    public static void main(String[] args) throws Exception {
        String endpoint = env("LWM2M_ENDPOINT_NAME", "rpi-client");
        String bootstrapUri = env("LWM2M_BOOTSTRAP_URI", "coap://lwm2m.os.1nce.com:5683");
        String signalkUrl = env("SIGNALK_WS_URL", "ws://localhost:3000/signalk/v1/stream");
        String signalkToken = env("SIGNALK_TOKEN", null);

        System.out.println("[LwM2M] Endpoint: " + endpoint);
        System.out.println("[LwM2M] Bootstrap (NoSec): " + bootstrapUri);
        System.out.println("[SignalK] WS URL: " + signalkUrl);

        // Load standard LwM2M/ IPSO object model
        LwM2mModel model = new LwM2mModel(ObjectLoader.loadDefault());

        // Initialize objects for the client
        ObjectsInitializer initializer = new ObjectsInitializer(model);

        // Security: Bootstrap only (NoSec). Do NOT create any Server instances here to force bootstrap.
        initializer.setInstancesForObject(LwM2mId.SECURITY, Security.noSecBootstrap(bootstrapUri));

        // Provide the Server class so the bootstrap server can write instances later.
        initializer.setClassForObject(LwM2mId.SERVER, Server.class);

        // Minimal Device object (adjust manufacturer/model/serial as desired)
        Device device = new Device(
                "Raspberry Pi",          // manufacturer
                "Leshan-SignalK",        // model number
                "rpi-" + endpoint,       // serial number
                null                     // firmware version (optional)
        );
        initializer.setInstancesForObject(LwM2mId.DEVICE, device);

        // Sensor objects will be added later when we finalize Signal K mappings.
        List<LwM2mObjectEnabler> enablers = initializer.createAll();

        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setObjects(enablers);
        // Binding mode/lifetime will come from Bootstrap. We keep defaults here.

        LeshanClient client = builder.build();

        // Start LwM2M client
        client.start();

        // Start Signal K WS stub (no mapping yet)
        SignalKClient skClient = new SignalKClient(signalkUrl, signalkToken, msg -> {
            // Placeholder: later we will parse deltas and update LwM2M resources
            // System.out.println("[SignalK] " + msg);
        });
        skClient.connect();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            try {
                skClient.close();
            } catch (Exception ignored) {}
            try {
                client.stop(true);
            } catch (Exception ignored) {}
        }));

        // Keep main thread alive
        while (true) {
            TimeUnit.MINUTES.sleep(10);
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? def : v;
    }
}
